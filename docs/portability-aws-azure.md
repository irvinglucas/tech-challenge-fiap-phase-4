# Cloud portability appendix

This document maps every GCP-specific concern in the platform to its AWS and
Azure equivalent, showing what a hypothetical migration would require.

**Key claim:** the `domain` and `application` layers of `common` are
**unchanged** across all three clouds. Quarkus's CDI wires the right adapter
implementation based on which extension is on the classpath.

## What changes per cloud

| Concern              | GCP (current)                                            | AWS                                                                                      | Azure                                                                                  |
| -------------------- | -------------------------------------------------------- | ---------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------- |
| HTTP inbound adapter | `quarkus-google-cloud-functions-http` (JAX-RS)           | `quarkus-amazon-lambda-http`                                                             | `quarkus-azure-functions-http`                                                         |
| Event inbound        | `quarkus-google-cloud-functions` (`CloudEventsFunction`) | `quarkus-amazon-lambda` (SNS / SQS event handler)                                        | `quarkus-azure-functions` (Service Bus trigger)                                        |
| `EvaluationRepository` adapter | Firestore (`quarkus-google-cloud-firestore`)   | DynamoDB (`software.amazon.awssdk:dynamodb-enhanced` or `quarkus-amazon-dynamodb`)       | Cosmos DB (`com.azure:azure-cosmos`)                                                   |
| `EventPublisher` adapter       | Pub/Sub (`quarkus-google-cloud-pubsub`)         | SNS topic → SQS subscription (`quarkus-amazon-sns`)                                      | Service Bus topic (`com.azure:azure-messaging-servicebus`)                             |
| Scheduler            | Cloud Scheduler → HTTP Function                          | EventBridge Scheduler → Lambda                                                           | Azure Functions Timer trigger                                                          |
| Secrets              | Secret Manager + `--set-secrets`                         | Secrets Manager / SSM Parameter Store                                                    | Key Vault                                                                              |
| Observability        | Cloud Logging + Monitoring + Alerting                    | CloudWatch Logs + CloudWatch Metrics + CloudWatch Alarms                                 | Azure Monitor (Application Insights) + Action Groups                                   |
| CI/CD deploy step    | `gcloud functions deploy --gen2`                         | `aws lambda update-function-code` / `aws cloudformation deploy`                          | `az functionapp deployment source config-zip`                                          |
| Email                | Gmail SMTP via `quarkus-mailer`                          | unchanged (SES is also possible, but SMTP works on Lambda too)                           | unchanged (or Azure Communication Services Email)                                      |

## Concrete migration recipe (e.g. → AWS)

1. **Replace inbound-adapter dependencies** in each function module's
   `pom.xml`:
   - `feedback-api`: swap `quarkus-google-cloud-functions-http` for
     `quarkus-amazon-lambda-http`.
   - `notification-handler`: swap `quarkus-google-cloud-functions` for
     `quarkus-amazon-lambda` and adapt the handler to implement
     `RequestHandler<SNSEvent, Void>` (or use the Funqy equivalent).
   - `weekly-report`: same as `notification-handler` but with EventBridge
     Scheduler input.
2. **Add a new outbound adapter** under
   `common/src/main/java/com/fiap/feedback/infrastructure/dynamodb/`
   implementing `EvaluationRepository`. Annotate `@ApplicationScoped`. Add
   `@io.quarkus.arc.Priority` or remove the Firestore adapter from the
   classpath so CDI picks the new one.
3. **Add a new outbound adapter** under `infrastructure/sns/` for
   `EventPublisher`.
4. **Domain and use cases**: untouched. No code edits, no test changes.
5. **Replace `infra/setup.sh`** with the equivalent `aws` CLI script (IAM
   roles, DynamoDB table, SNS topic, EventBridge Scheduler rule, Secrets
   Manager). The `.github/workflows/deploy-*.yml` files keep the same
   shape — only the deploy step changes.
6. **Tests**: the entire `common` test suite (use case + domain) still
   runs unchanged because it uses in-memory port fakes.

## Estimated effort

For a developer already familiar with the target cloud's SDK:

- 1 day — rewrite the four adapters and adjust function entry points.
- 1 day — port `setup.sh` and the deploy workflows.
- 0.5 day — adapter integration tests against AWS/Azure emulators
  (LocalStack, Azurite).

**Total: ~2.5 dev-days** to deploy the same platform on a different cloud,
purely because Clean Architecture made the dependency direction
explicit.

## What you would NOT want to change

- `domain/` — pure Java, no need.
- `application/` — pure Java, no need.
- The use-case test suite — already cloud-agnostic.
- The `quarkus-mailer` configuration — same Gmail SMTP works from every
  cloud (port 587 outbound is allowed by AWS Lambda, Azure Functions, and
  GCP Cloud Functions).
- The Postman collection / OpenAPI spec — the API contract is identical;
  only `baseUrl` changes.
