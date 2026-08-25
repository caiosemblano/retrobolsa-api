ALTER TABLE competitions DROP CONSTRAINT IF EXISTS competitions_status_check;

ALTER TABLE competitions
    ADD CONSTRAINT competitions_status_check
    CHECK (status IN ('draft', 'open', 'closed', 'simulating', 'simulated', 'revealed'));

ALTER TABLE competitions ADD COLUMN IF NOT EXISTS ends_at TIMESTAMP;
