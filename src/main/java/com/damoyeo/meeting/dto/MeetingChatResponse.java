package com.damoyeo.meeting.dto;

import java.time.Instant;
import java.util.List;

public record MeetingChatResponse(
        String reply,
        List<CandidateDateResponse> candidateDates,
        Instant resolvedStartAt,
        Instant resolvedEndAt
) {
    public record CandidateDateResponse(String date, boolean selected) {}
}
