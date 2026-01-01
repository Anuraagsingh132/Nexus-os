# Deployment

Recommended production mapping: Next.js on Vercel or container platform; Spring API/workers on Railway/Render/Kubernetes-compatible host; Supabase/managed PostgreSQL; Upstash/managed Redis; managed Elasticsearch; Qdrant Cloud; Cloudflare R2/S3; managed Kafka or local-event profile for small deployments.

Separate dev/staging/prod. Production requires TLS, custom domains, secret manager, database connection pooling, managed backups, point-in-time recovery, object lifecycle rules, private service networking where available, restricted CORS, and approved OAuth redirect URIs.

Deployment guide must cover migrations, seed policy, initial admin bootstrap, smoke test, observability validation, rollback to prior images, backward-compatible database changes, backup restore, and disaster recovery. External account creation, DNS, billing, secrets, and production approval remain human actions.

