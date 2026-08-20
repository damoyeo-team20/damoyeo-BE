package com.damoyeo.meeting.dto;

import com.damoyeo.meeting.domain.PreferredTimeOfDay;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record UpdateMeetingRequest(
        @Size(max = 1000, message = "모임 목적은 1000자 이하여야 합니다.")
        String purpose,
        @Size(max = 100, message = "지역은 100자 이하여야 합니다.")
        String region,
        @NotNull(message = "일정 탐색 시작일은 필수입니다.")
        LocalDate scheduleSearchFrom,
        @NotNull(message = "일정 탐색 종료일은 필수입니다.")
        LocalDate scheduleSearchTo,
        @NotNull(message = "희망 시간대는 필수입니다.")
        PreferredTimeOfDay preferredTimeOfDay
) {
}
