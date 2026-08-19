package com.damoyeo.meeting.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.damoyeo.common.exception.BusinessException;
import com.damoyeo.group.domain.MeetingGroup;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class MeetingTest {

    @Test
    void movesToSurveyingAfterAvailabilityCollectionWhenThePreferenceDeadlineIsActive() {
        LocalDate today = LocalDate.of(2026, 8, 19);
        Meeting meeting = completeDraft(today);

        meeting.submit(today, true);
        assertThat(meeting.getStatus()).isEqualTo(MeetingStatus.COLLECTING_AVAILABILITY);

        meeting.completeAvailabilityCollection(today);

        assertThat(meeting.getStatus()).isEqualTo(MeetingStatus.SURVEYING);
    }

    @Test
    void becomesReadyWithoutAFuturePreferenceDeadline() {
        LocalDate today = LocalDate.of(2026, 8, 19);
        Meeting meeting = completeDraft(null);

        meeting.submit(today, true);
        assertThat(meeting.getStatus()).isEqualTo(MeetingStatus.COLLECTING_AVAILABILITY);

        meeting.completeAvailabilityCollection(today);

        assertThat(meeting.getStatus()).isEqualTo(MeetingStatus.READY_TO_PLAN);
    }

    @Test
    void startsPlanningWhenTheSurveyHasEnded() {
        LocalDate today = LocalDate.of(2026, 8, 20);
        Meeting meeting = completeDraft(today.minusDays(1));
        meeting.submit(today, true);
        meeting.completeAvailabilityCollection(today);

        meeting.startPlanning(today);

        assertThat(meeting.getStatus()).isEqualTo(MeetingStatus.PLANNING);
    }

    @Test
    void cannotStartPlanningUntilTheEndOfTheDeadlineDate() {
        LocalDate today = LocalDate.of(2026, 8, 19);
        Meeting meeting = completeDraft(today);
        meeting.submit(today, true);
        meeting.completeAvailabilityCollection(today);

        assertThatThrownBy(() -> meeting.startPlanning(today))
                .isInstanceOf(BusinessException.class)
                .hasMessage("선호조사가 아직 진행 중입니다.");
    }

    @Test
    void rejectsSubmissionWithoutParticipants() {
        Meeting meeting = completeDraft(null);

        assertThatThrownBy(() -> meeting.submit(LocalDate.of(2026, 8, 19), false))
                .isInstanceOf(BusinessException.class)
                .hasMessage("참여자를 한 명 이상 선택해야 합니다.");
    }

    private Meeting completeDraft(LocalDate surveyDeadline) {
        Meeting meeting = Meeting.draft(new MeetingGroup("대학교 동기", "ABCDEFGH"), 1L);
        meeting.updateConditions(
                "오랜만에 저녁 식사",
                "건대",
                LocalDate.of(2026, 8, 23),
                LocalDate.of(2026, 9, 7),
                PreferredTimeOfDay.EVENING,
                surveyDeadline
        );
        return meeting;
    }
}
