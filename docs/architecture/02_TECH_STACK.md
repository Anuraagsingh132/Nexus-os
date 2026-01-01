# Technology Stack

- Java 21, Spring Boot 3.x, Gradle Kotlin DSL.
- Spring Security, OAuth2 client/resource server, WebSocket, Validation, Data JPA, Actuator.
- PostgreSQL 16+, Redis 7+, Elasticsearch 8+, Qdrant, Kafka 3+, MinIO for local S3 compatibility.
- Next.js 15 App Router, React, strict TypeScript, Tailwind, shadcn/ui/Radix, Framer Motion.
- TanStack Query for server state; Zustand only for short-lived UI state; React Hook Form + Zod.
- Flyway migrations; OpenAPI 3.1 via springdoc.
- JUnit 5, AssertJ, Testcontainers, WireMock; Vitest, Testing Library, MSW; Playwright.
- Docker Compose locally; GitHub Actions CI; Vercel-compatible web image and generic OCI backend image.
- OpenTelemetry conventions, structured JSON logging, Prometheus metrics, Grafana/Loki-ready deployment examples.

Version selection must prioritize current stable, mutually compatible releases. Avoid preview dependencies unless a binding requirement needs them. Record exact versions and upgrade notes.

