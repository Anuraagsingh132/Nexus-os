# DevOps

Provide `docker-compose.yml` profiles for core and extended infrastructure, multi-stage Dockerfiles, `.dockerignore`, health checks, named volumes, and one-command startup. Defaults are safe for local development and clearly unsuitable for production.

GitHub Actions stages: dependency cache, formatting/lint, compile/typecheck, unit tests, integration tests, frontend/backend build, image build, dependency and image scan, E2E against Compose, and artifact/report upload. Deployment jobs are templates gated by environment approval and secrets.

Use immutable image tags plus commit SHA, non-root containers, read-only filesystems where practical, graceful shutdown, resource limits, startup/readiness probes, and migration jobs. Do not automatically deploy or create cloud resources without user authorization.

