package com.damoyeo.meeting.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.damoyeo.group.dto.CreateGroupRequest;
import com.damoyeo.group.dto.GroupDetailResponse;
import com.damoyeo.group.service.GroupService;
import com.damoyeo.meeting.domain.MeetingStatus;
import com.damoyeo.meeting.domain.PreferredTimeOfDay;
import com.damoyeo.meeting.domain.ChatRole;
import com.damoyeo.meeting.domain.MeetingChatMessage;
import com.damoyeo.meeting.domain.MeetingMemory;
import com.damoyeo.meeting.dto.MeetingResponse;
import com.damoyeo.meeting.dto.UpdateMeetingRequest;
import com.damoyeo.meeting.repository.MeetingChatMessageRepository;
import com.damoyeo.meeting.repository.MeetingMemoryRepository;
import com.damoyeo.meeting.repository.MeetingRepository;
import com.damoyeo.user.domain.User;
import com.damoyeo.user.repository.UserRepository;
import java.time.LocalDate;
import java.util.Set;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class GroupMeetingFlowIntegrationTest {

    @Autowired
    private GroupService groupService;

    @Autowired
    private MeetingService meetingService;

    @Autowired
    private MeetingAvailabilityService availabilityService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private MeetingChatMessageRepository chatMessageRepository;

    @Autowired
    private MeetingMemoryRepository memoryRepository;

    @Test
    void createsAndSubmitsAMeetingDraft() {
        long userId = userRepository.save(new User("google-subject-1", "user@example.com", "동원")).getId();
        GroupDetailResponse group = groupService.create(userId, new CreateGroupRequest("대학교 동기"));
        MeetingResponse draft = meetingService.createDraft(userId, group.id());

        meetingService.update(
                userId,
                draft.id(),
                new UpdateMeetingRequest(
                        "오랜만에 저녁 식사",
                        "건대",
                        LocalDate.of(2026, 8, 23),
                        LocalDate.of(2026, 9, 7),
                        PreferredTimeOfDay.EVENING,
                        null
                )
        );
        meetingService.updateParticipants(
                userId,
                draft.id(),
                Set.of(group.members().getFirst().memberId())
        );

        MeetingResponse submitted = meetingService.submit(userId, draft.id());
        var availability = availabilityService.submit(
                userId,
                draft.id(),
                Set.of(LocalDate.of(2026, 8, 23))
        );
        MeetingResponse planning = meetingService.startPlanning(userId, draft.id());

        assertThat(submitted.status()).isEqualTo(MeetingStatus.COLLECTING_AVAILABILITY);
        assertThat(availability.meetingStatus()).isEqualTo(MeetingStatus.READY_TO_PLAN);
        assertThat(planning.status()).isEqualTo(MeetingStatus.PLANNING);
        assertThat(planning.participantMemberIds()).containsExactly(group.members().getFirst().memberId());
    }

    @Test
    void storesRawChatMessagesAndCompressedMemorySeparately() {
        long userId = userRepository.save(new User("google-subject-2", "memory@example.com", "메모리")).getId();
        GroupDetailResponse group = groupService.create(userId, new CreateGroupRequest("정기 모임"));
        MeetingResponse draft = meetingService.createDraft(userId, group.id());
        var meeting = meetingRepository.findById(draft.id()).orElseThrow();

        chatMessageRepository.save(new MeetingChatMessage(meeting, ChatRole.USER, "조용한 식당으로 찾아줘"));
        chatMessageRepository.save(new MeetingChatMessage(meeting, ChatRole.ASSISTANT, "선호를 반영해볼게요"));
        memoryRepository.save(new MeetingMemory(meeting, Map.of(
                "summary", "조용한 식사 장소를 선호함",
                "avoidPatterns", java.util.List.of("시끄러운 술집")
        )));
        chatMessageRepository.flush();
        memoryRepository.flush();

        assertThat(chatMessageRepository.findAllByMeetingIdOrderByIdAsc(draft.id()))
                .extracting(MeetingChatMessage::getRole)
                .containsExactly(ChatRole.USER, ChatRole.ASSISTANT);
        assertThat(memoryRepository.findById(draft.id()).orElseThrow().getMemory())
                .containsEntry("summary", "조용한 식사 장소를 선호함");
        assertThat(memoryRepository.findAllByGroupIdOrderByUpdatedAtDesc(group.id())).hasSize(1);
    }
}
