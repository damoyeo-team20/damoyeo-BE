package com.damoyeo.meeting.dto;

import com.damoyeo.meeting.domain.Meeting;
import com.damoyeo.meeting.domain.MeetingParticipant;
import com.damoyeo.meeting.domain.MeetingStatus;
import com.damoyeo.meeting.domain.PreferredTimeOfDay;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record MeetingResponse(
        Long id,
        Long groupId,
        Long createdBy,
        String purpose,
        String region,
        LocalDate scheduleSearchFrom,
        LocalDate scheduleSearchTo,
        PreferredTimeOfDay preferredTimeOfDay,
        LocalDate preferenceSurveyDeadline,
        MeetingStatus status,
        List<Long> participantMemberIds,
        Instant createdAt,
        Instant updatedAt
) {
    public static MeetingResponse of(Meeting meeting, List<MeetingParticipant> participants) {
        return new MeetingResponse(
                meeting.getId(),
                meeting.getGroup().getId(),
                meeting.getCreatedBy(),
                meeting.getPurpose(),
                meeting.getRegion(),
                meeting.getScheduleSearchFrom(),
                meeting.getScheduleSearchTo(),
                meeting.getPreferredTimeOfDay(),
                meeting.getPreferenceSurveyDeadline(),
                meeting.getStatus(),
                participants.stream().map(participant -> participant.getGroupMember().getId()).toList(),
                meeting.getCreatedAt(),
                meeting.getUpdatedAt()
        );
    }
}
