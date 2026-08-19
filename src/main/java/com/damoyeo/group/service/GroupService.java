package com.damoyeo.group.service;

import com.damoyeo.common.exception.BusinessException;
import com.damoyeo.group.domain.GroupMember;
import com.damoyeo.group.domain.GroupMemberStatus;
import com.damoyeo.group.domain.MeetingGroup;
import com.damoyeo.group.dto.CreateGroupRequest;
import com.damoyeo.group.dto.GroupDetailResponse;
import com.damoyeo.group.dto.GroupSummaryResponse;
import com.damoyeo.group.repository.GroupMemberRepository;
import com.damoyeo.group.repository.MeetingGroupRepository;
import com.damoyeo.user.repository.UserRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GroupService {

    private static final int MAX_INVITE_CODE_ATTEMPTS = 10;

    private final MeetingGroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;
    private final InviteCodeGenerator inviteCodeGenerator;
    private final UserRepository userRepository;

    public GroupService(
            MeetingGroupRepository groupRepository,
            GroupMemberRepository memberRepository,
            InviteCodeGenerator inviteCodeGenerator,
            UserRepository userRepository
    ) {
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
        this.inviteCodeGenerator = inviteCodeGenerator;
        this.userRepository = userRepository;
    }

    @Transactional
    public GroupDetailResponse create(long userId, CreateGroupRequest request) {
        if (!userRepository.existsById(userId)) {
            throw new BusinessException("USER_NOT_FOUND", "사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
        }
        MeetingGroup group = groupRepository.save(new MeetingGroup(request.name().trim(), issueInviteCode()));
        GroupMember host = memberRepository.save(GroupMember.host(group, userId));
        return GroupDetailResponse.of(group, List.of(host));
    }

    public List<GroupSummaryResponse> findMyGroups(long userId) {
        return groupRepository.findAllJoinedByUserId(userId).stream()
                .map(GroupSummaryResponse::from)
                .toList();
    }

    public GroupDetailResponse findDetail(long userId, long groupId) {
        MeetingGroup group = requireJoinedMember(userId, groupId).getGroup();
        List<GroupMember> members = memberRepository.findAllByGroupIdAndStatusOrderByJoinedAtAsc(
                groupId,
                GroupMemberStatus.JOINED
        );
        return GroupDetailResponse.of(group, members);
    }

    public GroupMember requireJoinedMember(long userId, long groupId) {
        GroupMember member = memberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new BusinessException("GROUP_NOT_FOUND", "그룹을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        if (member.getStatus() != GroupMemberStatus.JOINED) {
            throw new BusinessException("GROUP_NOT_FOUND", "그룹을 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
        }
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
}
