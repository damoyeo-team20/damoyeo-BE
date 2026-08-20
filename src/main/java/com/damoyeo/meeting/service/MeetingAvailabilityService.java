package com.damoyeo.meeting.service;

import com.damoyeo.calendar.GoogleCalendarService;
import com.damoyeo.common.exception.BusinessException;
import com.damoyeo.group.domain.GroupMember;
import com.damoyeo.group.service.GroupService;
import com.damoyeo.meeting.domain.Meeting;
import com.damoyeo.meeting.domain.MeetingAvailableDate;
import com.damoyeo.meeting.domain.MeetingParticipant;
import com.damoyeo.meeting.dto.AvailabilityResponse;
import com.damoyeo.meeting.dto.CoordinationStatusResponse;
import com.damoyeo.meeting.dto.CalendarBusyDatesResponse;
import com.damoyeo.meeting.repository.MeetingAvailableDateRepository;
import com.damoyeo.meeting.repository.MeetingParticipantRepository;
import com.damoyeo.meeting.repository.MeetingRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MeetingAvailabilityService {

    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository participantRepository;
    private final MeetingAvailableDateRepository availableDateRepository;
    private final GroupService groupService;
    private final GoogleCalendarService googleCalendarService;

    public MeetingAvailabilityService(
            MeetingRepository meetingRepository,
            MeetingParticipantRepository participantRepository,
            MeetingAvailableDateRepository availableDateRepository,
            GroupService groupService,
            GoogleCalendarService googleCalendarService
    ) {
        this.meetingRepository = meetingRepository;
        this.participantRepository = participantRepository;
        this.availableDateRepository = availableDateRepository;
        this.groupService = groupService;
        this.googleCalendarService = googleCalendarService;
    }

    @Transactional
    public AvailabilityResponse submit(long userId, long meetingId, Set<LocalDate> availableDates) {
        Meeting meeting = requireMeeting(meetingId);
        meeting.ensureCollectingAvailability();
        validateRange(meeting, availableDates);
        GroupMember member = groupService.requireJoinedMember(userId, meeting.getGroup().getId());
        MeetingParticipant participant = participantRepository
                .findByMeetingIdAndGroupMemberId(meetingId, member.getId())
                .orElseThrow(() -> new BusinessException(
                        "NOT_MEETING_PARTICIPANT",
                        "이번 일정의 참여자가 아닙니다.",
                        HttpStatus.FORBIDDEN
                ));

        availableDateRepository.deleteAllByMeetingParticipantId(participant.getId());
        availableDateRepository.flush();
        List<MeetingAvailableDate> dates = availableDates.stream()
                .sorted()
                .map(date -> new MeetingAvailableDate(participant, date))
                .toList();
        availableDateRepository.saveAll(dates);
        participant.markAvailabilitySubmitted();
        participantRepository.flush();

        List<MeetingParticipant> participants = participantRepository.findAllByMeetingIdOrderByIdAsc(meetingId);
        if (meeting.getStatus() == com.damoyeo.meeting.domain.MeetingStatus.SURVEYING
                && participants.stream().allMatch(value -> value.getAvailabilitySubmittedAt() != null)) {
            meeting.completeAvailabilityCollection();
        }

        return new AvailabilityResponse(
                participant.getGroupMember().getId(),
                participant.getAvailabilitySubmittedAt(),
                dates.stream().map(MeetingAvailableDate::getAvailableDate).toList(),
                meeting.getStatus()
        );
    }

    public AvailabilityResponse findMine(long userId, long meetingId) {
        Meeting meeting = requireMeeting(meetingId);
        GroupMember member = groupService.requireJoinedMember(userId, meeting.getGroup().getId());
        MeetingParticipant participant = participantRepository
                .findByMeetingIdAndGroupMemberId(meetingId, member.getId())
                .orElseThrow(() -> new BusinessException(
                        "NOT_MEETING_PARTICIPANT",
                        "이번 일정의 참여자가 아닙니다.",
                        HttpStatus.FORBIDDEN
                ));
        List<LocalDate> dates = availableDateRepository
                .findAllByMeetingParticipantIdOrderByAvailableDateAsc(participant.getId()).stream()
                .map(MeetingAvailableDate::getAvailableDate)
                .toList();
        return new AvailabilityResponse(
                participant.getGroupMember().getId(),
                participant.getAvailabilitySubmittedAt(),
                dates,
                meeting.getStatus()
        );
    }

    public CalendarBusyDatesResponse findMyCalendarBusyDates(long userId, long meetingId) {
        Meeting meeting = requireMeeting(meetingId);
        if (meeting.getScheduleSearchFrom() == null || meeting.getScheduleSearchTo() == null) {
            throw new BusinessException("SEARCH_PERIOD_REQUIRED", "일정 탐색 범위가 필요합니다.", HttpStatus.CONFLICT);
        }
        GroupMember member = groupService.requireJoinedMember(userId, meeting.getGroup().getId());
        participantRepository.findByMeetingIdAndGroupMemberId(meetingId, member.getId())
                .orElseThrow(() -> new BusinessException(
                        "NOT_MEETING_PARTICIPANT",
                        "이번 일정의 참여자가 아닙니다.",
                        HttpStatus.FORBIDDEN
                ));
        var result = googleCalendarService.findBusyDates(
                userId, meeting.getScheduleSearchFrom(), meeting.getScheduleSearchTo()
        );
        return new CalendarBusyDatesResponse(result.calendarConnected(), result.busyDates());
    }

    public CoordinationStatusResponse findCoordinationStatus(long userId, long meetingId) {
        Meeting meeting = requireMeeting(meetingId);
        groupService.requireJoinedMember(userId, meeting.getGroup().getId());
        return CoordinationStatusResponse.of(
                meetingId,
                meeting.getStatus(),
                participantRepository.findAllByMeetingIdOrderByIdAsc(meetingId)
        );
    }

    private Meeting requireMeeting(long meetingId) {
        return meetingRepository.findById(meetingId)
                .orElseThrow(() -> new BusinessException("MEETING_NOT_FOUND", "일정을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
    }

    private void validateRange(Meeting meeting, Set<LocalDate> availableDates) {
        if (availableDates.isEmpty()
                || (meeting.getScheduleSearchFrom() != null && availableDates.stream().anyMatch(date ->
                        date.isBefore(meeting.getScheduleSearchFrom())
                                || date.isAfter(meeting.getScheduleSearchTo())
                ))) {
            throw new BusinessException(
                    "INVALID_AVAILABLE_DATE",
                    "가능 날짜는 일정 탐색 범위 안에서 한 개 이상 선택해야 합니다.",
                    HttpStatus.BAD_REQUEST
            );
        }
    }
}
