DROP INDEX IF EXISTS uk_group_members_single_host;

CREATE UNIQUE INDEX uk_group_members_single_host
    ON group_members (group_id)
    WHERE role = 'HOST';

ALTER TABLE group_members DROP COLUMN status;
ALTER TABLE meetings DROP COLUMN preference_survey_deadline;
