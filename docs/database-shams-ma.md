# Database Design: Shams.ma
**Architecture Reference**: docs/architecture-shams-ma.md
**Version**: 1.0 | **Date**: 2026-07-21 | **Author**: DBA

## 1. Database Selection
- **Engine**: PostgreSQL 16 + PostGIS extension
- **Rationale**: Structured, relational data with clear FK relationships; PostGIS gives native geography types + `ST_DWithin` for radius-based coverage-zone matching (PRD FR-2, FR-12) without hand-rolled distance math. YAGNI default (PostgreSQL) plus one concrete geospatial need justifies the PostGIS extension specifically.
- **Hosting**: managed PostgreSQL (single instance for MVP, per System Design)

## 2. Entity-Relationship Model
```
users ──1:1──> homeowners
users ──1:1──> installers
installers ──1:N──> certification_documents
homeowners ──1:N──> quote_requests
installers ──1:N──> quote_requests
quote_requests ──0:1──> bookings
bookings ──1:1──> payments
users (admin) ──1:N──> certification_documents (reviewed_by, nullable)
users ──1:N──> audit_log (actor)
```

## 3. Schema Design
```sql
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS "pgcrypto"; -- for gen_random_uuid()

CREATE TYPE user_role AS ENUM ('HOMEOWNER', 'INSTALLER', 'ADMIN');

CREATE TABLE users (
  id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email          VARCHAR(255) NOT NULL UNIQUE,
  password_hash  VARCHAR(255) NOT NULL,
  role           user_role NOT NULL,
  mfa_secret     VARCHAR(255),          -- non-null only for ADMIN (Security doc: MFA required for Admin)
  created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE homeowners (
  user_id        UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
  full_name      VARCHAR(255) NOT NULL,
  phone          VARCHAR(30),
  address_text   VARCHAR(500) NOT NULL,
  location       GEOGRAPHY(POINT, 4326) NOT NULL,  -- geocoded from address_text
  created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TYPE verification_status AS ENUM ('PENDING', 'APPROVED', 'REJECTED');

CREATE TABLE installers (
  user_id            UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
  business_name      VARCHAR(255) NOT NULL,
  phone              VARCHAR(30),
  verification_status verification_status NOT NULL DEFAULT 'PENDING',
  base_location      GEOGRAPHY(POINT, 4326),        -- coverage-zone center; nullable until installer sets it
  coverage_radius_km NUMERIC(6,2),                  -- nullable until installer sets it
  created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at         TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE certification_documents (
  id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  installer_id   UUID NOT NULL REFERENCES installers(user_id) ON DELETE CASCADE,
  file_url       VARCHAR(1000) NOT NULL,   -- object storage key, not public URL
  status         verification_status NOT NULL DEFAULT 'PENDING',
  reviewed_by    UUID REFERENCES users(id),
  reviewed_at    TIMESTAMPTZ,
  uploaded_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TYPE quote_status AS ENUM ('REQUESTED', 'QUOTED', 'DECLINED', 'BOOKED');

CREATE TABLE quote_requests (
  id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  homeowner_id   UUID NOT NULL REFERENCES homeowners(user_id),
  installer_id   UUID NOT NULL REFERENCES installers(user_id),
  status         quote_status NOT NULL DEFAULT 'REQUESTED',
  message        VARCHAR(2000),
  roi_estimate_kwh   NUMERIC(10,2),   -- snapshot of ROI calc shown to homeowner at request time
  roi_payback_years  NUMERIC(5,2),
  quote_amount   NUMERIC(12,2),       -- set by installer on response
  quote_notes    VARCHAR(2000),
  responded_at   TIMESTAMPTZ,
  created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TYPE booking_status AS ENUM ('PENDING_PAYMENT', 'BOOKED', 'CANCELLED');

CREATE TABLE bookings (
  id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  quote_request_id UUID NOT NULL UNIQUE REFERENCES quote_requests(id),
  status           booking_status NOT NULL DEFAULT 'PENDING_PAYMENT',
  deposit_amount   NUMERIC(12,2) NOT NULL,
  created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TYPE payment_status AS ENUM ('PENDING', 'SUCCEEDED', 'FAILED', 'REFUNDED');

CREATE TABLE payments (
  id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  booking_id         UUID NOT NULL UNIQUE REFERENCES bookings(id),
  cmi_transaction_id VARCHAR(255) UNIQUE,   -- idempotency key for webhook processing
  amount             NUMERIC(12,2) NOT NULL,
  currency           VARCHAR(3) NOT NULL DEFAULT 'MAD',
  status             payment_status NOT NULL DEFAULT 'PENDING',
  webhook_received_at TIMESTAMPTZ,
  created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at         TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE audit_log (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  actor_user_id UUID NOT NULL REFERENCES users(id),
  action        VARCHAR(100) NOT NULL,      -- e.g. 'CERTIFICATION_APPROVED'
  entity_type   VARCHAR(50) NOT NULL,
  entity_id     UUID NOT NULL,
  previous_status VARCHAR(50),
  new_status      VARCHAR(50),
  created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

## 4. Index Strategy
| Table | Index Name | Columns | Query Pattern |
|---|---|---|---|
| users | idx_users_email (implicit via UNIQUE) | email | login lookup |
| homeowners | idx_homeowners_location | location (GiST) | not directly queried by location today, kept for symmetry/future |
| installers | idx_installers_base_location | base_location (GiST) | `ST_DWithin(base_location, homeowner_point, radius)` coverage match |
| installers | idx_installers_verification_status | verification_status | filter to APPROVED-only on public browse |
| certification_documents | idx_certdocs_installer_status | (installer_id, status) | admin review queue, installer's own doc list |
| quote_requests | idx_qr_installer_status | (installer_id, status) | installer's lead inbox |
| quote_requests | idx_qr_homeowner | (homeowner_id) | homeowner's request history |
| bookings | idx_bookings_status | (status) | admin booking overview filter |
| payments | idx_payments_cmi_txn (implicit via UNIQUE) | cmi_transaction_id | webhook idempotency check |
| audit_log | idx_audit_entity | (entity_type, entity_id) | "who changed this record" lookups |

## 5. Migration Plan
| Migration File | Description | Reversible |
|---|---|---|
| V1__enable_extensions.sql | postgis, pgcrypto | Yes |
| V2__create_users.sql | users table + role enum | Yes |
| V3__create_homeowners.sql | homeowners table | Yes |
| V4__create_installers.sql | installers + verification_status enum | Yes |
| V5__create_certification_documents.sql | cert docs table | Yes |
| V6__create_quote_requests.sql | quote_requests + quote_status enum | Yes |
| V7__create_bookings_payments.sql | bookings, payments, related enums | Yes |
| V8__create_audit_log.sql | audit_log table | Yes |
| V9__indexes.sql | all indexes from section 4 | Yes |

(Flyway naming convention — matches Spring Boot's default migration tool.)

## 6. Access Patterns
| Use Case | Query Pattern | Index Coverage |
|---|---|---|
| Homeowner browses installers near address | `SELECT * FROM installers WHERE verification_status='APPROVED' AND ST_DWithin(base_location, :point, coverage_radius_km * 1000)` | idx_installers_base_location + idx_installers_verification_status |
| Installer's lead inbox | `SELECT * FROM quote_requests WHERE installer_id=:id AND status=:status` | idx_qr_installer_status |
| Admin cert review queue | `SELECT * FROM certification_documents WHERE status='PENDING'` | idx_certdocs_installer_status (partial match via leading column) |
| Webhook idempotency check | `SELECT * FROM payments WHERE cmi_transaction_id=:txn` | UNIQUE index |

## 7. Sensitive Data
- Columns requiring extra care: `password_hash` (bcrypt/argon2id, never logged), `homeowners.address_text`/`location` (PII), `installers.phone`, `certification_documents.file_url` (points to access-controlled object storage, never public)
- Row-level security: not needed for MVP — authorization enforced in the application layer (per Security baseline); revisit only if direct multi-tenant DB access patterns emerge
- Backups: daily automated snapshot (managed Postgres default), encrypted at rest, 30-day retention (per System Design RPO=24h)
