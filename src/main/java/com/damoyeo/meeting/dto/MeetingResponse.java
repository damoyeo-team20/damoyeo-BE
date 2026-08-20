package com.damoyeo.meeting.dto;

import com.damoyeo.meeting.domain.Meeting;
import com.damoyeo.meeting.domain.MeetingParticipant;
import com.damoyeo.meeting.domain.MeetingStatus;
import com.damoyeo.meeting.domain.PreferredTimeOfDay;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Set;
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
        Instant resolvedStartAt,
        Instant resolvedEndAt,
        String scheduleResolutionReason,
        List<CandidateDateResponse> candidateDates,
        ConfirmedSuggestionResponse confirmedSuggestion,
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
        Set<LocalDate> commonDates = null;
        for (MeetingParticipant participant : participants) {
            Set<LocalDate> participantDates = new LinkedHashSet<>(
                    selectedDates.getOrDefault(participant.getId(), List.of())
            );
            if (commonDates == null) {
                commonDates = participantDates;
            } else {
                commonDates.retainAll(participantDates);
            }
        }
        LocalDate resolvedDate = meeting.getResolvedStartAt() == null
                ? null
                : meeting.getResolvedStartAt().atZone(java.time.ZoneId.of("Asia/Seoul")).toLocalDate();
        List<CandidateDateResponse> candidateDates = commonDates == null ? List.of() : commonDates.stream().sorted()
                .map(date -> new CandidateDateResponse(date, date.equals(resolvedDate)))
                .toList();
        return new MeetingResponse(
                meeting.getId(),
                meeting.getGroup().getId(),
                meeting.getCreatedBy(),
                meeting.getPurpose(),
                meeting.getRegion(),
                meeting.getScheduleSearchFrom(),
                meeting.getScheduleSearchTo(),
                meeting.getPreferredTimeOfDay(),
                meeting.getResolvedStartAt(),
                meeting.getResolvedEndAt(),
                meeting.getScheduleResolutionReason(),
                candidateDates,
                meeting.getConfirmedSuggestion() == null ? null : new ConfirmedSuggestionResponse(
                        meeting.getConfirmedSuggestion().getId(),
                        meeting.getConfirmedSuggestion().getName(),
                        meeting.getConfirmedSuggestion().getCategory(),
                        meeting.getConfirmedSuggestion().getAddress(),
                        meeting.getConfirmedSuggestion().getProposedStartAt(),
                        meeting.getConfirmedSuggestion().getProposedEndAt(),
                        meeting.getConfirmedSuggestion().getReasons() == null
                                ? List.of()
                                : meeting.getConfirmedSuggestion().getReasons()
                ),
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

    public record ConfirmedSuggestionResponse(
            Long id,
            String name,
            String category,
            String address,
            Instant proposedStartAt,
            Instant proposedEndAt,
            List<String> reasons
    ) {
    }

    public record CandidateDateResponse(LocalDate date, boolean selected) {
    }
}
