# Directory Structure

```text
/
  apps/
    web/                 # Next.js
    api/                 # Spring Boot application
    worker/              # optional Spring worker launcher
  backend/modules/
    identity/
    workspace/
    projects/
    conversations/
    content/
    files/
    notifications/
    calendar/
    search/
    ai/
    analytics/
    administration/
  packages/
    ui/
    api-client/
    config/
  infra/
    compose/
    docker/
    grafana/
    deployment/
  docs/
    decisions/
    architecture/
    api/
    operations/
    reports/
  tests/
    e2e/
    load/
  .github/workflows/
```

Adapt to build tooling without erasing bounded contexts. Generated code and local service data stay out of Git.

