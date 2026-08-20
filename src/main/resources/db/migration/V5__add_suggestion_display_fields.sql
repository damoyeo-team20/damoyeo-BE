ALTER TABLE meeting_suggestions
    ADD COLUMN business_hours VARCHAR(500),
    ADD COLUMN business_hours_verified BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN open_at_meeting_time BOOLEAN;
