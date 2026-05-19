#!/usr/bin/env bash
#
# teardown.sh — Remove every resource provisioned by setup.sh so the project
# stops consuming credits between work sessions.
#
# Idempotent: missing resources are ignored.
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if [[ -f "${SCRIPT_DIR}/.env" ]]; then
  set -a; source "${SCRIPT_DIR}/.env"; set +a
fi

: "${PROJECT_ID:?PROJECT_ID is required}"
REGION="${REGION:-southamerica-east1}"
URGENT_TOPIC="${URGENT_TOPIC:-urgent-feedback}"
URGENT_DLQ_TOPIC="${URGENT_DLQ_TOPIC:-urgent-feedback-dlq}"

bold() { printf "\033[1m== %s ==\033[0m\n" "$*"; }
step() { printf "\n\033[36m--> %s\033[0m\n" "$*"; }

bold "Tearing down FIAP Feedback Platform in ${PROJECT_ID}"

gcloud config set project "${PROJECT_ID}" --quiet
gcloud config set functions/region "${REGION}" --quiet

step "Deleting Cloud Scheduler jobs"
gcloud scheduler jobs delete weekly-report-job --location="${REGION}" --quiet 2>/dev/null || true

step "Deleting Cloud Functions"
for fn in feedback-api notification-handler weekly-report; do
  gcloud functions delete "${fn}" --gen2 --region="${REGION}" --quiet 2>/dev/null || true
done

step "Deleting Pub/Sub topics"
gcloud pubsub topics delete "${URGENT_TOPIC}"     --quiet 2>/dev/null || true
gcloud pubsub topics delete "${URGENT_DLQ_TOPIC}" --quiet 2>/dev/null || true

step "Deleting Secret Manager secrets"
for s in gmail-username gmail-app-password admin-email; do
  gcloud secrets delete "${s}" --quiet 2>/dev/null || true
done

step "Deleting service accounts"
for sa in feedback-api-sa notification-sa report-sa scheduler-invoker-sa github-deployer-sa; do
  gcloud iam service-accounts delete \
    "${sa}@${PROJECT_ID}.iam.gserviceaccount.com" --quiet 2>/dev/null || true
done

cat <<EOF

Note: Firestore databases cannot be deleted via 'gcloud firestore databases delete'
in Native mode without first emptying the database. To fully reset state, either:
  - Use the GCP Console to delete the (default) database, or
  - Recreate the project entirely (recommended for FIAP credit hygiene).
EOF
