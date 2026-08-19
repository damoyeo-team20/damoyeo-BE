package com.damoyeo.meeting.dto;

import com.damoyeo.meeting.domain.MeetingParticipant;
import com.damoyeo.meeting.domain.MeetingStatus;
import java.time.Instant;
import java.util.List;

public record CoordinationStatusResponse(
        Long meetingId,
        MeetingStatus status,
        boolean allSubmitted,
        List<ParticipantStatus> participants
) {
    public static CoordinationStatusResponse of(
            Long meetingId,
            MeetingStatus status,
            List<MeetingParticipant> participants
    ) {
        List<ParticipantStatus> statuses = participants.stream()
                .map(participant -> new ParticipantStatus(
                        participant.getId(),
                        participant.getGroupMember().getUserId(),
                        participant.getAvailabilitySubmittedAt() != null,
                        participant.getAvailabilitySubmittedAt()
                ))
                .toList();
        return new CoordinationStatusResponse(
                meetingId,
                status,
                statuses.stream().allMatch(ParticipantStatus::submitted),
                statuses
        );
    }

    public record ParticipantStatus(
            Long meetingParticipantId,
            Long userId,
            boolean submitted,
            Instant submittedAt
    ) {
    }
}
