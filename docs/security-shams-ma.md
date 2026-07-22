# Security Baseline: Shams.ma
**Architecture Reference**: docs/architecture-shams-ma.md
**Version**: 1.0 | **Date**: 2026-07-21 | **Author**: Security Engineer

## 1. Threat Model (5-Minute)
- **What are we building?** A marketplace handling PII (addresses, contact info), certification documents, and payment deposits (via CMI) for homeowners and installers in Morocco.
- **Who would attack it?** Mostly opportunistic (script kiddies scanning for common web vulns, credential stuffing); low probability but non-zero: a competitor scraping installer/lead data, or a malicious installer submitting forged certification documents.
- **Worst outcome?** Financial fraud via payment manipulation (e.g., forging a webhook to fake a payment), data leak of homeowner PII/addresses, or a fraudulent installer getting verified and defrauding homeowners.

## 2. STRIDE Analysis (top risks only)
| Threat | Component | Mitigation | Status |
|---|---|---|---|
| Spoofing | CMI payment webhook | Verify webhook signature (HMAC) against CMI shared secret; reject unsigned/invalid requests | TODO |
| Tampering | Quote/booking status transitions | Server-side state machine validation (e.g., can't jump Requested → Booked without a Quoted step + confirmed payment) | TODO |
| Repudiation | Admin certification approve/reject | Audit log: who approved/rejected, when, previous status | TODO |
| Info Disclosure | Homeowner address / installer cert documents | Authorization checks per-resource (IDOR prevention); cert documents served via signed/expiring URLs, not public bucket paths | TODO |
| DoS | Public ROI-calculator + installer-browse endpoints (unauthenticated) | Rate limiting per-IP on public endpoints | TODO |
| Elevation of Privilege | Role-based endpoints (installer/homeowner/admin) | JWT role claim validated server-side on every request; ownership check (not just role check) on resource-scoped endpoints | TODO |

## 3. Authentication Strategy
- **Type**: JWT (per Architecture ADR-2) — access token ≤ 15min, refresh token ≤ 7 days, RS256 signing
- **MFA**: Required for **Admin** role (TOTP) — admin approves certifications and can view payment/booking data, elevated risk justifies it. Not required for Homeowner/Installer in MVP (YAGNI — revisit if fraud patterns emerge).
- **Password policy**: bcrypt/argon2id hashing, minimum 10 characters, checked against a common-breached-password list at registration
- **Session management**: refresh token stored HttpOnly+Secure+SameSite=Strict cookie; access token in memory (not localStorage, to reduce XSS token theft risk)

## 4. Authorization Model
- **Pattern**: Simple roles (Homeowner / Installer / Admin) + per-resource ownership checks — RBAC would be over-engineering for 3 fixed roles with no dynamic permission combinations
- **Roles defined**: Homeowner, Installer, Admin
- **Resource-level checks**: Yes — e.g., an Installer can only view/respond to quote requests addressed to them; a Homeowner can only view/book their own quote requests

## 5. Data Protection
- **PII fields**: homeowner name/email/phone/address, installer business contact info, certification documents
- **Encryption at rest**: managed PostgreSQL with disk-level encryption (provider default); object storage with server-side encryption enabled
- **Encryption in transit**: HTTPS enforced everywhere, HSTS enabled, TLS termination at load balancer
- **Secrets management**: environment variables injected at deploy time (per `.env.example`), never committed; production secrets in the hosting platform's secret store (not plain env files on disk)

## 6. Security Requirements for Dev Team
- [ ] All inputs validated server-side (Bean Validation annotations on DTOs)
- [ ] Output encoded for context (React escapes by default; backend never returns raw HTML from user input)
- [ ] No secrets in code, logs, or error messages (scrub stack traces returned to clients)
- [ ] HTTPS only, security headers configured (HSTS, X-Content-Type-Options, X-Frame-Options, CSP baseline)
- [ ] Dependencies scanned in CI (SCA — see DevOps doc)
- [ ] File upload validation: whitelist MIME types (PDF/JPEG/PNG), max size (e.g., 10MB), stored outside webroot, served via time-limited signed URLs
- [ ] CMI webhook endpoint: signature verification mandatory, idempotency check (don't double-process the same payment event)
- [ ] Rate limiting on: login, registration, public ROI/browse endpoints
