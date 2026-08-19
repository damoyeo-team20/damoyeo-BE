package com.damoyeo.group.dto;

import com.damoyeo.group.domain.MeetingGroup;
import java.time.Instant;

public record GroupSummaryResponse(Long id, String name, Instant createdAt) {
    public static GroupSummaryResponse from(MeetingGroup group) {
        return new GroupSummaryResponse(group.getId(), group.getName(), group.getCreatedAt());
    }
}
