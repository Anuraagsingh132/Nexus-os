# Authentication and Authorization

Passwords use Argon2id or BCrypt with a documented cost. Short-lived access tokens are held in memory or secure same-site cookies; rotating refresh tokens are hashed at rest in Secure, HttpOnly, SameSite cookies. Detect refresh-token reuse and revoke the token family.

Support email/password, email verification, reset flow, session listing/revocation, OAuth2 (Google/GitHub adapters), and TOTP 2FA with recovery codes. Local development uses a mail-capture service and disabled-by-default fake OAuth.

Authorization combines coarse RBAC with resource checks. Every service method receives an authenticated principal and verified tenant context. Prevent IDOR by querying resources through authorized organization/workspace scope. Sensitive changes require recent authentication where feasible.

CSRF protection is mandatory for cookie-authenticated mutations. Apply login throttling, generic credential errors, constant-time comparisons, secure cookie configuration, CORS allowlists, and audit events for login, logout, reset, MFA, invite, membership, role, integration, and export changes.

