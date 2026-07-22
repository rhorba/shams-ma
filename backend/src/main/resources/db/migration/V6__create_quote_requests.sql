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
