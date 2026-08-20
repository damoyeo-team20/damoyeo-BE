package com.damoyeo.meeting.domain;

import com.damoyeo.common.domain.BaseEntity;
import com.damoyeo.common.exception.BusinessException;
import com.damoyeo.group.domain.MeetingGroup;
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
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.http.HttpStatus;

@Entity
@Table(name = "meetings")
public class Meeting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private MeetingGroup group;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(length = 1000)
    private String purpose;

    @Column(length = 100)
    private String region;

    @Column(name = "schedule_search_from")
    private LocalDate scheduleSearchFrom;

    @Column(name = "schedule_search_to")
    private LocalDate scheduleSearchTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_time_of_day", length = 30)
    private PreferredTimeOfDay preferredTimeOfDay;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MeetingStatus status;

    @Column(name = "confirmed_start_at")
    private Instant confirmedStartAt;

    @Column(name = "confirmed_end_at")
    private Instant confirmedEndAt;

    protected Meeting() {
    }

    public static Meeting draft(MeetingGroup group, long createdBy) {
        Meeting meeting = new Meeting();
        meeting.group = group;
        meeting.createdBy = createdBy;
        meeting.status = MeetingStatus.DRAFT;
        return meeting;
    }

    public void updateConditions(
            String purpose,
            String region,
            LocalDate scheduleSearchFrom,
            LocalDate scheduleSearchTo,
            PreferredTimeOfDay preferredTimeOfDay
    ) {
        requireDraft();
        this.purpose = trimToNull(purpose);
        this.region = trimToNull(region);
        this.scheduleSearchFrom = scheduleSearchFrom;
        this.scheduleSearchTo = scheduleSearchTo;
        this.preferredTimeOfDay = preferredTimeOfDay;
    }

    public void submit(boolean hasParticipants) {
        requireDraft();
        if (purpose == null || region == null) {
            throw new BusinessException("INCOMPLETE_MEETING", "모임 목적과 지역을 입력해야 합니다.", HttpStatus.BAD_REQUEST);
        }
        if (scheduleSearchFrom == null || scheduleSearchTo == null) {
            throw new BusinessException("SEARCH_PERIOD_REQUIRED", "일정 탐색 시작일과 종료일을 입력해야 합니다.", HttpStatus.BAD_REQUEST);
        }
        if (scheduleSearchFrom.isAfter(scheduleSearchTo)) {
            throw new BusinessException("INVALID_SEARCH_PERIOD", "일정 탐색 시작일은 종료일보다 늦을 수 없습니다.", HttpStatus.BAD_REQUEST);
        }
        if (preferredTimeOfDay == null) {
            throw new BusinessException("PREFERRED_TIME_REQUIRED", "희망 시간대를 선택해야 합니다.", HttpStatus.BAD_REQUEST);
        }
        if (!hasParticipants) {
            throw new BusinessException("PARTICIPANT_REQUIRED", "참여자를 한 명 이상 선택해야 합니다.", HttpStatus.BAD_REQUEST);
        }
        status = MeetingStatus.SURVEYING;
    }

    public void completeAvailabilityCollection() {
        if (status != MeetingStatus.SURVEYING) {
            throw new BusinessException("AVAILABILITY_NOT_COLLECTING", "가능 날짜를 수집 중인 일정이 아닙니다.", HttpStatus.CONFLICT);
        }
        status = MeetingStatus.READY_TO_PLAN;
    }

    public void ensureCollectingAvailability() {
        if (status != MeetingStatus.SURVEYING) {
            throw new BusinessException("AVAILABILITY_NOT_COLLECTING", "가능 날짜를 제출할 수 없는 상태입니다.", HttpStatus.CONFLICT);
        }
    }

    public void startPlanning() {
        if (status != MeetingStatus.READY_TO_PLAN) {
            throw new BusinessException("MEETING_NOT_READY", "조율을 시작할 수 없는 상태입니다.", HttpStatus.CONFLICT);
        }
        status = MeetingStatus.PLANNING;
    }

    public void applyAiPurpose(String purpose) {
        requireDraft();
        if (purpose == null || purpose.isBlank() || purpose.length() > 1000) {
            throw new BusinessException("AI_RESPONSE_INVALID", "AI 응답 형식이 올바르지 않습니다.", HttpStatus.BAD_GATEWAY);
        }
        this.purpose = purpose.trim();
    }

    public void completePlanning() {
        if (status != MeetingStatus.PLANNING) {
            throw new BusinessException("MEETING_NOT_PLANNING", "조율 중인 일정이 아닙니다.", HttpStatus.CONFLICT);
        }
        status = MeetingStatus.PROPOSING;
    }

    public void restoreReadyToPlan() {
        if (status != MeetingStatus.PLANNING) {
            throw new BusinessException("MEETING_NOT_PLANNING", "조율 중인 일정이 아닙니다.", HttpStatus.CONFLICT);
        }
        status = MeetingStatus.READY_TO_PLAN;
    }

    public void ensureEditable() {
        requireDraft();
    }

    private void requireDraft() {
        if (status != MeetingStatus.DRAFT) {
            throw new BusinessException("MEETING_NOT_EDITABLE", "초안 상태의 일정만 수정할 수 있습니다.", HttpStatus.CONFLICT);
        }
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public Long getId() {
        return id;
    }

    public MeetingGroup getGroup() {
        return group;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public String getPurpose() {
        return purpose;
    }

    public String getRegion() {
        return region;
    }

    public LocalDate getScheduleSearchFrom() {
        return scheduleSearchFrom;
    }

    public LocalDate getScheduleSearchTo() {
        return scheduleSearchTo;
    }

    public PreferredTimeOfDay getPreferredTimeOfDay() {
        return preferredTimeOfDay;
    }

    public MeetingStatus getStatus() {
        return status;
    }

    public Instant getConfirmedStartAt() {
        return confirmedStartAt;
    }

    public Instant getConfirmedEndAt() {
        return confirmedEndAt;
    }
}
