package com.damoyeo.group.dto;

import com.damoyeo.group.domain.GroupMemberRole;

public record JoinGroupResponse(
        Long groupId,
        String groupName,
        Long groupMemberId,
        GroupMemberRole role,
        boolean alreadyMember
) {
}
