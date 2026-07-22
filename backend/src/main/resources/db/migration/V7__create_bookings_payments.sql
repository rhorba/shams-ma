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
