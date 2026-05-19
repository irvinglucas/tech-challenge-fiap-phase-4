#!/usr/bin/env bash
#
# setup.sh — One-shot provisioning for the FIAP Phase 4 Feedback Platform on GCP.
#
# Idempotent: every command tolerates "already exists" so you can rerun it.
# Reads its inputs from environment variables (export before running) or from
# a .env file in the same directory (auto-loaded if present).
#
# Required env vars:
#   PROJECT_ID            GCP project ID
#   REGION                default: southamerica-east1
#   GMAIL_USERNAME        gmail address used as the sender (e.g. you@gmail.com)
#   GMAIL_APP_PASSWORD    16-char Google App Password (https://myaccount.google.com/apppasswords)
#   ADMIN_EMAIL           recipient for urgent notifications and weekly reports
#
# Usage:
#   cp infra/.env.example infra/.env   # then edit
#   ./infra/setup.sh
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# --- Load .env if present (gitignored; per-developer secrets) -----------------
if [[ -f "${SCRIPT_DIR}/.env" ]]; then
  set -a; source "${SCRIPT_DIR}/.env"; set +a
fi

: "${PROJECT_ID:?PROJECT_ID is required}"
: "${GMAIL_USERNAME:?GMAIL_USERNAME is required}"
: "${GMAIL_APP_PASSWORD:?GMAIL_APP_PASSWORD is required}"
: "${ADMIN_EMAIL:?ADMIN_EMAIL is required}"
REGION="${REGION:-southamerica-east1}"
URGENT_TOPIC="${URGENT_TOPIC:-urgent-feedback}"
URGENT_DLQ_TOPIC="${URGENT_DLQ_TOPIC:-urgent-feedback-dlq}"

# Project number is needed when granting roles to GCP-managed agents.
PROJECT_NUMBER="$(gcloud projects describe "${PROJECT_ID}" --format='value(projectNumber)')"

bold() { printf "\033[1m== %s ==\033[0m\n" "$*"; }
step() { printf "\n\033[36m--> %s\033[0m\n" "$*"; }
warn() { printf "\033[33m!! %s\033[0m\n" "$*" >&2; }

bold "Provisioning FIAP Feedback Platform"
echo "Project:    ${PROJECT_ID} (#${PROJECT_NUMBER})"
echo "Region:     ${REGION}"
echo "Topic:      ${URGENT_TOPIC} (DLQ: ${URGENT_DLQ_TOPIC})"
echo "Sender:     ${GMAIL_USERNAME}"
echo "Recipient:  ${ADMIN_EMAIL}"
echo

# --- 1. Defaults -------------------------------------------------------------
step "Setting gcloud defaults"
gcloud config set project "${PROJECT_ID}" --quiet
gcloud config set functions/region "${REGION}" --quiet
gcloud config set run/region "${REGION}" --quiet

# --- 2. Enable APIs ----------------------------------------------------------
step "Enabling required APIs"
gcloud services enable \
  cloudfunctions.googleapis.com \
  run.googleapis.com \
  cloudbuild.googleapis.com \
  artifactregistry.googleapis.com \
  eventarc.googleapis.com \
  firestore.googleapis.com \
  pubsub.googleapis.com \
  cloudscheduler.googleapis.com \
  secretmanager.googleapis.com \
  logging.googleapis.com \
  monitoring.googleapis.com \
  iam.googleapis.com \
  iamcredentials.googleapis.com \
  --quiet

# --- 3. Firestore (Native mode) ----------------------------------------------
step "Ensuring Firestore database (Native mode)"
if ! gcloud firestore databases describe --database='(default)' >/dev/null 2>&1; then
  gcloud firestore databases create --location="${REGION}" --quiet
else
  echo "Firestore database already exists; skipping."
fi

# --- 4. Pub/Sub topics + DLQ --------------------------------------------------
step "Ensuring Pub/Sub topics"
gcloud pubsub topics create "${URGENT_TOPIC}"     --quiet 2>/dev/null || echo "Topic ${URGENT_TOPIC} already exists."
gcloud pubsub topics create "${URGENT_DLQ_TOPIC}" --quiet 2>/dev/null || echo "Topic ${URGENT_DLQ_TOPIC} already exists."

# --- 5. Secret Manager secrets -----------------------------------------------
step "Ensuring Secret Manager secrets"
ensure_secret() {
  local name="$1" value="$2"
  if ! gcloud secrets describe "${name}" >/dev/null 2>&1; then
    printf "%s" "${value}" | gcloud secrets create "${name}" --replication-policy=automatic --data-file=- --quiet
    echo "  created ${name}"
  else
    printf "%s" "${value}" | gcloud secrets versions add "${name}" --data-file=- --quiet >/dev/null
    echo "  added new version to ${name}"
  fi
}
ensure_secret "gmail-username"     "${GMAIL_USERNAME}"
ensure_secret "gmail-app-password" "${GMAIL_APP_PASSWORD}"
ensure_secret "admin-email"        "${ADMIN_EMAIL}"

# --- 6. Service accounts + IAM ------------------------------------------------
step "Ensuring per-function service accounts"
ensure_sa() {
  local sa_id="$1" display_name="$2"
  if ! gcloud iam service-accounts describe "${sa_id}@${PROJECT_ID}.iam.gserviceaccount.com" >/dev/null 2>&1; then
    gcloud iam service-accounts create "${sa_id}" --display-name="${display_name}" --quiet
    echo "  created ${sa_id}"
  else
    echo "  ${sa_id} already exists"
  fi
}
ensure_sa "feedback-api-sa"      "Feedback API"
ensure_sa "notification-sa"      "Urgent-Feedback Notification Handler"
ensure_sa "report-sa"            "Weekly Report Generator"
ensure_sa "scheduler-invoker-sa" "Cloud Scheduler -> weekly-report invoker"
ensure_sa "github-deployer-sa"   "GitHub Actions Deployer"

grant() {
  local sa_id="$1" role="$2"
  gcloud projects add-iam-policy-binding "${PROJECT_ID}" \
    --member="serviceAccount:${sa_id}@${PROJECT_ID}.iam.gserviceaccount.com" \
    --role="${role}" --condition=None --quiet >/dev/null
  echo "  ${sa_id} += ${role}"
}

step "Granting least-privilege IAM roles"
# feedback-api: writes to Firestore, publishes to Pub/Sub, reads secrets
grant feedback-api-sa roles/datastore.user
grant feedback-api-sa roles/pubsub.publisher
grant feedback-api-sa roles/secretmanager.secretAccessor

# notification-handler: reads Pub/Sub events via Eventarc, reads secrets,
# is invoked as a Gen2 function (Cloud Run under the hood)
grant notification-sa roles/secretmanager.secretAccessor
grant notification-sa roles/eventarc.eventReceiver
grant notification-sa roles/run.invoker

# weekly-report: reads Firestore, reads secrets, can be invoked by Scheduler
grant report-sa roles/datastore.viewer
grant report-sa roles/secretmanager.secretAccessor

# scheduler-invoker-sa: only allowed to invoke the weekly-report function
# (the run.invoker role is bound on the specific service after deploy)

# github-deployer-sa: scoped CI/CD principal (deploys functions, accesses GCS
# build sources, can act-as the per-function service accounts).
grant github-deployer-sa roles/cloudfunctions.developer
grant github-deployer-sa roles/run.admin
grant github-deployer-sa roles/iam.serviceAccountUser
grant github-deployer-sa roles/storage.admin
grant github-deployer-sa roles/artifactregistry.admin

# The Eventarc Pub/Sub trigger relies on the Pub/Sub service agent being able
# to mint tokens for the function's service account. (Required since Eventarc
# refactor in late 2023.)
gcloud projects add-iam-policy-binding "${PROJECT_ID}" \
  --member="serviceAccount:service-${PROJECT_NUMBER}@gcp-sa-pubsub.iam.gserviceaccount.com" \
  --role=roles/iam.serviceAccountTokenCreator --condition=None --quiet >/dev/null

# --- 7. Cloud Monitoring email channel ---------------------------------------
step "Ensuring Cloud Monitoring email notification channel"
CHANNEL_NAME="$(gcloud alpha monitoring channels list \
  --filter="type=email AND labels.email_address=${ADMIN_EMAIL}" \
  --format='value(name)' | head -n1 || true)"
if [[ -z "${CHANNEL_NAME}" ]]; then
  CHANNEL_NAME="$(gcloud alpha monitoring channels create \
    --display-name='FIAP Feedback Admin' \
    --type=email \
    --channel-labels="email_address=${ADMIN_EMAIL}" \
    --format='value(name)')"
  echo "  created channel ${CHANNEL_NAME}"
else
  echo "  channel already exists: ${CHANNEL_NAME}"
fi

# --- 8. Done ------------------------------------------------------------------
bold "Provisioning complete"
cat <<EOF

Next steps:
  1. Push to main (or run the workflows manually) — GitHub Actions will deploy
     the three functions and print their URLs.
  2. After 'feedback-api' is deployed, capture its URL:
       FEEDBACK_API_URL=\$(gcloud functions describe feedback-api --gen2 \\
         --region=${REGION} --format='value(serviceConfig.uri)')
  3. After 'weekly-report' is deployed, create the Cloud Scheduler job:
       REPORT_URL=\$(gcloud functions describe weekly-report --gen2 \\
         --region=${REGION} --format='value(serviceConfig.uri)')
       gcloud scheduler jobs create http weekly-report-job \\
         --location=${REGION} \\
         --schedule='0 11 * * 1' --time-zone='UTC' \\
         --http-method=POST --uri="\${REPORT_URL}" \\
         --oidc-service-account-email=scheduler-invoker-sa@${PROJECT_ID}.iam.gserviceaccount.com \\
         --oidc-token-audience="\${REPORT_URL}"
       gcloud run services add-iam-policy-binding weekly-report \\
         --region=${REGION} \\
         --member="serviceAccount:scheduler-invoker-sa@${PROJECT_ID}.iam.gserviceaccount.com" \\
         --role=roles/run.invoker
  4. For CI/CD, download a key for github-deployer-sa and store it as the
     GH secret GCP_SA_KEY:
       gcloud iam service-accounts keys create gh-deployer.json \\
         --iam-account=github-deployer-sa@${PROJECT_ID}.iam.gserviceaccount.com
       gh secret set GCP_SA_KEY < gh-deployer.json   # then 'rm gh-deployer.json'
EOF
