# Stories: Shams.ma
**PRD**: docs/prd-shams-ma.md | **Architecture**: docs/architecture-shams-ma.md | **Database**: docs/database-shams-ma.md
**Test Strategy**: docs/test-strategy-shams-ma.md

## Epic 0: Foundation Scaffold
Repo, CI, Docker, and base auth scaffolding — nothing user-facing yet, but everything else depends on it.

### Story 0.1: Project scaffold + CI pipeline
**Priority**: Must | **Size**: M | **Specialist**: DevOps/DevSecOps

As a developer, I want a working Spring Boot + React repo skeleton with CI, so that every subsequent story lands on a tested, scanned pipeline.

**Acceptance Criteria**:
- [ ] Given a push to any branch, when CI runs, then lint + test + coverage-check + security-scan + Docker build all execute
- [ ] Given coverage < 80%, when CI runs, then the build fails

**Technical Notes**: Per DevOps doc — Maven build, JaCoCo, Semgrep/Trivy/Gitleaks, Dockerfiles for both services, docker-compose for dev.
**Dependencies**: None

### Story 0.2: Database schema + migrations
**Priority**: Must | **Size**: M | **Specialist**: Backend Dev + DBA

As a developer, I want the full schema applied via Flyway migrations, so that the API has tables to work against.

**Acceptance Criteria**:
- [ ] Given a fresh database, when migrations run, then all 9 tables + enums + indexes from the Database doc exist

**Technical Notes**: Migrations V1-V9 per docs/database-shams-ma.md section 5.
**Dependencies**: 0.1

### Story 0.3: Auth (registration, login, JWT, role-based access)
**Priority**: Must | **Size**: L | **Specialist**: Backend Dev + Security Engineer

As a user (any role), I want to register and log in, so that I get a role-scoped session.

**Acceptance Criteria**:
- [ ] Given valid registration data, when a homeowner/installer registers, then a user + role-specific profile row is created and password is hashed (bcrypt/argon2id)
- [ ] Given valid credentials, when a user logs in, then a JWT access token (≤15min) + refresh token (≤7d, HttpOnly cookie) is issued
- [ ] Given a non-admin role, when accessing `/api/v1/admin/*`, then a 403 is returned
- [ ] Given an admin account, when logging in without MFA enrolled, then MFA enrollment is required before dashboard access

**Technical Notes**: Per Architecture ADR-2 and Security doc sections 3-4.
**Dependencies**: 0.2

---

## Epic 1: Installer Onboarding & Verification
Installers register, set up their profile/coverage zone, and get verified by an admin.

### Story 1.1: Installer registration + coverage zone
**Priority**: Must | **Size**: M | **Specialist**: Backend Dev + Frontend Dev

As an installer, I want to register and set my coverage zone (base location + radius), so that I only receive relevant leads.

**Acceptance Criteria**:
- [ ] Given a registered installer, when they set a base location + radius, then it's stored and used for future matching
- [ ] Given the homeowner browse endpoint, when a homeowner's point is within an installer's radius, then that installer appears in results (see Test Strategy coverage-zone scenarios)

**Technical Notes**: `installers.base_location` (PostGIS geography), `ST_DWithin` query per Database doc section 6. Map-pin+radius UI component per UI doc section 3.
**Dependencies**: 0.3

### Story 1.2: Certification upload
**Priority**: Must | **Size**: M | **Specialist**: Backend Dev + Frontend Dev

As an installer, I want to upload certification documents, so that I can get verified.

**Acceptance Criteria**:
- [ ] Given a PDF/JPEG/PNG under 10MB, when an installer uploads it, then it's stored in object storage with status PENDING
- [ ] Given a disallowed file type or oversized file, when uploaded, then it's rejected with a clear error (see Test Strategy adversarial checklist)

**Technical Notes**: Signed/expiring URLs, MIME-type + magic-byte validation per Security doc section 6.
**Dependencies**: 0.3

### Story 1.3: Admin certification review queue
**Priority**: Must | **Size**: M | **Specialist**: Backend Dev + Frontend Dev

As an admin, I want to review and approve/reject pending certifications, so that only qualified installers are listed.

**Acceptance Criteria**:
- [ ] Given a pending certification, when an admin approves it, then installer.verification_status becomes APPROVED and an audit_log entry is recorded
- [ ] Given verification_status is not APPROVED, when a homeowner browses installers, then that installer is excluded (see Test Strategy scenario "Unverified installer is never publicly discoverable")

**Technical Notes**: Audit log per Security doc STRIDE "Repudiation" control.
**Dependencies**: 1.2

---

## Epic 2: Homeowner Discovery
ROI calculation and installer browsing.

### Story 2.1: ROI/payback calculator
**Priority**: Must | **Size**: S | **Specialist**: Backend Dev + Frontend Dev

As a homeowner, I want an ROI/payback estimate from my address and energy usage, so that I can decide if solar is worth it before contacting anyone.

**Acceptance Criteria**:
- [ ] Given an address + monthly bill, when calculated, then an estimated payback period + annual savings is shown
- [ ] Public endpoint — no login required (per PRD scope)

**Technical Notes**: Uses Geocoding API to resolve address → lat/lng (per Architecture section 5, `/api/v1/roi/estimate`).
**Dependencies**: 0.1

### Story 2.2: Browse verified installers
**Priority**: Must | **Size**: S | **Specialist**: Backend Dev + Frontend Dev

As a homeowner, I want to see verified installers covering my address, so that I can choose who to request quotes from.

**Acceptance Criteria**:
- [ ] Given my address, when browsing, then only APPROVED installers within their coverage radius are shown
- [ ] Given no installers cover my area, then an empty state is shown (per UX doc)

**Dependencies**: 1.1, 1.3, 2.1

---

## Epic 3: Quote & Booking Workflow
The core marketplace transaction.

### Story 3.1: Homeowner requests quotes
**Priority**: Must | **Size**: M | **Specialist**: Backend Dev + Frontend Dev

As a homeowner, I want to request quotes from one or more installers, so that I can compare offers.

**Acceptance Criteria**:
- [ ] Given selected installer(s), when a homeowner submits a request, then a quote_request row (status REQUESTED) is created per installer, with the ROI snapshot attached
- [ ] Given a new quote request, then the installer receives an email notification

**Dependencies**: 2.2

### Story 3.2: Installer lead inbox + response
**Priority**: Must | **Size**: M | **Specialist**: Backend Dev + Frontend Dev

As an installer, I want to see and respond to quote requests, so that I can manage my pipeline.

**Acceptance Criteria**:
- [ ] Given a pending request, when an installer quotes an amount, then status becomes QUOTED and the homeowner is notified
- [ ] Given a pending request, when an installer declines, then status becomes DECLINED
- [ ] Given an installer attempts to access another installer's requests, then 403 (IDOR check, per Test Strategy)

**Dependencies**: 3.1

### Story 3.3: Homeowner books an installer
**Priority**: Must | **Size**: S | **Specialist**: Backend Dev + Frontend Dev

As a homeowner, I want to book an installer from a quote, so that I can proceed to payment.

**Acceptance Criteria**:
- [ ] Given a QUOTED request, when the homeowner books it, then a booking row (status PENDING_PAYMENT) is created
- [ ] Given a request still in REQUESTED status, when a booking is attempted, then it's rejected (invalid state transition, per Test Strategy)

**Dependencies**: 3.2

---

## Epic 4: Payments
Booking deposit via CMI — highest-risk component per Test Strategy (Maximum rigor).

### Story 4.1: CMI checkout integration
**Priority**: Must | **Size**: L | **Specialist**: Backend Dev + Security Engineer

As a homeowner, I want to pay a booking deposit via CMI, so that my booking is secured.

**Acceptance Criteria**:
- [ ] Given a PENDING_PAYMENT booking, when the homeowner completes CMI checkout, then a payment row (status PENDING) is created with the CMI transaction ID
- [ ] No raw card data is ever received or stored by our servers (hosted/tokenized checkout only)

**Dependencies**: 3.3

### Story 4.2: CMI webhook handling (idempotent, signature-verified)
**Priority**: Must | **Size**: L | **Specialist**: Backend Dev + Security Engineer

As the system, I want to confirm payment only via verified webhook, so that bookings can't be spoofed into "confirmed."

**Acceptance Criteria** (from Test Strategy ATDD):
- [ ] Given a valid signed webhook reporting success, when received, then payment.status → SUCCEEDED and booking.status → BOOKED, and both parties are emailed
- [ ] Given an invalid/unsigned webhook, when received, then it's rejected with no state change
- [ ] Given a duplicate webhook for an already-SUCCEEDED transaction ID, when received, then it's processed idempotently (no double-booking)
- [ ] Given a webhook reporting failure, when received, then booking stays PENDING_PAYMENT and homeowner sees a retry option

**Dependencies**: 4.1

### Story 4.3: Nightly payment reconciliation job
**Priority**: Should | **Size**: S | **Specialist**: Backend Dev + DevOps

As an admin, I want a nightly job comparing CMI's transaction log to our payments table, so that lost webhooks don't leave bookings stuck.

**Dependencies**: 4.2

---

## Epic 5: Admin Oversight
### Story 5.1: Admin booking/payment overview
**Priority**: Should | **Size**: S | **Specialist**: Backend Dev + Frontend Dev

As an admin, I want to see all bookings and their payment status, so that I can resolve disputes.

**Dependencies**: 4.2

---

## Sprint Allocation
| Sprint | Stories | Estimated Effort |
|---|---|---|
| Sprint 1 (this session, docs only) | Foundation docs (this document chain) | Done |
| Sprint 2 | 0.1, 0.2, 0.3 | ~3-4 days |
| Sprint 3 | 1.1, 1.2, 1.3, 2.1, 2.2 | ~4-5 days |
| Sprint 4 | 3.1, 3.2, 3.3 | ~3 days |
| Sprint 5 | 4.1, 4.2, 4.3, 5.1 | ~4-5 days |
