package com.damoyeo.meeting.dto;

import java.time.LocalDate;
import java.util.List;

public record CalendarBusyDatesResponse(
        boolean calendarConnected,
        List<LocalDate> busyDates
) {
}
