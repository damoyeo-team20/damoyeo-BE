package com.damoyeo.group.service;

import com.damoyeo.common.exception.BusinessException;
import com.damoyeo.group.domain.GroupMember;
import com.damoyeo.group.domain.MeetingGroup;
import com.damoyeo.group.dto.CreateGroupRequest;
import com.damoyeo.group.dto.GroupDetailResponse;
import com.damoyeo.group.dto.GroupSummaryResponse;
import com.damoyeo.group.dto.JoinGroupResponse;
import com.damoyeo.group.repository.GroupMemberRepository;
import com.damoyeo.group.repository.MeetingGroupRepository;
import com.damoyeo.user.repository.UserRepository;
import com.damoyeo.preference.repository.UserPreferenceRepository;
import com.damoyeo.meeting.domain.Meeting;
import com.damoyeo.meeting.domain.MeetingStatus;
import com.damoyeo.meeting.repository.MeetingRepository;
import com.damoyeo.user.domain.User;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GroupService {

    private static final int MAX_INVITE_CODE_ATTEMPTS = 10;
    private static final String CALENDAR_EVENTS_SCOPE = "https://www.googleapis.com/auth/calendar.events";
    private static final String CALENDAR_FREE_BUSY_SCOPE = "https://www.googleapis.com/auth/calendar.freebusy";

    private final MeetingGroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;
    private final InviteCodeGenerator inviteCodeGenerator;
    private final UserRepository userRepository;
    private final UserPreferenceRepository preferenceRepository;
    private final MeetingRepository meetingRepository;
    private final OAuth2AuthorizedClientService authorizedClientService;

    public GroupService(
            MeetingGroupRepository groupRepository,
            GroupMemberRepository memberRepository,
            InviteCodeGenerator inviteCodeGenerator,
            UserRepository userRepository,
            UserPreferenceRepository preferenceRepository,
            MeetingRepository meetingRepository,
            OAuth2AuthorizedClientService authorizedClientService
    ) {
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
        this.inviteCodeGenerator = inviteCodeGenerator;
        this.userRepository = userRepository;
        this.preferenceRepository = preferenceRepository;
        this.meetingRepository = meetingRepository;
        this.authorizedClientService = authorizedClientService;
    }

    @Transactional
    public GroupDetailResponse create(long userId, CreateGroupRequest request) {
        if (!userRepository.existsById(userId)) {
            throw new BusinessException("USER_NOT_FOUND", "사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
        }
        MeetingGroup group = groupRepository.save(new MeetingGroup(request.name().trim(), issueInviteCode()));
        GroupMember host = memberRepository.save(GroupMember.host(group, userId));
        return toDetailResponse(group, List.of(host));
    }

    @Transactional
    public JoinGroupResponse join(long userId, String inviteCode) {
        if (!userRepository.existsById(userId)) {
            throw new BusinessException("USER_NOT_FOUND", "사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
        }
        MeetingGroup group = groupRepository.findByInviteCode(inviteCode.trim().toUpperCase())
                .orElseThrow(() -> new BusinessException("INVITE_CODE_NOT_FOUND", "존재하지 않는 초대 코드입니다.", HttpStatus.NOT_FOUND));
        var existingMember = memberRepository.findByGroupIdAndUserId(group.getId(), userId);
        if (existingMember.isPresent()) {
            GroupMember member = existingMember.get();
            return new JoinGroupResponse(group.getId(), group.getName(), member.getId(), member.getRole(), true);
        }
        GroupMember member = memberRepository.save(GroupMember.member(group, userId));
        return new JoinGroupResponse(group.getId(), group.getName(), member.getId(), member.getRole(), false);
    }

    public List<GroupSummaryResponse> findMyGroups(long userId) {
        return groupRepository.findAllJoinedByUserId(userId).stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    public GroupDetailResponse findDetail(long userId, long groupId) {
        MeetingGroup group = requireJoinedMember(userId, groupId).getGroup();
        List<GroupMember> members = memberRepository.findAllByGroupIdOrderByJoinedAtAsc(groupId);
        return toDetailResponse(group, members);
    }

    @Transactional
    public void delete(long userId, long groupId) {
        GroupMember member = requireJoinedMember(userId, groupId);
        if (member.getRole() != com.damoyeo.group.domain.GroupMemberRole.HOST) {
            throw new BusinessException("GROUP_DELETE_FORBIDDEN", "그룹 대표만 모임을 삭제할 수 있습니다.", HttpStatus.FORBIDDEN);
        }
        groupRepository.delete(member.getGroup());
    }

    public GroupMember requireJoinedMember(long userId, long groupId) {
        GroupMember member = memberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new BusinessException("GROUP_NOT_FOUND", "그룹을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        return member;
    }

    private String issueInviteCode() {
        for (int attempt = 0; attempt < MAX_INVITE_CODE_ATTEMPTS; attempt++) {
            String code = inviteCodeGenerator.generate();
            if (!groupRepository.existsByInviteCode(code)) {
                return code;
            }
        }
        throw new BusinessException("INVITE_CODE_ISSUE_FAILED", "초대 코드를 생성하지 못했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private GroupDetailResponse toDetailResponse(MeetingGroup group, List<GroupMember> members) {
        List<Long> userIds = members.stream().map(GroupMember::getUserId).toList();
        Map<Long, User> users = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        Map<Long, Long> preferenceCounts = preferenceRepository.countAllByUserIds(userIds).stream()
                .collect(Collectors.toMap(value -> value.userId(), value -> value.count()));
        Map<Long, Boolean> calendarConnections = users.values().stream().collect(Collectors.toMap(
                User::getId,
                user -> {
                    var client = authorizedClientService.loadAuthorizedClient("google", user.getGoogleSubject());
                    return client != null
                            && client.getAccessToken().getScopes().contains(CALENDAR_EVENTS_SCOPE)
                            && client.getAccessToken().getScopes().contains(CALENDAR_FREE_BUSY_SCOPE);
                }
        ));
        List<Meeting> meetings = meetingRepository.findAllByGroupIdOrderByCreatedAtDesc(group.getId());
        long pastMeetingCount = meetings.stream()
                .filter(meeting -> meeting.getStatus() == MeetingStatus.CONFIRMED)
                .filter(meeting -> meeting.getConfirmedStartAt() != null && meeting.getConfirmedStartAt().isBefore(java.time.Instant.now()))
                .count();
        List<GroupDetailResponse.ActiveMeetingResponse> activeMeetings = meetings.stream()
                .filter(this::isActive)
                .map(meeting -> new GroupDetailResponse.ActiveMeetingResponse(
                        meeting.getId(), meeting.getStatus().name(), meeting.getPurpose(), meeting.getRegion(),
                        meeting.getScheduleSearchFrom(), meeting.getScheduleSearchTo(), meeting.getCreatedBy(), meeting.getCreatedAt()
                ))
                .toList();
        GroupDetailResponse.ActiveMeetingResponse activeMeeting = activeMeetings.stream().findFirst().orElse(null);
        return GroupDetailResponse.of(group, members, users, preferenceCounts, calendarConnections, pastMeetingCount, activeMeetings, activeMeeting);
    }

    private GroupSummaryResponse toSummaryResponse(MeetingGroup group) {
        List<GroupMember> members = memberRepository.findAllByGroupIdOrderByJoinedAtAsc(group.getId());
        Map<Long, User> users = userRepository.findAllById(members.stream().map(GroupMember::getUserId).toList()).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        List<GroupSummaryResponse.MemberResponse> responses = members.stream()
                .map(member -> new GroupSummaryResponse.MemberResponse(
                        member.getUserId(),
                        users.get(member.getUserId()) == null ? null : users.get(member.getUserId()).getNickname(),
                        users.get(member.getUserId()) == null ? null : users.get(member.getUserId()).getProfileImageUrl(),
                        member.getRole()
                ))
                .toList();
        List<Meeting> meetings = meetingRepository.findAllByGroupIdOrderByCreatedAtDesc(group.getId());
        GroupSummaryResponse.LastMeetingResponse lastMeeting = meetings.stream()
                .filter(meeting -> meeting.getStatus() == MeetingStatus.CONFIRMED && meeting.getConfirmedStartAt() != null)
                .filter(meeting -> meeting.getConfirmedStartAt().isBefore(java.time.Instant.now()))
                .max(java.util.Comparator.comparing(Meeting::getConfirmedStartAt))
                .map(meeting -> new GroupSummaryResponse.LastMeetingResponse(meeting.getConfirmedStartAt(), meeting.getRegion()))
                .orElse(null);
        List<GroupSummaryResponse.ActiveMeetingResponse> activeMeetings = meetings.stream()
                .filter(this::isActive)
                .map(meeting -> new GroupSummaryResponse.ActiveMeetingResponse(
                        meeting.getId(), meeting.getStatus().name(), meeting.getPurpose(), meeting.getRegion(),
                        meeting.getScheduleSearchFrom(), meeting.getScheduleSearchTo(), meeting.getCreatedBy(), meeting.getCreatedAt()
                ))
                .toList();
        GroupSummaryResponse.ActiveMeetingResponse activeMeeting = activeMeetings.stream().findFirst().orElse(null);
        return new GroupSummaryResponse(group.getId(), group.getName(), members.size(), responses, lastMeeting, activeMeetings, activeMeeting, group.getCreatedAt());
    }

    private boolean isActive(Meeting meeting) {
        return meeting.getStatus() == MeetingStatus.SURVEYING
                || meeting.getStatus() == MeetingStatus.READY_TO_PLAN
                || meeting.getStatus() == MeetingStatus.PLANNING
                || meeting.getStatus() == MeetingStatus.PROPOSING;
    }
}
