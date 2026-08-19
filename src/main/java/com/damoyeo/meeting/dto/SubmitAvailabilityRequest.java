package com.damoyeo.meeting.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.Set;

public record SubmitAvailabilityRequest(
        @NotEmpty(message = "가능한 날짜를 한 개 이상 선택해야 합니다.")
        Set<@NotNull(message = "날짜는 null일 수 없습니다.") LocalDate> availableDates
) {
}
