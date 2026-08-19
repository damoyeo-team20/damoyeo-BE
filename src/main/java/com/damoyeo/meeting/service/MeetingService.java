package com.damoyeo.meeting.service;

import com.damoyeo.common.exception.BusinessException;
import com.damoyeo.group.domain.GroupMember;
import com.damoyeo.group.domain.GroupMemberStatus;
import com.damoyeo.group.repository.GroupMemberRepository;
import com.damoyeo.group.service.GroupService;
import com.damoyeo.meeting.domain.Meeting;
import com.damoyeo.meeting.domain.MeetingParticipant;
import com.damoyeo.meeting.dto.MeetingResponse;
import com.damoyeo.meeting.dto.UpdateMeetingRequest;
import com.damoyeo.meeting.repository.MeetingParticipantRepository;
import com.damoyeo.meeting.repository.MeetingRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MeetingService {

    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository participantRepository;
    private final GroupMemberRepository memberRepository;
    private final GroupService groupService;

    public MeetingService(
            MeetingRepository meetingRepository,
            MeetingParticipantRepository participantRepository,
            GroupMemberRepository memberRepository,
            GroupService groupService
    ) {
        this.meetingRepository = meetingRepository;
        this.participantRepository = participantRepository;
        this.memberRepository = memberRepository;
        this.groupService = groupService;
    }

    @Transactional
    public MeetingResponse createDraft(long userId, long groupId) {
        GroupMember creator = groupService.requireJoinedMember(userId, groupId);
        Meeting meeting = meetingRepository.save(Meeting.draft(creator.getGroup(), userId));
        return MeetingResponse.of(meeting, List.of());
    }

    @Transactional
    public MeetingResponse update(long userId, long meetingId, UpdateMeetingRequest request) {
        Meeting meeting = requireEditableMeeting(userId, meetingId);
        meeting.updateConditions(
                request.purpose(),
                request.region(),
                request.scheduleSearchFrom(),
                request.scheduleSearchTo(),
                request.preferredTimeOfDay(),
                request.preferenceSurveyDeadline()
        );
        return toResponse(meeting);
    }

    @Transactional
    public MeetingResponse updateParticipants(long userId, long meetingId, Set<Long> requestedMemberIds) {
        Meeting meeting = requireEditableMeeting(userId, meetingId);
        meeting.ensureEditable();
        Set<Long> memberIds = new LinkedHashSet<>(requestedMemberIds);
        List<GroupMember> members = memberRepository.findAllById(memberIds);
        boolean allJoinedGroupMembers = members.size() == memberIds.size()
                && members.stream().allMatch(member ->
                        member.getGroup().getId().equals(meeting.getGroup().getId())
                                && member.getStatus() == GroupMemberStatus.JOINED
                );
        if (!allJoinedGroupMembers) {
            throw new BusinessException(
                    "INVALID_PARTICIPANT",
                    "해당 그룹에 참여 중인 멤버만 선택할 수 있습니다.",
                    HttpStatus.BAD_REQUEST
            );
        }
        participantRepository.deleteAllByMeetingId(meetingId);
        participantRepository.flush();
        List<MeetingParticipant> participants = members.stream()
                .map(member -> new MeetingParticipant(meeting, member))
                .toList();
        participantRepository.saveAll(participants);
        return MeetingResponse.of(meeting, participants);
    }

    public MeetingResponse find(long userId, long meetingId) {
        return toResponse(requireAccessibleMeeting(userId, meetingId));
    }

    @Transactional
    public MeetingResponse submit(long userId, long meetingId) {
        Meeting meeting = requireEditableMeeting(userId, meetingId);
        meeting.submit(LocalDate.now(SERVICE_ZONE), participantRepository.existsByMeetingId(meetingId));
        return toResponse(meeting);
    }

    @Transactional
    public MeetingResponse startPlanning(long userId, long meetingId) {
        Meeting meeting = requireEditableMeeting(userId, meetingId);
        meeting.startPlanning(LocalDate.now(SERVICE_ZONE));
        return toResponse(meeting);
    }

    private Meeting requireAccessibleMeeting(long userId, long meetingId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new BusinessException("MEETING_NOT_FOUND", "일정을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        groupService.requireJoinedMember(userId, meeting.getGroup().getId());
        return meeting;
    }

    private Meeting requireEditableMeeting(long userId, long meetingId) {
        Meeting meeting = requireAccessibleMeeting(userId, meetingId);
        if (!meeting.getCreatedBy().equals(userId)) {
            throw new BusinessException("MEETING_EDIT_FORBIDDEN", "일정을 만든 사용자만 수정할 수 있습니다.", HttpStatus.FORBIDDEN);
        }
        return meeting;
    }

    private MeetingResponse toResponse(Meeting meeting) {
        return MeetingResponse.of(meeting, participantRepository.findAllByMeetingIdOrderByIdAsc(meeting.getId()));
    }
}
