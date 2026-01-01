# Environment Variables

Provide a documented `.env.example` with no secrets. Validate configuration at startup and fail clearly.

Groups include:

- Web/API URLs, public app URL, environment, log level.
- PostgreSQL URL/user/password/pool settings.
- Redis, Elasticsearch, Qdrant, Kafka endpoints.
- S3 endpoint/region/bucket/access credentials/path-style switch.
- JWT signing/rotation configuration, cookie domain/security, allowed origins.
- OAuth client IDs/secrets and callback URLs.
- SMTP/mail-capture, web-push keys.
- LLM/embedding provider, model names, API key, token/cost limits.
- OpenTelemetry exporter, metrics, Sentry-compatible DSN if used.
- Feature flags, upload limits, retention and bootstrap admin.

Classify each variable as server-secret, server-nonsecret, or browser-public. Only intentionally public values use `NEXT_PUBLIC_`.

