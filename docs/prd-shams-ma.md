# PRD: Shams.ma — Solar Installer Marketplace
**Version**: 1.0 | **Date**: 2026-07-21 | **Author**: PM | **Status**: Draft

## 1. Problem Statement
Morocco targets 52% renewable energy by 2030 and subsidizes residential solar, but homeowners have no way to find vetted installers, installers have no unified lead-generation channel, and ROI on a solar investment is opaque. Shams.ma is a three-sided marketplace connecting homeowners, verified installers, and admins to close this gap.

## 2. Goals & Success Metrics
| Goal | Metric | Target (6 mo post-launch) |
|---|---|---|
| Verified installer supply | # verified installer profiles | 50+ |
| Homeowner demand | # quote requests/month | 200+ |
| Marketplace liquidity | Quote-request → booked-installer conversion | ≥ 20% |
| Trust | % installers with verified certification | 100% (no unverified listed) |

## 3. User Stories
- As a **homeowner**, I want to enter my address and roof/energy details, so that I get an ROI/payback estimate before requesting quotes.
- As a **homeowner**, I want to see only installers verified to serve my zone, so that I trust who I'm contacting.
- As a **homeowner**, I want to request quotes from multiple installers and book one, so that I can compare offers in one place.
- As a **homeowner**, I want to pay a booking deposit online via CMI when I confirm an installer, so that the booking is secured for both sides.
- As an **installer**, I want to register and submit certification documents, so that I can appear as verified.
- As an **installer**, I want a dashboard of incoming leads/quote requests, so that I can respond and manage my pipeline.
- As an **installer**, I want to define my coverage zone(s), so that I only receive relevant leads.
- As an **admin**, I want to review and approve/reject installer certification submissions, so that only qualified installers are listed.
- As an **admin**, I want visibility into marketplace activity (installers, leads, bookings), so that I can monitor health and resolve disputes.

## 4. Scope
### In Scope (MVP)
- Homeowner: address/energy input, ROI & payback calculator, browse verified installers by coverage zone, request quotes, book an installer
- Installer: registration, certification upload, coverage-zone definition, lead/quote inbox, accept/decline/quote response
- Admin: certification review queue (approve/reject), installer directory management, basic activity overview
- Auth: email/password login for all three roles, role-based access
- Notifications: email on new quote request, quote response, certification decision

### Out of Scope (v1)
- Native mobile apps (responsive web only)
- Installer subscription billing / lead-fee monetization logic (pricing model TBD post-MVP)
- Multi-language i18n beyond French/Arabic copy placeholders
- In-app messaging/chat between homeowner and installer (email notification only for v1)
- Reviews/ratings system

## 5. Requirements
### Functional
- FR-1: System calculates estimated ROI/payback period from homeowner-provided address, roof size/orientation (or estimate), and energy usage.
- FR-2: System matches homeowners to installers whose declared coverage zone (geospatial) includes the homeowner's address.
- FR-3: Installers can submit one or more certification documents for admin review; status is Pending/Approved/Rejected.
- FR-4: Only Approved installers are visible/discoverable to homeowners.
- FR-5: Homeowners can submit a quote request to one or more installers; installers see requests in a lead inbox.
- FR-6: Installers can respond to a quote request (quote amount + notes) or decline.
- FR-7: Homeowner can book an installer from among responses; booking status is tracked (Requested → Quoted → Booked → Paid).
- FR-8: On booking confirmation, homeowner pays a deposit via CMI; booking only moves to "Booked" once payment succeeds (webhook-confirmed). Deposit amount = 10% of the installer's quoted price (flat rate, confirmed business rule — no tiering or cap for MVP).
- FR-9: Admin can view all installers and their verification status, and approve/reject pending certifications.
- FR-10: Admin can view payment/booking transaction status for dispute resolution.
- FR-11: Email notification sent on: new quote request (installer), quote response (homeowner), certification decision (installer), payment confirmation (both parties).
- FR-12: Installer coverage zone is defined as a home-base coordinate + radius (km); matching uses PostGIS `ST_DWithin`.

### Non-Functional
- NFR-1: Performance — p95 API response < 500ms under expected MVP load (< 100 concurrent users).
- NFR-2: Security — all endpoints require authentication except public installer browse/ROI calculator; role-based authorization enforced server-side; no raw card data touches our servers (CMI-hosted payment page/tokenization only).
- NFR-3: Accessibility — WCAG 2.1 AA for core homeowner and installer flows.
- NFR-4: Availability — single-region deployment, no formal SLA for MVP (best-effort).

## 6. Constraints & Assumptions
- Stack: Java Spring Boot (backend), React (frontend), PostgreSQL + PostGIS (coverage-zone geospatial queries), Docker for deployment (decided this session — see Architecture doc for rationale).
- Assumption: coverage zones are modeled as home-base coordinate + radius (km) per installer — simplest MVP model, matched via PostGIS.
- Assumption: certification documents are files (PDF/image) requiring object storage.
- Assumption: CMI provides a hosted payment page or tokenized API so we never store raw card data.
- Constraint: single market (Morocco) for MVP; currency MAD; French/Arabic as primary languages for copy (English UI acceptable for MVP, translation is a copy task not a blocker).

## 7. Risks
| Risk | Probability | Impact | Mitigation |
|---|---|---|---|
| Low initial installer supply (no verified installers = empty marketplace) | H | H | Admin manually recruits/onboards first ~20 installers before public launch |
| Certification fraud (fake documents) | M | H | Manual admin review for MVP; flag for future automated verification |
| Coverage-zone matching too coarse/wrong installers surfaced | M | M | Start with simple radius-based zones (YAGNI), upgrade to polygons if needed |
| No monetization model defined | H | M | Explicitly out of scope for MVP; revisit after liquidity is proven |
| Payment failures/disputes (deposit charged but booking fails, or refund requests) | M | H | Webhook-driven status sync with CMI; admin manual refund/dispute tool for MVP (no automated refund flow) |

## 8. Timeline
| Milestone | Target Date |
|---|---|
| PRD Approved | 2026-07-21 |
| Architecture Done | 2026-07-21 (same session) |
| Foundation Docs Complete | 2026-07-21 |
| Implementation Start | Session 2 |
| MVP Ready | TBD after story estimation |

## Decisions Log (resolved this session)
- Payments: booking deposit via CMI **is in MVP scope** (webhook-confirmed, hosted/tokenized — no raw card data stored).
- Coverage zones: **radius-from-point** model for MVP (not polygons).
