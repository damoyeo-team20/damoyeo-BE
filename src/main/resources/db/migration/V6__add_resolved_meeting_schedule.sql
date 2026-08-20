ALTER TABLE meetings
    ADD COLUMN resolved_start_at TIMESTAMPTZ,
    ADD COLUMN resolved_end_at TIMESTAMPTZ;
