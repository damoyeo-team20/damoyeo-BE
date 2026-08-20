package com.damoyeo.group.dto;

import com.damoyeo.group.domain.GroupMemberRole;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record GroupSummaryResponse(
        Long id,
        String name,
        int memberCount,
        List<MemberResponse> members,
        LastMeetingResponse lastMeeting,
        List<ActiveMeetingResponse> activeMeetings,
        ActiveMeetingResponse activeMeeting,
        Instant createdAt
) {
    public record MemberResponse(Long userId, String nickname, GroupMemberRole role) {
    }

    public record LastMeetingResponse(Instant confirmedStartAt, String region) {
    }

    public record ActiveMeetingResponse(
            Long id,
            String status,
            String purpose,
            String region,
            LocalDate scheduleSearchFrom,
            LocalDate scheduleSearchTo,
            Long createdBy,
            Instant createdAt
    ) {
    }
}
