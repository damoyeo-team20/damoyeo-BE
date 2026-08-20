package com.damoyeo.meeting.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.damoyeo.common.exception.BusinessException;
import com.damoyeo.group.domain.MeetingGroup;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class MeetingTest {

    @Test
    void becomesReadyAfterEveryParticipantConfirmsAvailability() {
        Meeting meeting = completeDraft();

        meeting.submit(true);
        assertThat(meeting.getStatus()).isEqualTo(MeetingStatus.SURVEYING);

        meeting.completeAvailabilityCollection();
        assertThat(meeting.getStatus()).isEqualTo(MeetingStatus.READY_TO_PLAN);
    }

    @Test
    void startsPlanningOnlyWhenReady() {
        Meeting meeting = completeDraft();
        meeting.submit(true);

        assertThatThrownBy(meeting::startPlanning).isInstanceOf(BusinessException.class);

        meeting.completeAvailabilityCollection();
        meeting.startPlanning();
        assertThat(meeting.getStatus()).isEqualTo(MeetingStatus.PLANNING);
    }

    @Test
    void rejectsSubmissionWithoutParticipants() {
        Meeting meeting = completeDraft();

        assertThatThrownBy(() -> meeting.submit(false))
                .isInstanceOf(BusinessException.class)
                .hasMessage("참여자를 한 명 이상 선택해야 합니다.");
    }

    @Test
    void rejectsSubmissionWithoutSearchPeriod() {
        Meeting meeting = Meeting.draft(new MeetingGroup("대학교 동기", "ABCDEFGH"), 1L);
        meeting.updateConditions(
                "오랜만에 저녁 식사",
                "건대",
                null,
                null,
                null
        );

        assertThatThrownBy(() -> meeting.submit(true))
                .isInstanceOf(BusinessException.class)
                .hasMessage("일정 탐색 시작일과 종료일을 입력해야 합니다.");
    }

    @Test
    void submitsWithoutPurposeBecausePurposeIsCreatedAfterAvailabilityCollection() {
        Meeting meeting = Meeting.draft(new MeetingGroup("대학교 동기", "ABCDEFGH"), 1L);
        meeting.updateConditions(
                null,
                "건대",
                LocalDate.of(2026, 8, 23),
                LocalDate.of(2026, 9, 7),
                PreferredTimeOfDay.EVENING
        );

        meeting.submit(true);

        assertThat(meeting.getStatus()).isEqualTo(MeetingStatus.SURVEYING);
    }

    @Test
    void rejectsSubmissionWithoutPreferredTimeOfDay() {
        Meeting meeting = Meeting.draft(new MeetingGroup("대학교 동기", "ABCDEFGH"), 1L);
        meeting.updateConditions(
                "오랜만에 저녁 식사",
                "건대",
                LocalDate.of(2026, 8, 23),
                LocalDate.of(2026, 9, 7),
                null
        );

        assertThatThrownBy(() -> meeting.submit(true))
                .isInstanceOf(BusinessException.class)
                .hasMessage("희망 시간대를 선택해야 합니다.");
    }

    private Meeting completeDraft() {
        Meeting meeting = Meeting.draft(new MeetingGroup("대학교 동기", "ABCDEFGH"), 1L);
        meeting.updateConditions(
                "오랜만에 저녁 식사",
                "건대",
                LocalDate.of(2026, 8, 23),
                LocalDate.of(2026, 9, 7),
                PreferredTimeOfDay.EVENING
        );
        return meeting;
    }
}
