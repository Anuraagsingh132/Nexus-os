# Product Requirements

## Success metrics

- A new team reaches its first created task or message within 10 minutes.
- Core pages reach useful content in under 2.5 seconds on a typical broadband connection.
- Permission-denied and cross-tenant access tests have zero failures.
- AI answers show sources or explicitly say that evidence is insufficient.
- Core workflows are usable by keyboard and on 360 px-wide screens.

## Priority

### P0 — must be fully executable

Identity and sessions; organizations/workspaces/memberships/invitations/RBAC; profiles; projects; task CRUD, board/list, subtasks, comments, labels, due dates, attachments and activity; channels/DMs/messages/threads/read state/presence; documents and versions; file/folder upload and download; notifications; meetings/calendar basics; unified search; RAG ingestion and cited answers; dashboards; audit logs; admin/settings; Docker, CI, migrations, seed data, tests and docs.

### P1 — complete after P0 gates

Timeline/calendar task views, dependencies, recurring tasks, time tracking, voice-message file type, richer templates, Google Calendar adapter, email/push adapters, advanced analytics, saved searches, PWA shell, infinite scrolling, command palette, keyboard shortcuts, localization foundation, feature flags.

### P2 — extension-ready

CRDT live cursors and conflict-free collaborative editing, robust offline mutation sync, native video/audio calling, plugin marketplace, real billing settlement, enterprise SSO/SCIM, multi-region deployment.

## Global behavior

Every resource belongs to an organization and usually a workspace. Authorization is enforced server-side. Destructive actions require confirmation and return recoverable errors. Empty, loading, offline, error, forbidden, and not-found states are designed states, not afterthoughts.

