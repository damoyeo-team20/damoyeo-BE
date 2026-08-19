package com.damoyeo.meeting.domain;

import com.damoyeo.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;

@Entity
@Table(
        name = "meeting_available_dates",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_meeting_available_dates_participant_date",
                columnNames = {"meeting_participant_id", "available_date"}
        )
)
public class MeetingAvailableDate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meeting_participant_id", nullable = false)
    private MeetingParticipant meetingParticipant;

    @Column(name = "available_date", nullable = false)
    private LocalDate availableDate;

    protected MeetingAvailableDate() {
    }

    public MeetingAvailableDate(MeetingParticipant meetingParticipant, LocalDate availableDate) {
        this.meetingParticipant = meetingParticipant;
        this.availableDate = availableDate;
    }

    public Long getId() {
        return id;
    }

    public MeetingParticipant getMeetingParticipant() {
        return meetingParticipant;
    }

    public LocalDate getAvailableDate() {
        return availableDate;
    }
}
