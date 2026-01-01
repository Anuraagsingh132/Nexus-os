# Security Requirements

Threat-model tenant isolation, IDOR, credential theft, token replay, CSRF, XSS, SQL/NoSQL injection, SSRF, malicious uploads, stored content attacks, WebSocket subscription abuse, prompt injection, vector leakage, dependency compromise, and log leakage.

Required controls: server-side RBAC/ABAC checks; DTO allowlists; Bean Validation and Zod UX validation; parameterized persistence; CSP and safe rich-text rendering; strict CORS; CSRF for cookies; rate limits; secure headers; encrypted transport; secret management; hashed passwords/tokens; signed short-lived object URLs; file type/size validation and quarantine; audit logs; dependency/container scanning; least-privilege service accounts.

Uploads use server-generated keys, checksum verification, content sniffing, quota enforcement, and scan status before preview. AI retrieval filters by current ACL. Logs redact authorization headers, cookies, tokens, passwords, reset links, signed URLs, and sensitive content.

Document a vulnerability disclosure path, retention/deletion process, backup encryption, incident response, restore drill, and key rotation. “Helmet” is a Node library and is not required; configure equivalent headers in Spring and Next.js.

