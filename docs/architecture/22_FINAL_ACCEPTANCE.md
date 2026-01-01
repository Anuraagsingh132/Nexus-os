# Final Acceptance

Nexus OS is acceptable only when:

- A clean clone can start local dependencies, migrate, seed, run web/API/workers, and display healthy status using documented commands.
- A user can sign up/login, create or join a workspace, and role restrictions are enforced.
- A member can create a project/task, move it, comment, attach a file, and see activity.
- Two browser sessions can exchange channel/DM messages and update read/presence state.
- A member can create/version a document and upload/version/download a file.
- Calendar/meeting/reminder and in-app notification flows work.
- Unified search returns only authorized content.
- A seeded document can be ingested and a workspace AI question returns a grounded answer with a working citation; unauthorized sources never appear.
- Dashboard figures derive from persisted data.
- Admin can inspect members, usage, feature flags, and audit events subject to authorization.
- Builds, migrations and committed tests pass; critical security, accessibility and responsive journeys are verified.
- Docker, CI, OpenAPI, environment templates, deployment guide, backup/rollback guide, and final evidence are present.

P0 placeholders, dead controls, fake backend data, cross-tenant leaks, invented test results, or undocumented startup steps are automatic rejection conditions.

