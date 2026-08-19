package com.damoyeo.group.dto;

import com.damoyeo.group.domain.GroupMember;
import com.damoyeo.group.domain.GroupMemberRole;
import com.damoyeo.group.domain.MeetingGroup;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import com.damoyeo.user.domain.User;

public record GroupDetailResponse(
        Long id,
        String name,
        String inviteCode,
        List<MemberResponse> members,
        Instant createdAt
) {
    public static GroupDetailResponse of(
            MeetingGroup group,
            List<GroupMember> members,
            Map<Long, User> users,
            Map<Long, Long> preferenceCounts
    ) {
        return new GroupDetailResponse(
                group.getId(),
                group.getName(),
                group.getInviteCode(),
                members.stream()
                        .map(member -> MemberResponse.of(
                                member,
                                users.get(member.getUserId()),
                                preferenceCounts.getOrDefault(member.getUserId(), 0L)
                        ))
                        .toList(),
                group.getCreatedAt()
        );
    }

    public record MemberResponse(
            Long memberId,
            Long userId,
            String nickname,
            GroupMemberRole role,
            long preferenceCount
    ) {
        public static MemberResponse of(GroupMember member, User user, long preferenceCount) {
            return new MemberResponse(
                    member.getId(),
                    member.getUserId(),
                    user == null ? null : user.getNickname(),
                    member.getRole(),
                    preferenceCount
            );
        }
    }
}
