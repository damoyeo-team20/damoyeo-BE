package com.damoyeo.meeting.dto;

import com.damoyeo.meeting.domain.PreferredTimeOfDay;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateMeetingRequest(
        @Size(max = 1000, message = "모임 목적은 1000자 이하여야 합니다.")
        String purpose,
        @Size(max = 100, message = "지역은 100자 이하여야 합니다.")
        String region,
        LocalDate scheduleSearchFrom,
        LocalDate scheduleSearchTo,
        PreferredTimeOfDay preferredTimeOfDay
) {
}
