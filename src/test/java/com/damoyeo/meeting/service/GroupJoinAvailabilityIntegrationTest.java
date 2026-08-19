package com.damoyeo.meeting.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.damoyeo.common.exception.BusinessException;
import com.damoyeo.group.dto.CreateGroupRequest;
import com.damoyeo.group.service.GroupService;
import com.damoyeo.meeting.domain.MeetingStatus;
import com.damoyeo.meeting.domain.PreferredTimeOfDay;
import com.damoyeo.meeting.dto.UpdateMeetingRequest;
import com.damoyeo.user.domain.User;
import com.damoyeo.user.repository.UserRepository;
import com.damoyeo.user.service.UserService;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class GroupJoinAvailabilityIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private GroupService groupService;

    @Autowired
    private MeetingService meetingService;

    @Autowired
    private MeetingAvailabilityService availabilityService;

    @Test
    void joinsByInviteCodeAndCollectsEveryParticipantsAvailableDates() {
        User host = userRepository.save(new User("join-host", "join-host@example.com", "호스트"));
        User member = userRepository.save(new User("join-member", "join-member@example.com", "멤버"));
        var group = groupService.create(host.getId(), new CreateGroupRequest("정기 모임"));
        groupService.join(member.getId(), group.inviteCode().toLowerCase());
        var joinedGroup = groupService.findDetail(host.getId(), group.id());
        var meeting = meetingService.createDraft(host.getId(), group.id());
        LocalDate from = LocalDate.now(ZoneId.of("Asia/Seoul")).plusDays(1);
        LocalDate to = from.plusDays(7);

        meetingService.update(host.getId(), meeting.id(), new UpdateMeetingRequest(
                "주말 저녁 모임",
                "건대",
                from,
                to,
                PreferredTimeOfDay.EVENING
        ));
        meetingService.updateParticipants(
                host.getId(),
                meeting.id(),
                joinedGroup.members().stream().map(memberResponse -> memberResponse.memberId()).collect(java.util.stream.Collectors.toSet())
        );
        meetingService.submit(host.getId(), meeting.id());

        var hostSubmission = availabilityService.submit(host.getId(), meeting.id(), Set.of(from));
        var waiting = availabilityService.findCoordinationStatus(host.getId(), meeting.id());
        var memberSubmission = availabilityService.submit(member.getId(), meeting.id(), Set.of(from, from.plusDays(1)));
        var completed = availabilityService.findCoordinationStatus(host.getId(), meeting.id());

        assertThat(joinedGroup.members()).hasSize(2);
        assertThat(hostSubmission.meetingStatus()).isEqualTo(MeetingStatus.SURVEYING);
        assertThat(waiting.allSubmitted()).isFalse();
        assertThat(memberSubmission.meetingStatus()).isEqualTo(MeetingStatus.READY_TO_PLAN);
        assertThat(completed.allSubmitted()).isTrue();
    }

    @Test
    void rejectsDuplicateGroupJoinAndCompletesOnboarding() {
        User user = userRepository.save(new User("duplicate-user", "duplicate@example.com", "사용자"));
        var group = groupService.create(user.getId(), new CreateGroupRequest("내 그룹"));

        assertThat(groupService.join(user.getId(), group.inviteCode()).alreadyMember()).isTrue();

        assertThat(userService.findMe(user.getId()).onboardingCompleted()).isFalse();
        assertThat(userService.completeOnboarding(user.getId()).onboardingCompleted()).isTrue();
    }
}
