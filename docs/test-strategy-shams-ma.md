# Test Strategy: Shams.ma
**PRD**: docs/prd-shams-ma.md | **Architecture**: docs/architecture-shams-ma.md | **Security**: docs/security-shams-ma.md
**Version**: 1.0 | **Date**: 2026-07-21 | **Author**: Test Architect

## 1. Risk Assessment
| Component | Impact | Frequency | Complexity | Risk | Test Level |
|---|---|---|---|---|---|
| Payment (CMI checkout + webhook) | Critical (5) | Medium (3) | High (5) | 13 | Maximum |
| Auth (JWT, MFA-for-admin) | Critical (5) | Low (2) | Medium (3) | 10 | High |
| Certification review (admin approve/reject) | High (4) | Low (2) | Low (2) | 8 | Standard |
| Quote request → booking workflow | High (4) | Medium (3) | Medium (3) | 10 | High |
| Coverage-zone matching (PostGIS radius) | Medium (3) | Low (2) | Medium (3) | 8 | Standard |
| ROI calculator | Medium (3) | Low (2) | Low (2) | 7 | Standard |
| Installer browse / listing | Low (2) | Low (2) | Low (1) | 5 | Standard |
| File upload (certification docs) | High (4) | Low (2) | Medium (3) | 9 | High |

## 2. Test Pyramid Targets
| Layer | Coverage Target | Tooling |
|---|---|---|
| Unit | ≥ 60% of business logic | JUnit 5 (backend), Vitest (React frontend) |
| Integration | ≥ 40% of API + DB layer | Spring Boot Test + Testcontainers (real Postgres/PostGIS) |
| E2E | Critical happy paths only | Playwright |
| **Combined gate** | **≥ 80%** — non-negotiable | CI blocks merge if below (JaCoCo for backend, Vitest coverage for frontend) |

## 3. ATDD Acceptance Scenarios (critical paths)

```gherkin
Feature: Homeowner booking with payment

  Scenario: Successful booking after quote acceptance
    Given a homeowner has an accepted quote from a verified installer
    When the homeowner confirms booking and completes CMI payment
    Then the booking status becomes "BOOKED" only after the payment webhook is received and verified
    And both homeowner and installer receive a confirmation email

  Scenario: Payment fails at CMI
    Given a homeowner is completing a booking payment
    When CMI reports payment failure
    Then the booking remains "PENDING_PAYMENT"
    And the homeowner sees a retry option

  Scenario: Duplicate webhook delivery
    Given a payment has already been marked "SUCCEEDED" for a transaction ID
    When the same CMI webhook is delivered again
    Then the system processes it idempotently and does not double-book or double-charge

Feature: Installer certification verification

  Scenario: Admin approves a pending certification
    Given an installer has submitted a certification document
    When an admin approves it
    Then the installer's verification_status becomes "APPROVED"
    And the installer becomes visible in homeowner browse results
    And an audit_log entry is recorded

  Scenario: Unverified installer is never publicly discoverable
    Given an installer's verification_status is "PENDING" or "REJECTED"
    When a homeowner browses installers in that installer's coverage area
    Then that installer does not appear in results

Feature: Coverage-zone matching

  Scenario: Homeowner within installer radius sees the installer
    Given an installer has base_location L and coverage_radius_km R, and is APPROVED
    When a homeowner at a point within R km of L browses installers
    Then that installer appears in the results

  Scenario: Homeowner outside installer radius does not see the installer
    Given the same installer as above
    When a homeowner at a point beyond R km of L browses installers
    Then that installer does not appear in results
```

## 4. Adversarial Checklist (high-risk components only)

### Payment (CMI webhook) — Maximum rigor
- [ ] Webhook without valid signature → rejected, no state change
- [ ] Webhook replay (same transaction ID twice) → idempotent, no double-processing
- [ ] Webhook for a transaction ID that doesn't exist in our system → rejected/logged, no crash
- [ ] Race: homeowner double-clicks "Pay" → only one payment/booking created (double-submit prevention)
- [ ] Payment amount in webhook doesn't match expected deposit amount → rejected, flagged for admin review

### Auth / Authorization — High rigor
- [ ] Installer A attempts to view/respond to Installer B's quote requests (IDOR) → 403
- [ ] Homeowner attempts to book a quote request that isn't theirs → 403
- [ ] Non-admin attempts to hit `/api/v1/admin/*` endpoints → 403
- [ ] Expired JWT access token → 401, refresh flow triggered
- [ ] Admin login without MFA (once enrolled) → rejected

### Certification file upload — High rigor
- [ ] Upload a file with a disallowed MIME type (e.g., .exe renamed to .pdf — check magic bytes, not just extension) → rejected
- [ ] Upload oversized file (> 10MB) → rejected with clear error
- [ ] Path traversal in filename (`../../etc/passwd`) → sanitized, stored safely
- [ ] Access another installer's certification file URL directly → 403 (signed URL scoped/expiring)

### Quote/Booking workflow — High rigor
- [ ] Attempt to book a quote request that's still "REQUESTED" (skip "QUOTED" step) → rejected, invalid state transition
- [ ] Attempt to respond to a quote request twice → second response rejected or overwrites cleanly (defined behavior, not undefined)

## 5. Release Gate Criteria
- [ ] All acceptance scenarios above pass
- [ ] Combined unit + integration coverage ≥ 80%
- [ ] No critical/high security findings open (from Security baseline + adversarial review)
- [ ] E2E happy path passes for all 3 roles (homeowner booking, installer lead response, admin cert approval) — recorded per Video Recording workflow at version completion
