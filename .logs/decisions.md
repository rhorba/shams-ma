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

## 2026-07-26 (cont.) — Epic 1 CI security-gate fixes
- **netty CVEs (CVE-2026-59901/-55831/-55833/-56745, HIGH)**: transitive via software.amazon.awssdk:netty-nio-client (S3 client's async HTTP client), fixed in netty 4.2.16.Final. Fixed by importing io.netty:netty-bom:4.2.16.Final in dependencyManagement (backend/pom.xml) rather than pinning each of the ~12 individual netty-* artifacts — same "override the transitive version" pattern as the postgresql-driver CVE fix in Epic 0.
- **react-router GHSA-qwww-vcr4-c8h2 (HIGH, fixed in react-router-dom 8.3.0)**: CSRF bypass specific to React Router's RSC/framework-mode server actions. Verified frontend/src/App.tsx uses only plain declarative `<Routes>/<Route>` (no data router, no loaders/actions, no RSC) — vulnerable code path is unreachable in this app. No 7.x patched release exists (only 8.x). Accepted via .trivyignore with justification rather than forcing an unplanned major-version bump of the core routing library under a CI-gate deadline — same "accept with documented rationale" pattern as Epic 0's pebble/golang.org-x-net CVEs. Revisit react-router 8 upgrade as its own planned task.

## 2026-07-27 — Epic 2 brainstorm: Comprehensive scope chosen
- User picked "Comprehensive": region-aware irradiance-by-city ROI formula, roof-orientation multiplier, 20-year savings chart, address-based browse (closes Story 2.2's literal gap), over Simple/Balanced alternatives.
- Refinement pending user confirmation in PLAN: electricity tariff (MAD/kWh) will be a single national blended constant, NOT city-varying — ONEE's residential tariff schedule is national (tiered by consumption bracket, not by region), unlike irradiance which genuinely varies by city; no verified source exists for regional tariff variation so modeling it would be fabricated precision.
- Refinement pending confirmation: standalone ROI-estimate results will NOT get a new DB table this epic — bookings.roi_estimate_kwh/roi_payback_years (per database doc) already exist as a snapshot captured when a quote request is made (Epic 3's job); persisting un-requested calculator runs now has no reader and would be premature (YAGNI).

## 2026-07-28 (cont.) — Epic 5 scope
User chose the "Comprehensive" option for Story 5.1: read-only admin bookings/payments overview + a dedicated `payment_review_flags` table (new entity, own migration) populated by the webhook amount-mismatch path (previously log-only) + resolve/dismiss endpoints (audited via existing shared AuditLogService) + CSV export + search.
Reused existing infra instead of building new: `AuditLogService`/`audit_log` table (V8, already exists) for the resolve/dismiss action trail; `HomeownerService.getSummary`/`InstallerService.getSummary` (existing cross-package pattern from Epic 3) for names in the overview; existing `/api/v1/admin/**` -> ROLE_ADMIN_MFA matcher in SecurityConfig (no security config change needed).

## 2026-07-28 (cont.) — Epic 5 plan confirmed
User approved Batches 1-4 as planned. For the rule-9 video-recording requirement (Epic 5 completes every story in stories-shams-ma.md, i.e. MVP version-completion): user chose option B — skip the Playwright video recording this session, log it as an explicitly deferred gap/follow-up rather than adding new Playwright tooling now or doing an unrecorded manual smoke pass.

## 2026-07-29 (cont.) — Post-MVP cleanup scope
User asked to resolve every remaining longstanding open item (deposit rate, skills/ duplication, JaCoCo/JDK25, react-router-dom major bump, deferred Playwright recording) EXCEPT the missing real CMI merchant account, which was explicitly excluded (needs an actual bank relationship, not buildable). Via AskUserQuestion, user confirmed the 10% deposit rate should be kept and documented as the real business rule rather than replaced with a different number or a tiered/capped scheme.
