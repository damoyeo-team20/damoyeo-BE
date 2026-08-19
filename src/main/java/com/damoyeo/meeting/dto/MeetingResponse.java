package com.damoyeo.meeting.dto;

import com.damoyeo.meeting.domain.Meeting;
import com.damoyeo.meeting.domain.MeetingParticipant;
import com.damoyeo.meeting.domain.MeetingStatus;
import com.damoyeo.meeting.domain.PreferredTimeOfDay;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import com.damoyeo.user.domain.User;

public record MeetingResponse(
        Long id,
        Long groupId,
        Long createdBy,
        String purpose,
        String region,
        LocalDate scheduleSearchFrom,
        LocalDate scheduleSearchTo,
        PreferredTimeOfDay preferredTimeOfDay,
        MeetingStatus status,
        List<Long> participantMemberIds,
        List<ParticipantResponse> participants,
        Instant createdAt,
        Instant updatedAt
) {
    public static MeetingResponse of(
            Meeting meeting,
            List<MeetingParticipant> participants,
            Map<Long, User> users,
            Map<Long, List<LocalDate>> selectedDates
    ) {
        return new MeetingResponse(
                meeting.getId(),
                meeting.getGroup().getId(),
                meeting.getCreatedBy(),
                meeting.getPurpose(),
                meeting.getRegion(),
                meeting.getScheduleSearchFrom(),
                meeting.getScheduleSearchTo(),
                meeting.getPreferredTimeOfDay(),
                meeting.getStatus(),
                participants.stream().map(participant -> participant.getGroupMember().getId()).toList(),
                participants.stream().map(participant -> {
                    User user = users.get(participant.getGroupMember().getUserId());
                    return new ParticipantResponse(
                            participant.getGroupMember().getId(),
                            participant.getGroupMember().getUserId(),
                            user == null ? null : user.getNickname(),
                            participant.getAvailabilitySubmittedAt(),
                            selectedDates.getOrDefault(participant.getId(), List.of())
                    );
                }).toList(),
                meeting.getCreatedAt(),
                meeting.getUpdatedAt()
        );
    }

    public record ParticipantResponse(
            Long groupMemberId,
            Long userId,
            String nickname,
            Instant confirmedAt,
            List<LocalDate> selectedDates
    ) {
    }
}
