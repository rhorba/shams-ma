# DECISIONS — Shams.ma



## 2026-07-21
- Stack: Java Spring Boot + React + Docker + PostgreSQL/PostGIS (pivot from Next.js/Drizzle, user-confirmed).
- Architecture: Layered/package-by-feature monolith, stateless JWT, service-interface module boundaries, React (no Redux, React Query for server state).

## 2026-07-22 (cont.) — Story 0.3: Auth judgement calls
- **homeowners.location NOT NULL gap**: V3's `location GEOGRAPHY(POINT,4326) NOT NULL` can't be populated by Story 0.3 (auth-only; geocoding address_text -> location is Epic 1's ROI-calc/coverage-matching job). Resolved via an additive migration V10__homeowners_location_default.sql that sets `DEFAULT ST_GeogFromText('POINT(0 0)')` on the column, and the Homeowner JPA entity simply omits mapping `location` at all — Hibernate's INSERT omits the column, DB applies the default. Epic 1's geocoding step will UPDATE the real point later; no schema change needed then. Chose this over adding hibernate-spatial+JTS (dependency-version risk on Spring Boot 4.1/Hibernate 7.4, unproven compatibility) or a native-SQL side-insert (messier, bypasses JPA transactionally). installers.base_location/coverage_radius_km needed no such fix — already nullable per V4.
- **Breached-password list**: bundled the public SecLists `10k-most-common.txt` (danielmiessler/SecLists, well-known breach-derived corpus) as `backend/src/main/resources/security/common-passwords.txt`, checked via in-memory HashSet at registration. Chose this over a live HaveIBeenPwned k-anonymity API call — no network dependency at registration time, sufficient for MVP scale per Security doc section 3's "common-breached-password-list" requirement (doesn't mandate a live-API check specifically).
- **Self-registration scope**: `/api/v1/auth/register` only accepts HOMEOWNER/INSTALLER roles. ADMIN accounts are provisioned out-of-band (direct DB insert for now) — not an explicit doc requirement, but standard practice for a role with MFA-gated access to payment/booking data; open self-registration to ADMIN would be a real privilege-escalation risk.
- **Admin MFA gating mechanism**: rather than a two-step "TOTP code required at login time" flow (more realistic for production but needs frontend UX that doesn't exist yet), implemented as a JWT claim: `mfaEnrolled` = `(user.mfa_secret != null)`, checked via an extra `ROLE_ADMIN_MFA` Spring Security authority granted only when the claim is true. `/api/v1/admin/**` requires `ROLE_ADMIN_MFA`; `/api/v1/auth/mfa/**` requires only `ROLE_ADMIN` (so an unenrolled admin can still reach the enrollment endpoint). Satisfies the acceptance criterion ("MFA enrollment required before dashboard access") without over-building. A full TOTP-challenge-at-every-login is a natural Epic 1+ enhancement once there's a frontend to build the 2-step login UX.

## 2026-07-22 — CI hardening decisions
- Test-only JWT RSA keypair committed at backend/src/test/resources/keys/ (safe — not a real secret, only used in ephemeral test Spring contexts). Prod/dev keys stay gitignored at backend/src/main/resources/keys/.
- .gitleaksignore and .trivyignore introduced as the accepted mechanism for documenting known-safe/known-accepted security-scanner findings, each entry justified inline. Both apply repo-wide; keep entries narrowly scoped (specific fingerprints/CVE IDs, not blanket rule disables).
- security job's checkout uses fetch-depth: 0 (full history) specifically because gitleaks fingerprints are commit-pinned and need full history for stability — other jobs can stay shallow.

## 2026-07-23 — Epic 1 planning
- Sequencing: plan all 3 stories (1.1-1.3) together, execute sequentially without stopping, single checkpoint at end (user choice).
- GET /api/v1/installers/browse: public, no auth required (lead-gen marketplace browsing pattern) — user confirmed.
- Coverage radius validated 1-200km (no doc-specified bound, judgment call).
- Cert approval: approving any certification_documents row flips installers.verification_status to APPROVED (schema allows multiple certs/installer, but stories describe a single-cert MVP flow).
- File storage: AWS SDK v2 S3Client against MinIO (path-style + endpoint override) rather than MinIO's own SDK — reuses the already-configured FILE_STORAGE_* env vars, standard/well-supported library.
