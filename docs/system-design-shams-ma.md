# System Design: Shams.ma
**PRD Reference**: docs/prd-shams-ma.md
**Version**: 1.0 | **Date**: 2026-07-21 | **Author**: System Designer

## 1. Non-Functional Requirements
| Attribute | Target | Notes |
|---|---|---|
| Availability | 99.9% SLA | Best-effort MVP; single region, active-passive acceptable |
| Latency (p99) | < 500ms | Matches PRD NFR-1; no real-time/low-latency requirement |
| Throughput | ~10 RPS peak | 200 quote-requests/mo + browsing traffic, < 100 concurrent users, 5x peak multiplier |
| Data Volume | < 1 GB/day | Text records + cert document uploads (PDF/image) |
| Retention | Indefinite (business records) | No regulatory retention limit identified for MVP |
| Recovery (RTO) | 4 hours | Acceptable for MVP — manual restore from backup |
| Recovery (RPO) | 24 hours | Daily automated DB backup is sufficient at this scale |

**YAGNI call**: this load (~10 RPS peak, <1GB/day) does not justify a CDN, cache layer, message queue, or multi-region deployment. Single region, single app instance (scalable to 2+ replicas behind a load balancer if needed), managed Postgres.

## 2. Component Topology
```
[Clients: Web (React SPA)]
        ↓ HTTPS
[Reverse Proxy / Load Balancer]  (nginx or cloud LB — TLS termination)
        ↓
[Spring Boot API] (single monolith, Dockerized, 1-2 replicas)
        ↓
        ├── [PostgreSQL + PostGIS]  ← installers, homeowners, quotes, bookings, coverage zones
        ├── [Object Storage (S3-compatible)]  ← installer certification files
        ├── [CMI Payment Gateway]  ← booking deposit (hosted/tokenized checkout + webhook callback)
        ├── [Geocoding API]  ← address → lat/lng for ROI calc + coverage matching
        └── [SMTP Provider]  ← transactional email
        ↓
[Observability: structured logs → hosting-provider log aggregation]
```

No API gateway, no service mesh, no queue — a single Spring Boot service handles all three roles (homeowner/installer/admin) via role-based endpoints. Re-evaluate if traffic or team size grows materially (see SDR-1).

## 3. Integration Patterns
| Integration | Pattern | Reason |
|---|---|---|
| CMI Payments | REST + Webhook | Synchronous checkout redirect, async webhook confirms payment before booking finalizes |
| Geocoding API | REST (sync, request-time) | Needed at ROI-calc and coverage-zone-save time only, low volume |
| SMTP | REST (via provider SDK), fire-and-forget from request thread | Volume is low enough that a queue isn't justified yet (see SDR-2) |
| Object Storage | REST (S3-compatible SDK) | Standard pattern for file upload/download |

## 4. Scalability Strategy
- Scaling approach: vertical first; horizontal (2+ container replicas behind LB) only if CPU/memory saturates
- Cache strategy: none for MVP — no repeated-query bottleneck exists yet
- Queue strategy: none for MVP — email send is low-volume; revisit if send failures/latency become visible (SDR-2)

## 5. System Design Decision Records

### SDR-1: Single Monolith vs. Microservices
- **NFR Driver**: Throughput (~10 RPS) and team size (single small team)
- **Decision**: One Spring Boot monolith with modular packages (installer, homeowner, admin, payment, notification) — not separate services
- **Alternatives**: Microservices per domain — rejected, adds deployment/ops overhead with no scale justification
- **Re-evaluate when**: Team grows beyond ~1 squad per domain, or one module's load pattern diverges sharply from the rest (e.g., needs independent scaling)

### SDR-2: Synchronous Email Send vs. Queue
- **NFR Driver**: Data volume / throughput (low — a handful of emails per quote/booking event)
- **Decision**: Send transactional email synchronously (or via Spring `@Async` thread pool) from the request path, no message broker
- **Alternatives**: Queue (RabbitMQ/SQS) — rejected as premature; adds infra to operate for a volume that doesn't need decoupling yet
- **Re-evaluate when**: Email volume grows enough to cause request latency, or delivery reliability requires retry/backoff beyond simple `@Async` retry

### SDR-3: Payment Confirmation Pattern
- **NFR Driver**: Correctness (booking must not confirm without confirmed payment) + availability (webhook delivery isn't guaranteed instantly)
- **Decision**: Booking status moves to "Booked" only on receipt of CMI's payment-confirmation webhook, not on the client-side redirect alone
- **Alternatives**: Trust client-side redirect success — rejected, spoofable and doesn't guarantee actual payment capture
- **Re-evaluate when**: N/A — this is a correctness requirement, not a scale trade-off

### SDR-4: Deployment Topology
- **NFR Driver**: Availability (99.9%) + RTO/RPO targets
- **Decision**: Single region, Dockerized Spring Boot app (1-2 replicas) + managed PostgreSQL with daily automated backups
- **Alternatives**: Multi-region active-active — rejected, far exceeds MVP availability/RTO needs and adds significant complexity/cost
- **Re-evaluate when**: SLA commitments tighten (e.g., a paying enterprise customer requires 99.99%) or traffic grows beyond single-region capacity
