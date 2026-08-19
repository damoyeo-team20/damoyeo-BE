package com.damoyeo.meeting.domain;

import com.damoyeo.common.domain.BaseEntity;
import com.damoyeo.group.domain.GroupMember;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Column;
import java.time.Instant;

@Entity
@Table(
        name = "meeting_participants",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_meeting_participants_meeting_member",
                columnNames = {"meeting_id", "group_member_id"}
        )
)
public class MeetingParticipant extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meeting_id", nullable = false)
    private Meeting meeting;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_member_id", nullable = false)
    private GroupMember groupMember;

    @Column(name = "availability_submitted_at")
    private Instant availabilitySubmittedAt;

    protected MeetingParticipant() {
    }

    public MeetingParticipant(Meeting meeting, GroupMember groupMember) {
        this.meeting = meeting;
        this.groupMember = groupMember;
    }

    public Long getId() {
        return id;
    }

    public Meeting getMeeting() {
        return meeting;
    }

    public GroupMember getGroupMember() {
        return groupMember;
    }

    public void markAvailabilitySubmitted() {
        availabilitySubmittedAt = Instant.now();
    }

    public Instant getAvailabilitySubmittedAt() {
        return availabilitySubmittedAt;
    }
}
