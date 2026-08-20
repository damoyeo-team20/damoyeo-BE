package com.damoyeo.meeting.dto;

import com.damoyeo.meeting.domain.Meeting;
import com.damoyeo.meeting.domain.MeetingStatus;
import java.time.Instant;

public record MeetingListItemResponse(
        Long id,
        String purpose,
        String region,
        Instant confirmedStartAt,
        Instant confirmedEndAt,
        String confirmedPlaceName,
        String confirmedPlaceAddress,
        MeetingStatus status
) {
    public static MeetingListItemResponse from(Meeting meeting) {
        return new MeetingListItemResponse(
                meeting.getId(), meeting.getPurpose(), meeting.getRegion(),
                meeting.getConfirmedStartAt(), meeting.getConfirmedEndAt(),
                meeting.getConfirmedSuggestion() == null ? null : meeting.getConfirmedSuggestion().getName(),
                meeting.getConfirmedSuggestion() == null ? null : meeting.getConfirmedSuggestion().getAddress(),
                meeting.getStatus()
        );
    }
}
