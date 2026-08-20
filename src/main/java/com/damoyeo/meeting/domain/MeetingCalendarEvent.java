package com.damoyeo.meeting.domain;

import com.damoyeo.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "meeting_calendar_events")
public class MeetingCalendarEvent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meeting_id", nullable = false)
    private Meeting meeting;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "google_event_id", length = 255)
    private String googleEventId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CalendarEventStatus status;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    protected MeetingCalendarEvent() {
    }

    private MeetingCalendarEvent(Meeting meeting, long userId, String googleEventId,
                                 CalendarEventStatus status, String failureReason) {
        this.meeting = meeting;
        this.userId = userId;
        this.googleEventId = googleEventId;
        this.status = status;
        this.failureReason = failureReason;
    }

    public static MeetingCalendarEvent success(Meeting meeting, long userId, String googleEventId) {
        return new MeetingCalendarEvent(meeting, userId, googleEventId, CalendarEventStatus.SUCCESS, null);
    }

    public static MeetingCalendarEvent failure(Meeting meeting, long userId, CalendarEventStatus status, String reason) {
        return new MeetingCalendarEvent(meeting, userId, null, status, reason);
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getGoogleEventId() { return googleEventId; }
    public CalendarEventStatus getStatus() { return status; }
    public String getFailureReason() { return failureReason; }
}
