# Error Handling

Map domain failures to stable machine codes and correct HTTP statuses. Include a safe human message, request ID, optional field errors, and optional retry metadata. Do not expose stack traces, SQL, object keys, provider secrets, or existence of inaccessible resources.

Use 400 validation, 401 unauthenticated, 403 forbidden, 404 absent-or-concealed, 409 conflict/version/idempotency, 413 payload too large, 415 unsupported media, 422 domain rule, 429 throttled, and 5xx unexpected/provider failures.

Frontend errors retain user input, explain recovery, support retry where safe, and log correlation IDs. Background jobs record attempts, terminal reason, and safe replay controls. Global handlers are the last boundary, not a substitute for domain handling.

