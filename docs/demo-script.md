# Demo video script

Suggested 5–8 minute recording flow for the FIAP Phase 4 deliverable. Adjust
pacing to taste; the order below is optimized to surface the architecture
story first, then the live behavior, then the security details.

## 0. Before you press record

```bash
# Make sure your shell sees the right project
gcloud config set project $PROJECT_ID
gcloud config set functions/region southamerica-east1

# Warm the API up to avoid cold-start latency on camera.
# In GitHub: Actions → Deploy feedback-api → Run workflow → min_instances=1
```

Open these tabs ahead of time:

- GitHub repository (root of this codebase)
- GCP Console → Cloud Functions
- GCP Console → Firestore data
- GCP Console → Cloud Monitoring → Dashboards
- A local terminal authenticated as you (`gcloud auth login`)
- Postman with the imported collection + environment
- Your Gmail inbox (sender = `gmail-username`, recipient = `admin-email`)

## 1. Intro (60s)

> "This is the FIAP Phase 4 Tech Challenge — a serverless feedback platform
> on Google Cloud Platform, written in Java 21 with Quarkus."

Open the repository. Walk through:

- `README.md` — quick summary, the architecture mermaid diagram.
- `ARCHITECTURE.md` — show the layers diagram and the function table.

> "Three Cloud Functions, one per business responsibility, all backed by a
> single Clean Architecture library."

## 2. Code tour — Clean Architecture (60s)

Drill into `common/src/main/java/com/fiap/feedback/`:

- `domain/Evaluation.java` — pure Java, invariants in the factory.
- `application/port/in/SubmitEvaluation.java` — input port (interface).
- `application/usecase/SubmitEvaluationService.java` — orchestration; show
  that it depends only on output ports.
- `feedback-api/.../EvaluationResource.java` — thin inbound adapter. No
  Firestore code here.

> "If we ever needed to move to AWS or Azure, only the infrastructure
> adapters and inbound adapters change — the domain and use cases are
> untouched. See `docs/portability-aws-azure.md`."

## 3. Live happy path (90s)

In Postman, run the **Submit Evaluation** folder requests one by one:

- BAIXA → 201 with `urgencia: BAIXA`, no notification.
- MEDIA → 201, no notification.
- ALTA → 201 with `urgencia: ALTA`.

Switch to **Firestore** in the Console and refresh — show the three new
documents in the `evaluations` collection.

Switch to **Gmail inbox** — show the urgent e-mail that just arrived:

> "Notice the e-mail body has the three fields required by the brief:
> Descrição, Urgência, Data de envio."

## 4. Live validation errors (30s)

Run the **Validation errors** folder. Each request returns 400 with a
structured error body. Briefly show the test assertions panel — every
request has a `Tests` script.

## 5. Weekly report on demand (60s)

```bash
# Trigger the scheduled job ad-hoc (Console works too)
gcloud scheduler jobs run weekly-report-job --location=southamerica-east1

# Watch logs in real time
gcloud functions logs read weekly-report --gen2 \
  --region=southamerica-east1 --limit=20
```

Switch to the Gmail inbox — show the weekly report e-mail with per-day and
per-urgency counts plus the average.

## 6. Security & governance (60s)

```bash
# Show per-function service accounts
gcloud iam service-accounts list \
  --filter='email~feedback-api-sa|notification-sa|report-sa|scheduler-invoker-sa'

# Show the least-privileged IAM bindings
gcloud projects get-iam-policy $PROJECT_ID \
  --flatten=bindings \
  --filter='bindings.members~feedback-api-sa' \
  --format='table(bindings.role)'

# Show secrets — values are hidden
gcloud secrets list
```

> "Three secrets, never in git. Each function has its own service account
> with the smallest set of roles. The CI deployer in GitHub has a separate
> SA that can only deploy functions — it cannot read Firestore or secrets."

## 7. Observability (45s)

Open Cloud Monitoring → Dashboards → "FIAP Feedback Platform". Show the
four charts (request rate, errors, p95 latency, instances) populating in
real time after the test traffic you sent earlier.

Open Cloud Logging, filter by `resource.labels.service_name="feedback-api"`
and show one of the structured JSON log lines.

## 8. Wrap-up (30s)

> "Push to main triggers per-function GitHub Actions workflows that
> redeploy independently. The `infra/setup.sh` script provisioned this
> whole environment from scratch in under 5 minutes. After this recording
> I'll switch `min_instances` back to 0 to preserve credits, and
> `teardown.sh` removes everything when I'm done."

## 9. After you stop recording

```bash
# 1. Scale the API back to zero
#    GitHub: Actions → Deploy feedback-api → Run workflow → min_instances=0

# 2. (Optional) Tear down to fully stop spend
./infra/teardown.sh
```
