package com.damoyeo.group.dto;

import com.damoyeo.group.domain.GroupMember;
import com.damoyeo.group.domain.GroupMemberRole;
import com.damoyeo.group.domain.MeetingGroup;
import java.time.Instant;
import java.util.List;

public record GroupDetailResponse(
        Long id,
        String name,
        String inviteCode,
        List<MemberResponse> members,
        Instant createdAt
) {
    public static GroupDetailResponse of(MeetingGroup group, List<GroupMember> members) {
        return new GroupDetailResponse(
                group.getId(),
                group.getName(),
                group.getInviteCode(),
                members.stream().map(MemberResponse::from).toList(),
                group.getCreatedAt()
        );
    }

    public record MemberResponse(Long memberId, Long userId, GroupMemberRole role) {
        public static MemberResponse from(GroupMember member) {
            return new MemberResponse(member.getId(), member.getUserId(), member.getRole());
        }
    }
}
