package com.damoyeo.group.dto;

import com.damoyeo.group.domain.GroupMember;
import com.damoyeo.group.domain.GroupMemberRole;
import com.damoyeo.group.domain.MeetingGroup;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import com.damoyeo.user.domain.User;

public record GroupDetailResponse(
        Long id,
        String name,
        String inviteCode,
        int memberCount,
        long pastMeetingCount,
        List<MemberResponse> members,
        List<ActiveMeetingResponse> activeMeetings,
        ActiveMeetingResponse activeMeeting,
        Instant createdAt
) {
    public static GroupDetailResponse of(
            MeetingGroup group,
            List<GroupMember> members,
            Map<Long, User> users,
            Map<Long, Long> preferenceCounts,
            Map<Long, Boolean> calendarConnections,
            long pastMeetingCount,
            List<ActiveMeetingResponse> activeMeetings,
            ActiveMeetingResponse activeMeeting
    ) {
        return new GroupDetailResponse(
                group.getId(),
                group.getName(),
                group.getInviteCode(),
                members.size(),
                pastMeetingCount,
                members.stream()
                        .map(member -> MemberResponse.of(
                                member,
                                users.get(member.getUserId()),
                                preferenceCounts.getOrDefault(member.getUserId(), 0L),
                                calendarConnections.getOrDefault(member.getUserId(), false)
                        ))
                        .toList(),
                activeMeetings,
                activeMeeting,
                group.getCreatedAt()
        );
    }

    public record MemberResponse(
            Long memberId,
            Long userId,
            String nickname,
            GroupMemberRole role,
            long preferenceCount,
            boolean calendarConnected
    ) {
        public static MemberResponse of(GroupMember member, User user, long preferenceCount, boolean calendarConnected) {
            return new MemberResponse(
                    member.getId(),
                    member.getUserId(),
                    user == null ? null : user.getNickname(),
                    member.getRole(),
                    preferenceCount,
                    calendarConnected
            );
        }
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
