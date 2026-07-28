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

## SESSION_START — 2026-07-23
Request: "continue" (resuming per prior session-end notes).
State verified: working tree clean, git log matches Epic 0 shipped/CI-green record, docs/stories-shams-ma.md Epic 1 (Stories 1.1-1.3) confirmed as next unit of work.

## PLAN — 2026-07-23
User confirmed: Epic 1 (Stories 1.1, 1.2, 1.3) planned together, execute sequentially without stopping. Handing off to EXECUTE via fork agent. See .logs/decisions.md for judgment calls made during planning.

## SESSION_END — 2026-07-23
Epic 1 (Stories 1.1-1.3) EXECUTE phase was delegated to a background fork agent (implementing coverage-zone+geocoding, cert upload, admin verification queue per the plan logged above). Status at session end: UNCONFIRMED — the fork's completion notification surfaced a mid-transcript snippet (fixing a frontend upload test) rather than a final report; a follow-up status request was sent to the fork asking it to confirm whether work is fully implemented/verified/logged/committed/pushed/CI-green, but no reply had arrived before the user ended the session.
Next session: FIRST STEP — check `git log`/`git status`/`gh run list` (or ask the fork agent directly, name/id a328150e276d6a27f, if still resumable) to determine actual Epic 1 completion state before assuming anything shipped. Do not assume Epic 1 is done. If incomplete, resume/finish EXECUTE→VERIFY→SHIP per the plan already recorded in this session's log entries above and in .logs/decisions.md.

## SESSION_START — 2026-07-26
Request: "continue" (resuming per prior session-end notes, fork status a328150e276d6a27f unconfirmed).
Verified: git status shows Epic 1 backend (matches logged Story 1.1-1.3 work) PLUS a full frontend implementation (auth/, features/installer, features/admin, features/browse, lib/api.ts, theme.ts, test files) that was never logged/verified/committed — the fork continued past the last logged checkpoint. Treating this as EXECUTE-complete/unverified; resuming at VERIFY phase before SHIP.

## SESSION_END — 2026-07-27
Epic 1 (Installer Onboarding & Verification) fully shipped. Picked up an unfinished/unlogged fork handoff from 2026-07-23 (backend was logged but a complete frontend layer — auth, coverage-zone form, cert upload, admin queue, browse page — was sitting uncommitted and unverified). Ran full VERIFY (backend unit tests + spotless, frontend lint/tests/coverage 90.27%, secret scan), committed and pushed (0171b8e), then CI's security gate caught 2 real dependency CVEs the local run couldn't see: 4 HIGH netty CVEs (transitive via AWS S3 SDK, fixed via netty-bom 4.2.16.Final override) and 1 HIGH react-router CVE (RSC-mode-only, unreachable in this app's plain client routing, accepted via .trivyignore with rationale). Pushed fix (303388c), CI now green on all 5 jobs (backend/frontend/security/build/deploy-staging).
Note: JaCoCo can no longer run on this dev machine — local JDK is now 25.0.3, JaCoCo 0.8.12 can't instrument JDK25 class files (major version 69), while CI still pins JDK 21. Backend coverage this session was confirmed via the CI gate itself, not locally. Revisit if this keeps happening (bump JaCoCo, or pin a local JDK 21 via sdkman/jenv for backend work).
Next session (Epic 2, per docs/stories-shams-ma.md — check exact story numbers there): booking & ROI calculator is the next unbuilt epic per the original stories doc sequencing. Also unresolved from earlier: skills/ -> .claude/.skills/ duplication (still just left as-is), and a possible future react-router-dom 7->8 major bump to drop the .trivyignore entry.

## SESSION_START — 2026-07-27 (cont., same session — "ok continue")
Request: proceed to next unit of work after Epic 1 ship. Confirmed next per docs/stories-shams-ma.md: Epic 2 (Homeowner Discovery) — Story 2.1 ROI/payback calculator (new), Story 2.2 Browse verified installers (mostly already satisfied by Epic 1's browse endpoint — APPROVED-only + ST_DWithin + empty state all already built; literal gap is "given my address" vs current raw lat/lng-only input).

## SESSION_END — 2026-07-28
Epic 2 (Homeowner Discovery) in progress. Batch 1 (ROI calculator backend) and Batch 2 (ROI calculator frontend, incl. 2 real bugs found+fixed: shared rate-limit bucket across paths, unhandled ConstraintViolationException) fully done — but NOT yet committed. Batch 3 (address-based browse, closing Story 2.2's literal gap) also built but not yet committed:
- Backend: InstallerService/InstallerServiceImpl/InstallerBrowseController now accept an `address` param (geocoded server-side) as an alternative to lat/lng; added validation (400 when neither address nor full lat+lng given). New tests in InstallerServiceImplTest + InstallerCoverageZoneIntegrationTest. Full backend suite (real Docker via PowerShell, Testcontainers) confirmed GREEN just before session end (exit code 0).
- Frontend: BrowsePage.tsx rewritten to a single address field (was raw lat/lng inputs), reads `?lat=&lng=` from the URL so the ROI calculator's "See verified installers" deep link auto-populates. BrowsePage.test.tsx rewritten (address search, empty state, URL-param auto-search, geolocation button) — NOT YET RUN this session (ran out of time before the user asked to stop).
Next session — FIRST STEPS before anything else:
1. Run frontend suite (`npm run lint`, `npx vitest run --coverage` — prefer `--testTimeout=15000` given this sandbox's demonstrated flakiness on unrelated pre-existing tests under parallel load) to confirm Batch 3's BrowsePage changes pass.
2. Since Docker IS reachable via PowerShell (not Bash) on this machine — use PowerShell for backend Maven runs going forward instead of skipping Testcontainers tests.
3. If frontend is green: VERIFY -> SHIP for Epic 2 as a whole (all 3 batches) — commit, push, monitor CI (rule 11), following the exact same pattern as Epic 1's session-end CI-hardening loop. Do not assume CI will be green on the first push; budget time to diagnose/fix.
4. After Epic 2 ships: Epic 3 (Quote & Booking Workflow) is next per docs/stories-shams-ma.md.
No git commits were made this session — everything above is sitting in the working tree uncommitted (verified via `git status`, matches the file list logged in this entry).
