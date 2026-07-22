-- homeowners.location is NOT NULL (per V3) but geocoding address_text -> location is
-- built in Epic 1 (ROI-calc/coverage-matching stories), not Story 0.3 (auth only).
-- A DEFAULT lets registration insert a homeowner row today; Epic 1's geocoding step
-- will UPDATE each row with the real point once it runs.
ALTER TABLE homeowners ALTER COLUMN location SET DEFAULT ST_GeogFromText('POINT(0 0)');
