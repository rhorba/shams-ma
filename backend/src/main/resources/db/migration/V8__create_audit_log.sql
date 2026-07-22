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
