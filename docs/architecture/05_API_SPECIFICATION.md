# API Specification

Base path `/api/v1`. JSON uses camelCase, ISO-8601 UTC timestamps, UUID strings, and a stable error envelope:

```json
{"code":"TASK_NOT_FOUND","message":"Task not found","requestId":"...","fieldErrors":[]}
```

OpenAPI is generated and validated in CI. Unbounded collections use opaque cursor pagination. Mutation endpoints accept `Idempotency-Key` where retries could duplicate work. Updates use explicit PATCH DTOs and version/ETag conflict handling.

## Endpoint groups

- `/auth`: signup, login, refresh, logout, logout-all, verify-email, resend-verification, forgot/reset-password, OAuth start/callback, MFA setup/verify/disable.
- `/users/me`, `/users/{id}`: profile, avatar, preferences, sessions, presence-safe public profile.
- `/organizations`, `/workspaces`: CRUD, members, invitations, role assignments, settings, usage.
- `/projects`: CRUD, archive, members, summaries.
- `/tasks`: CRUD, move/reorder, assign, labels, dependencies, subtasks, comments, attachments, time entries, recurrence, history.
- `/channels`, `/conversations`, `/messages`: channel/DM lifecycle, threads, reactions, reads, pins, search.
- `/documents`: CRUD, versions, restore, comments, templates, AI actions.
- `/folders`, `/files`, `/uploads`: folder tree, multipart initiation/completion, download/preview URLs, versions, permissions.
- `/calendar-events`, `/meetings`, `/reminders`: CRUD, attendees, link adapters.
- `/notifications`: list, mark read/all-read, preferences, realtime subscription metadata.
- `/search`: unified keyword/semantic search with type, workspace, project, author and date filters.
- `/ai`: conversations, workspace chat, summarize, draft document/email, meeting summary, task extraction, code explanation/review, ingestion status.
- `/analytics`: overview, workload, completion, velocity, burndown, AI insights.
- `/admin`: users, audit logs, storage/usage, feature flags, integrations, reports.

## Realtime

Authenticate the WebSocket handshake. Destinations are workspace/channel/user scoped. Events include message created/updated/deleted, typing started/stopped, presence changed, task changed, notification created, document presence, and ingestion progress. Clients must refetch authoritative data after reconnect; sequence IDs prevent gaps.

## Authorization

Every endpoint documents permitted roles and resource rules. Owner/Admin manage workspace and roles; Manager manages projects and ordinary members; Member collaborates; Guest sees only explicitly shared resources. Object ownership never substitutes for workspace membership.

