package com.damoyeo.group.dto;

import com.damoyeo.group.domain.GroupMemberRole;
import java.time.Instant;
import java.util.List;

public record GroupSummaryResponse(
        Long id,
        String name,
        int memberCount,
        List<MemberResponse> members,
        LastMeetingResponse lastMeeting,
        Instant createdAt
) {
    public record MemberResponse(Long userId, String nickname, GroupMemberRole role) {
    }

    public record LastMeetingResponse(Instant confirmedStartAt, String region) {
    }
}
