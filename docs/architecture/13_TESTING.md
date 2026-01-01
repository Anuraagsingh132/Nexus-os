# Testing Strategy

Use a risk pyramid:

- Unit tests for domain rules, validators, permission policies, reducers, parsers, and AI prompt/output schemas.
- Repository/service integration tests with PostgreSQL/Redis/Elasticsearch/Qdrant/MinIO through Testcontainers where practical.
- API contract tests for status, error envelope, pagination, idempotency, authorization, and OpenAPI drift.
- Frontend component tests for key states and keyboard behavior using Testing Library/MSW.
- Playwright E2E for the core journeys named in the master prompt.
- Security tests for cross-tenant IDs, role escalation, refresh reuse, CSRF, unsafe files, unauthorized WebSocket subscriptions, and RAG leakage.

Tests are deterministic, parallel-safe, and create isolated data. Never disable flaky tests; diagnose timing and state. CI publishes test and coverage reports. Coverage is a diagnostic target (80% domain/service code, meaningful branch coverage), not a substitute for behavior.

