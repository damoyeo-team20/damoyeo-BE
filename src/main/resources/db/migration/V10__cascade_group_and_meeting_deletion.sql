ALTER TABLE meetings DROP CONSTRAINT meetings_confirmed_suggestion_id_fkey;
ALTER TABLE meetings
    ADD CONSTRAINT meetings_confirmed_suggestion_id_fkey
    FOREIGN KEY (confirmed_suggestion_id) REFERENCES meeting_suggestions(id) ON DELETE SET NULL;

ALTER TABLE meetings DROP CONSTRAINT meetings_group_id_fkey;
ALTER TABLE meetings
    ADD CONSTRAINT meetings_group_id_fkey
    FOREIGN KEY (group_id) REFERENCES meeting_groups(id) ON DELETE CASCADE;

ALTER TABLE group_members DROP CONSTRAINT group_members_group_id_fkey;
ALTER TABLE group_members
    ADD CONSTRAINT group_members_group_id_fkey
    FOREIGN KEY (group_id) REFERENCES meeting_groups(id) ON DELETE CASCADE;

ALTER TABLE meeting_participants DROP CONSTRAINT meeting_participants_group_member_id_fkey;
ALTER TABLE meeting_participants
    ADD CONSTRAINT meeting_participants_group_member_id_fkey
    FOREIGN KEY (group_member_id) REFERENCES group_members(id) ON DELETE CASCADE;
