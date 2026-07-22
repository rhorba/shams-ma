# SESSIONS — Shams.ma



## SESSION_START — 2026-07-21
Request: "Start sprint 1 — document all project docs, stack = Java Spring + Angular/React + Docker deployment."
Note: existing README.md describes stack as Next.js 15 + TypeScript + PostgreSQL/PostGIS + Drizzle + Cloudflare R2 + CMI — conflicts with requested stack. Flagged to user for clarification before UNDERSTAND phase proceeds.

## SESSION_END — 2026-07-21
Sprint 1 complete: full document-first foundation chain approved and pushed.
Done:
- Stack pivot confirmed: Java Spring Boot + React + Docker + PostgreSQL/PostGIS (supersedes old README stack — README text itself still says Next.js, not yet updated).
- All 10 foundation docs written + user-approved: prd, system-design, architecture, security, database, ux, ui, test-strategy, devops, stories (all in docs/).
- Key decisions locked: payments (CMI deposit) IN MVP scope; coverage zones = radius-from-point; MFA required for Admin only; MUI + solar-gold/green theme; monolith (no microservices/queue/cache) for MVP scale.
- .env.example drafted (real secret values NOT yet collected — needed before Sprint 2/EXECUTE starts).
- Git repo initialized, remote https://github.com/rhorba/shams-ma added, pushed to main (2 commits).
Next session (Sprint 2):
- Collect real env var values (DB, JWT secret, CMI creds, geocoding API key, SMTP, object storage) before writing code.
- Start Epic 0 (Foundation Scaffold): repo skeleton, CI pipeline, DB migrations, auth — per docs/stories-shams-ma.md.
- Consider updating README.md stack section to match the new Java/React/Docker stack (still describes old Next.js stack).

## SESSION_START — 2026-07-22
Request: "continue" (resuming Sprint 2 per prior session-end notes).
Env vars collected for Epic 0 scope (DB, JWT); CMI/geocoding/SMTP/storage filled with local sandbox defaults per user decision. Next: brainstorm/plan Epic 0 (Foundation Scaffold) per docs/stories-shams-ma.md.

## SESSION_END — 2026-07-22
Epic 0 (Foundation Scaffold) complete and shipped: Stories 0.1 (repo scaffold + CI), 0.2 (DB schema/migrations), 0.3 (auth: RS256 JWT, RBAC, admin MFA, rate limiting) all built, verified, committed, pushed, CI green (5/5 jobs).
Real bugs found and fixed along the way (see .logs/decisions.md and .logs/activity.md for detail): docker-compose networking (DB_HOST=localhost breaking container-to-container), 6 CI/security-scanner config issues, 1 real dependency CVE (postgresql driver).
Deferred/untouched: the skills/ -> .claude/.skills/ migration question from session start (user chose to leave as-is) — still unresolved, revisit if it starts causing friction.
Next session (Epic 1 — Installer Onboarding & Verification, per docs/stories-shams-ma.md): coverage-zone setting (base location + radius, ST_DWithin matching), certification document upload (MinIO), admin verification queue. Will need real geocoding behavior wired up (Nominatim, currently just a config placeholder) since homeowner/installer location fields are stubbed.
