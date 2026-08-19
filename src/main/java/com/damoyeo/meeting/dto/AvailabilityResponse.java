package com.damoyeo.meeting.dto;

import com.damoyeo.meeting.domain.MeetingStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record AvailabilityResponse(
        Long meetingId,
        Long meetingParticipantId,
        List<LocalDate> availableDates,
        Instant submittedAt,
        MeetingStatus meetingStatus
) {
}
