# Architecture: Shams.ma
**PRD Reference**: docs/prd-shams-ma.md
**System Design Reference**: docs/system-design-shams-ma.md
**Version**: 1.0 | **Date**: 2026-07-21 | **Author**: Software Architect

## 1. Overview
A single Spring Boot monolith (Layered architecture, package-by-feature) serving three role-based frontends from one React SPA. CRUD-dominant domain with one non-trivial workflow (quote → booking → payment) — does not justify DDD, CQRS, or hexagonal ports/adapters at this stage.

## 2. Architecture Decision Records

### ADR-1: Layered Architecture, Package-by-Feature
- **Context**: Domain is CRUD-heavy (installers, homeowners, quotes, bookings) with one multi-step workflow (booking+payment). No multiple bounded contexts, no need for port/adapter isolation (only one primary I/O adapter: REST).
- **Decision**: Standard Layered architecture (Controller → Service → Repository → Entity), packages organized by feature (`installer`, `homeowner`, `booking`, `payment`, `admin`, `auth`, `notification`, `shared`) rather than by technical layer.
- **Alternatives**: Hexagonal/Clean Architecture — rejected, no multiple adapters or complex domain logic to justify the extra indirection. DDD aggregates — rejected, entities are simple enough for plain JPA entities + services.
- **Consequences**: + Fast to build, easy to navigate for a small team. − Less isolation if we later need to swap persistence or add a second I/O adapter (e.g., a queue consumer) — acceptable trade-off now.
- **Re-evaluate when**: A second bounded context emerges (e.g., a separate installer-CRM product) or business logic complexity grows enough that domain rules get tangled with persistence/HTTP concerns.

### ADR-2: Stateless JWT Authentication
- **Context**: Three roles (homeowner, installer, admin) need authenticated, authorized access; single backend, no need for cross-service session sharing beyond what JWT already gives.
- **Decision**: Spring Security with stateless JWT (role claim embedded), validated per-request. No server-side session store.
- **Alternatives**: Server-side sessions — rejected, adds session-store infra (Redis) for no benefit at this scale.
- **Consequences**: + Simple, horizontally scalable without sticky sessions. − Token revocation requires short expiry + refresh flow (handled in Security doc).
- **Re-evaluate when**: Need for instant token revocation (e.g., compliance requirement) arises.

### ADR-3: Module Boundaries via Package + Service Interfaces (not physical separation)
- **Context**: Single monolith (per System Design SDR-1), but modules should stay decoupled enough to split out later if needed.
- **Decision**: Each feature package exposes its functionality via a Service interface; other packages call through the service, never direct repository/entity access across package boundaries.
- **Alternatives**: No enforced boundary (import anything from anywhere) — rejected, leads to unmaintainable coupling as the codebase grows.
- **Consequences**: + Keeps future extraction to a separate service realistic. − Slightly more interfaces than a fully open monolith.
- **Re-evaluate when**: N/A — this is a low-cost discipline, not a scale trade-off.

### ADR-4: React Frontend — Feature-Folder Structure, No Global State Library
- **Context**: Three distinct role-based UIs (homeowner, installer, admin) sharing an auth/layout shell; moderate state complexity (forms, lists, a multi-step booking flow).
- **Decision**: React + TypeScript, routed by role (`/homeowner/*`, `/installer/*`, `/admin/*`), feature-folder structure, server state via React Query (or SWR), local/UI state via component state — no Redux/global store.
- **Alternatives**: Redux/global state — rejected, no cross-cutting client state complex enough to justify it; server state (the bulk of app state) is better handled by a data-fetching library with caching built in.
- **Consequences**: + Less boilerplate, faster to build. − If truly complex cross-feature client state emerges, revisit.
- **Re-evaluate when**: State sharing across unrelated features becomes painful to prop-drill or duplicate fetch.

## 3. System Design Summary
```
[React SPA] → [Spring Boot Monolith] → [PostgreSQL + PostGIS]
                        ↓        ↓            ↓
                 [Object Storage] [CMI]  [Geocoding API]
                        ↓
                  [SMTP Provider]
```
(Full topology + NFRs: see docs/system-design-shams-ma.md)

## 4. Data Model (high-level — full schema owned by DBA)
```
User (base: id, email, password_hash, role) ──1:1──> Homeowner | Installer | Admin (profile)
Installer ──1:N──> CertificationDocument
Installer ──1:1──> CoverageZone (lat, lng, radius_km)
Homeowner ──1:N──> QuoteRequest
QuoteRequest ──N:1──> Installer
QuoteRequest ──1:1──> Booking (nullable until quote accepted)
Booking ──1:1──> Payment
```

## 5. API Design (representative — full contract in stories)
| Method | Endpoint | Description | Auth |
|---|---|---|---|
| POST | /api/v1/auth/register | Register (role-specific) | Public |
| POST | /api/v1/auth/login | Login, issue JWT | Public |
| GET | /api/v1/roi/estimate | ROI/payback calculation | Public |
| GET | /api/v1/installers | Browse verified installers by location | Public |
| POST | /api/v1/installers/{id}/coverage-zone | Set coverage zone | Installer (owner) |
| POST | /api/v1/installers/{id}/certifications | Upload cert document | Installer (owner) |
| PATCH | /api/v1/admin/certifications/{id} | Approve/reject cert | Admin |
| POST | /api/v1/quote-requests | Homeowner requests quote(s) | Homeowner |
| GET | /api/v1/installers/{id}/quote-requests | Installer's lead inbox | Installer (owner) |
| POST | /api/v1/quote-requests/{id}/respond | Installer quotes or declines | Installer (owner) |
| POST | /api/v1/quote-requests/{id}/book | Homeowner books + initiates payment | Homeowner (owner) |
| POST | /api/v1/payments/webhook | CMI payment confirmation callback | CMI (signature-verified) |
| GET | /api/v1/admin/bookings | Booking/payment overview | Admin |

## 6. Security Considerations (summary — full baseline from Security Engineer)
- Authentication: stateless JWT (ADR-2); Authorization: role-based, resource-ownership checks on installer/homeowner-scoped endpoints
- Payment data: never touches our servers directly (CMI hosted/tokenized checkout); webhook signature verification required
- File uploads (cert documents): type/size validation, stored in object storage, not served directly from app server

## 7. Infrastructure
- Hosting: single-region cloud VM or managed container platform (decided in DevOps doc)
- Database: managed PostgreSQL + PostGIS
- CI/CD: GitHub Actions (lint → test w/ coverage gate → security scan → build Docker image → deploy)
- Monitoring: structured JSON logs to hosting-provider aggregator (per System Design)

## 8. Technical Risks
| Risk | Mitigation | Owner |
|---|---|---|
| PostGIS radius query performance at scale | Index on geography column; not a concern at MVP volume, monitor | Backend Dev |
| Webhook delivery failure (payment confirmed but webhook lost) | CMI reconciliation/status-poll fallback job (nightly) | Backend Dev |
| Cross-module coupling creep in monolith | Enforce service-interface boundary (ADR-3) via code review | Tech Lead |
