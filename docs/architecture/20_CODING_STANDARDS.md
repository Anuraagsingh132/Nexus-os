# Coding Standards

Java: domain-oriented packages, constructor injection, immutable DTOs/records where appropriate, explicit transaction boundaries, no field injection, no repository access from controllers, no catch-all exceptions, no entity exposure at API boundaries.

TypeScript: strict mode; no unjustified `any`; typed API client; colocated schemas/tests; semantic components; effects only for synchronization; stable query keys; exhaustive status handling.

General: SOLID where it reduces coupling, simple code over ceremonial abstraction, meaningful names, small functions, dependency inversion at external boundaries, comments for rationale, and ADRs for durable decisions. Format automatically. Remove dead code and stale TODOs before acceptance.

