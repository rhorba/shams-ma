CREATE TYPE payment_review_flag_status AS ENUM ('OPEN', 'RESOLVED', 'DISMISSED');

CREATE TABLE payment_review_flags (
  id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  payment_id        UUID NOT NULL REFERENCES payments(id),
  reason            VARCHAR(100) NOT NULL,
  expected_amount   NUMERIC(12,2) NOT NULL,
  actual_amount     NUMERIC(12,2) NOT NULL,
  status            payment_review_flag_status NOT NULL DEFAULT 'OPEN',
  resolution_note   VARCHAR(500),
  resolved_by_user_id UUID REFERENCES users(id),
  resolved_at       TIMESTAMPTZ,
  created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payment_review_flags_status ON payment_review_flags(status);
