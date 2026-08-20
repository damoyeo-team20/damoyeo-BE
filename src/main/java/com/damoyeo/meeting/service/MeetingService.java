package com.damoyeo.meeting.service;

import com.damoyeo.ai.AiClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.damoyeo.common.exception.BusinessException;
import com.damoyeo.group.domain.GroupMember;
import com.damoyeo.group.domain.GroupMemory;
import com.damoyeo.group.repository.GroupMemberRepository;
import com.damoyeo.group.repository.GroupMemoryRepository;
import com.damoyeo.group.service.GroupService;
import com.damoyeo.meeting.domain.Meeting;
import com.damoyeo.meeting.domain.MeetingParticipant;
import com.damoyeo.meeting.domain.MeetingChatMessage;
import com.damoyeo.meeting.domain.ChatRole;
import com.damoyeo.meeting.domain.MeetingMemory;
import com.damoyeo.meeting.domain.MeetingSuggestion;
import com.damoyeo.meeting.dto.ContextChatResponse;
import com.damoyeo.meeting.dto.RevisionChatResponse;
import com.damoyeo.meeting.dto.MeetingChatResponse;
import com.damoyeo.meeting.dto.MeetingResponse;
import com.damoyeo.meeting.dto.MeetingListItemResponse;
import com.damoyeo.meeting.dto.UpdateMeetingRequest;
import com.damoyeo.meeting.repository.MeetingParticipantRepository;
import com.damoyeo.meeting.repository.MeetingRepository;
import com.damoyeo.meeting.repository.MeetingAvailableDateRepository;
import com.damoyeo.meeting.repository.MeetingChatMessageRepository;
import com.damoyeo.meeting.repository.MeetingMemoryRepository;
import com.damoyeo.meeting.repository.MeetingSuggestionRepository;
import com.damoyeo.preference.repository.UserPreferenceRepository;
import com.damoyeo.user.repository.UserRepository;
import com.damoyeo.user.domain.User;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.LinkedHashMap;
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
    private final MeetingAvailableDateRepository availableDateRepository;
    private final UserRepository userRepository;
    private final MeetingChatMessageRepository chatMessageRepository;
    private final AiClient aiClient;
    private final UserPreferenceRepository preferenceRepository;
    private final MeetingMemoryRepository memoryRepository;
    private final ObjectMapper objectMapper;
    private final MeetingSuggestionRepository suggestionRepository;
    private final GroupMemoryRepository groupMemoryRepository;

    public MeetingService(
            MeetingRepository meetingRepository,
            MeetingParticipantRepository participantRepository,
            GroupMemberRepository memberRepository,
            GroupService groupService,
            MeetingAvailableDateRepository availableDateRepository,
            UserRepository userRepository,
            MeetingChatMessageRepository chatMessageRepository,
            AiClient aiClient,
            UserPreferenceRepository preferenceRepository,
            MeetingMemoryRepository memoryRepository,
            ObjectMapper objectMapper,
            MeetingSuggestionRepository suggestionRepository,
            GroupMemoryRepository groupMemoryRepository
    ) {
        this.meetingRepository = meetingRepository;
        this.participantRepository = participantRepository;
        this.memberRepository = memberRepository;
        this.groupService = groupService;
        this.availableDateRepository = availableDateRepository;
        this.userRepository = userRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.aiClient = aiClient;
        this.preferenceRepository = preferenceRepository;
        this.memoryRepository = memoryRepository;
        this.objectMapper = objectMapper;
        this.suggestionRepository = suggestionRepository;
        this.groupMemoryRepository = groupMemoryRepository;
    }

    @Transactional
    public MeetingResponse createDraft(long userId, long groupId) {
        GroupMember creator = groupService.requireJoinedMember(userId, groupId);
        Meeting meeting = meetingRepository.save(Meeting.draft(creator.getGroup(), userId));
        return responseOf(meeting, List.of());
    }

    @Transactional
    public MeetingResponse update(long userId, long meetingId, UpdateMeetingRequest request) {
        Meeting meeting = requireEditableMeeting(userId, meetingId);
        meeting.updateConditions(
                request.purpose(),
                request.region(),
                request.scheduleSearchFrom(),
                request.scheduleSearchTo(),
                request.preferredTimeOfDay()
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
        return responseOf(meeting, participants);
    }

    public MeetingResponse find(long userId, long meetingId) {
        return toResponse(requireAccessibleMeeting(userId, meetingId));
    }

    public Object findSuggestions(long userId, long meetingId) {
        Meeting meeting = requireAccessibleMeeting(userId, meetingId);
        int generation = suggestionRepository.findFirstByMeetingIdOrderByGenerationDescRankAsc(meetingId)
                .map(MeetingSuggestion::getGeneration)
                .orElseThrow(() -> new BusinessException("SUGGESTION_NOT_FOUND", "생성된 제안을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        return suggestionRepository.findAllByMeetingIdAndGenerationOrderByRankAsc(meetingId, generation).stream()
                .map(value -> new SuggestionResponse(value.getId(), value.getGeneration(), value.getName(), value.getCategory(),
                        value.getAddress(), value.getProposedStartAt(), value.getProposedEndAt(), value.getExternalPlaceId(),
                        value.getExternalUrl(), value.getBusinessHours(), value.isBusinessHoursVerified(),
                        value.getOpenAtMeetingTime(), value.getReasons()))
                .toList();
    }

    public List<ChatMessageResponse> findChatMessages(long userId, long meetingId) {
        requireAccessibleMeeting(userId, meetingId);
        return chatMessageRepository.findAllByMeetingIdOrderByIdAsc(meetingId).stream()
                .map(value -> new ChatMessageResponse(value.getId(), value.getRole(), value.getContent(), value.getCreatedAt())).toList();
    }

    public Object findByGroup(long userId, long groupId, String timing) {
        groupService.requireJoinedMember(userId, groupId);
        List<Meeting> meetings = meetingRepository.findAllByGroupIdOrderByCreatedAtDesc(groupId);
        if ("UPCOMING".equalsIgnoreCase(timing)) {
            return meetings.stream()
                    .filter(meeting -> meeting.getStatus() == com.damoyeo.meeting.domain.MeetingStatus.CONFIRMED)
                    .filter(meeting -> meeting.getConfirmedStartAt() != null && meeting.getConfirmedStartAt().isAfter(java.time.Instant.now()))
                    .min(java.util.Comparator.comparing(Meeting::getConfirmedStartAt))
                    .map(MeetingListItemResponse::from)
                    .orElse(null);
        }
        if ("PAST".equalsIgnoreCase(timing)) {
            return meetings.stream()
                    .filter(meeting -> meeting.getStatus() == com.damoyeo.meeting.domain.MeetingStatus.CONFIRMED)
                    .filter(meeting -> meeting.getConfirmedStartAt() != null && !meeting.getConfirmedStartAt().isAfter(java.time.Instant.now()))
                    .sorted(java.util.Comparator.comparing(Meeting::getConfirmedStartAt).reversed())
                    .map(MeetingListItemResponse::from)
                    .toList();
        }
        return meetings.stream().map(MeetingListItemResponse::from).toList();
    }

    @Transactional
    public MeetingResponse submit(long userId, long meetingId) {
        Meeting meeting = requireEditableMeeting(userId, meetingId);
        meeting.submit(participantRepository.existsByMeetingId(meetingId));
        return toResponse(meeting);
    }

    @Transactional
    public MeetingResponse startPlanning(long userId, long meetingId) {
        Meeting meeting = requireEditableMeeting(userId, meetingId);
        meeting.startPlanning();
        String requestId = UUID.randomUUID().toString();
        AiClient.CandidateResponse response = aiClient.generateCandidates(meetingId, candidateRequest(meeting, requestId));
        validateCandidateResponse(response, requestId);
        storeCandidateResult(meeting, response);
        if ("OK".equals(response.status())) {
            meeting.completePlanning();
        } else {
            meeting.restoreReadyToPlan();
        }
        return toResponse(meeting);
    }

    @Transactional
    public MeetingResponse regenerate(long userId, long meetingId) {
        Meeting meeting = requireEditableMeeting(userId, meetingId);
        meeting.reopenForRegeneration();
        Map<String, Object> memory = new LinkedHashMap<>(memoryRepository.findById(meetingId)
                .map(MeetingMemory::getMemory).orElse(Map.of()));
        List<String> excluded = suggestionRepository.findFirstByMeetingIdOrderByGenerationDescRankAsc(meetingId)
                .map(value -> suggestionRepository.findAllByMeetingIdAndGenerationOrderByRankAsc(meetingId, value.getGeneration()))
                .orElse(List.of()).stream().map(MeetingSuggestion::getExternalPlaceId).toList();
        memory.put("excludedExternalPlaceIds", excluded);
        memoryRepository.findById(meetingId).ifPresentOrElse(value -> value.update(memory),
                () -> memoryRepository.save(new MeetingMemory(meeting, memory)));
        return toResponse(meeting);
    }

    @Transactional
    public MeetingResponse confirm(long userId, long meetingId, long suggestionId) {
        Meeting meeting = requireEditableMeeting(userId, meetingId);
        MeetingSuggestion suggestion = suggestionRepository.findByIdAndMeetingId(suggestionId, meetingId)
                .orElseThrow(() -> new BusinessException("SUGGESTION_NOT_FOUND", "후보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        meeting.confirm(suggestion);
        // 후보 확정은 사용자 의사결정이다. AI 장기기억 갱신 실패가 확정을 되돌리면 안 된다.
        try {
            updateGroupMemory(meeting, suggestion);
        } catch (BusinessException exception) {
            if (!isAiIntegrationFailure(exception)) {
                throw exception;
            }
        }
        return toResponse(meeting);
    }

    private boolean isAiIntegrationFailure(BusinessException exception) {
        return Set.of("AI_UNAVAILABLE", "AI_SERVICE_ERROR", "AI_RESPONSE_INVALID").contains(exception.getCode());
    }

    @Transactional
    public ContextChatResponse chatContext(long userId, long meetingId, String message) {
        Meeting meeting = requireEditableMeeting(userId, meetingId);
        String userMessage = message.trim();
        AiClient.MeetingContextResponse response = aiClient.summarizeContext(
                meetingId, List.of(userMessage), meeting.getPurpose()
        );
        if (response.reply() == null || response.reply().isBlank()
                || response.purpose() == null || response.purpose().isBlank() || response.purpose().length() > 1000) {
            throw new BusinessException("AI_RESPONSE_INVALID", "AI 응답 형식이 올바르지 않습니다.", HttpStatus.BAD_GATEWAY);
        }
        meeting.applyAiPurpose(response.purpose());
        chatMessageRepository.save(new MeetingChatMessage(meeting, ChatRole.USER, userMessage));
        chatMessageRepository.save(new MeetingChatMessage(meeting, ChatRole.ASSISTANT, response.reply()));
        return new ContextChatResponse(response.reply(), response.purpose());
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public RevisionChatResponse chatRevision(long userId, long meetingId, String message) {
        Meeting meeting = requireAccessibleMeeting(userId, meetingId);
        if (!meeting.getCreatedBy().equals(userId)) {
            throw new BusinessException("MEETING_EDIT_FORBIDDEN", "일정을 만든 사용자만 재생성을 요청할 수 있습니다.", HttpStatus.FORBIDDEN);
        }
        if (meeting.getStatus() != com.damoyeo.meeting.domain.MeetingStatus.PROPOSING) {
            throw new BusinessException("MEETING_NOT_PROPOSING", "제안이 생성된 일정만 재생성할 수 있습니다.", HttpStatus.CONFLICT);
        }
        Map<String, Object> memory = new LinkedHashMap<>(memoryRepository.findById(meetingId)
                .map(MeetingMemory::getMemory)
                .orElseThrow(() -> new BusinessException("SUGGESTION_NOT_FOUND", "생성된 제안을 찾을 수 없습니다.", HttpStatus.NOT_FOUND)));
        Map<String, Object> candidateResult = (Map<String, Object>) memory.get("candidateResult");
        if (candidateResult == null || !(candidateResult.get("suggestions") instanceof List<?> rawSuggestions)) {
            throw new BusinessException("SUGGESTION_NOT_FOUND", "생성된 제안을 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
        }
        List<AiClient.CurrentSuggestion> suggestions = rawSuggestions.stream().map(raw -> {
            Map<String, Object> value = (Map<String, Object>) raw;
            return new AiClient.CurrentSuggestion(
                    ((Number) value.get("rank")).intValue(), (String) value.get("externalPlaceId"),
                    (String) value.get("name"), (String) value.get("category"),
                    (String) value.get("proposedStartAt"), (String) value.get("proposedEndAt"),
                    (List<String>) value.get("reasons")
            );
        }).toList();
        Map<String, Object> priorDraft = memory.get("revisionDraft") instanceof Map<?, ?> value
                ? (Map<String, Object>) value : Map.of();
        String draftPurpose = (String) priorDraft.getOrDefault("draftPurpose", meeting.getPurpose());
        List<String> excluded = (List<String>) priorDraft.getOrDefault("excludedExternalPlaceIds", List.of());
        AiClient.RevisionResponse response = aiClient.revise(meetingId,
                new AiClient.RevisionRequest(List.of(message.trim()), draftPurpose, suggestions, excluded));
        if (response.reply() == null || response.reply().isBlank() || response.draftPurpose() == null
                || response.draftPurpose().isBlank() || response.draftPurpose().length() > 1000
                || response.excludedExternalPlaceIds() == null || response.uiChangeRequests() == null) {
            throw new BusinessException("AI_RESPONSE_INVALID", "AI 응답 형식이 올바르지 않습니다.", HttpStatus.BAD_GATEWAY);
        }
        List<RevisionChatResponse.UiChangeRequest> changes = response.uiChangeRequests().stream()
                .map(value -> new RevisionChatResponse.UiChangeRequest(value.field(), value.mentionedValue(), value.question()))
                .toList();
        memory.put("revisionDraft", Map.of(
                "draftPurpose", response.draftPurpose(),
                "excludedExternalPlaceIds", response.excludedExternalPlaceIds(),
                "uiChangeRequests", changes
        ));
        memoryRepository.findById(meetingId).orElseThrow().update(memory);
        chatMessageRepository.save(new MeetingChatMessage(meeting, ChatRole.USER, message.trim()));
        chatMessageRepository.save(new MeetingChatMessage(meeting, ChatRole.ASSISTANT, response.reply()));
        return new RevisionChatResponse(response.reply(), response.draftPurpose(), response.excludedExternalPlaceIds(), changes);
    }

    @Transactional
    public MeetingChatResponse chat(long userId, long meetingId, String message) {
        Meeting meeting = requireEditableMeeting(userId, meetingId);
        if (meeting.getStatus() != com.damoyeo.meeting.domain.MeetingStatus.READY_TO_PLAN) {
            throw new BusinessException("MEETING_NOT_READY", "가능 날짜 제출 완료 후 조율 채팅을 시작할 수 있습니다.", HttpStatus.CONFLICT);
        }
        String userMessage = message.trim();
        Map<String, Object> memory = new LinkedHashMap<>(memoryRepository.findById(meetingId)
                .map(MeetingMemory::getMemory).orElse(Map.of()));
        String currentContext = memory.get("meetingContext") instanceof String value ? value : null;
        List<String> excluded = memory.get("excludedExternalPlaceIds") instanceof List<?> values
                ? values.stream().map(String::valueOf).toList() : List.of();
        List<AiClient.CurrentSuggestion> currentSuggestions = suggestionRepository.findFirstByMeetingIdOrderByGenerationDescRankAsc(meetingId)
                .map(value -> suggestionRepository.findAllByMeetingIdAndGenerationOrderByRankAsc(meetingId, value.getGeneration()))
                .orElse(List.of()).stream().map(value -> new AiClient.CurrentSuggestion(value.getRank(),
                        value.getExternalPlaceId(), value.getName(), value.getCategory(), value.getProposedStartAt().toString(),
                        value.getProposedEndAt().toString(), List.of())).toList();
        AiClient.MeetingChatResponse response = aiClient.chat(meetingId,
                new AiClient.MeetingChatRequest(List.of(userMessage), currentContext, currentSuggestions, excluded));
        if (response.reply() == null || response.reply().isBlank() || response.updatedContext() == null
                || response.updatedContext().isBlank() || response.updatedContext().length() > 1000
                || response.excludedExternalPlaceIds() == null || response.uiChangeRequests() == null) {
            throw new BusinessException("AI_RESPONSE_INVALID", "AI 응답 형식이 올바르지 않습니다.", HttpStatus.BAD_GATEWAY);
        }
        List<RevisionChatResponse.UiChangeRequest> changes = response.uiChangeRequests().stream()
                .map(value -> new RevisionChatResponse.UiChangeRequest(value.field(), value.mentionedValue(), value.question())).toList();
        memory.put("meetingContext", response.updatedContext());
        memory.put("excludedExternalPlaceIds", response.excludedExternalPlaceIds());
        memoryRepository.findById(meetingId).ifPresentOrElse(value -> value.update(memory),
                () -> memoryRepository.save(new MeetingMemory(meeting, memory)));
        chatMessageRepository.save(new MeetingChatMessage(meeting, ChatRole.USER, userMessage));
        chatMessageRepository.save(new MeetingChatMessage(meeting, ChatRole.ASSISTANT, response.reply()));
        return new MeetingChatResponse(response.reply(), response.updatedContext(), response.excludedExternalPlaceIds(), changes);
    }

    private AiClient.CandidateRequest candidateRequest(Meeting meeting, String requestId) {
        List<AiClient.CandidateParticipant> participants = participantRepository.findAllByMeetingIdOrderByIdAsc(meeting.getId())
                .stream()
                .map(participant -> {
                    long participantUserId = participant.getGroupMember().getUserId();
                    List<String> dates = availableDateRepository
                            .findAllByMeetingParticipantIdOrderByAvailableDateAsc(participant.getId()).stream()
                            .map(date -> date.getAvailableDate().toString())
                            .toList();
                    List<AiClient.CandidatePreference> preferences = preferenceRepository
                            .findAllByUserIdOrderByIdAsc(participantUserId).stream()
                            .map(preference -> new AiClient.CandidatePreference(
                                    preference.getVocabulary().getCode(), preference.getSentiment().name(),
                                    preference.getStrength().name(), preference.getRawValue()
                            )).toList();
                    return new AiClient.CandidateParticipant(participantUserId, dates, preferences);
                }).toList();
        Map<String, Object> memory = memoryRepository.findById(meeting.getId()).map(MeetingMemory::getMemory).orElse(Map.of());
        String meetingContext = memory.get("meetingContext") instanceof String value ? value : meeting.getPurpose();
        Object groupMemory = groupMemoryRepository.findById(meeting.getGroup().getId()).map(GroupMemory::getSummary).map(summary -> Map.of("summary", summary)).orElse(null);
        List<String> excluded = memory.get("excludedExternalPlaceIds") instanceof List<?> values
                ? values.stream().map(String::valueOf).toList() : List.of();
        return new AiClient.CandidateRequest(
                "1.0", requestId,
                new AiClient.CandidateMeeting(meeting.getId(), meetingContext, meeting.getRegion(),
                        meeting.getScheduleSearchFrom().toString(), meeting.getScheduleSearchTo().toString(),
                        meeting.getPreferredTimeOfDay().name(), 120, SERVICE_ZONE.getId()),
                participants, memory, groupMemory, excluded
        );
    }

    private void validateCandidateResponse(AiClient.CandidateResponse response, String requestId) {
        if (response == null || !requestId.equals(response.requestId()) || response.status() == null
                || response.suggestions() == null || response.verificationTimedOut() == null
                || !(response.status().equals("OK") || response.status().equals("NO_COMMON_SLOT")
                || response.status().equals("NO_CANDIDATE") || response.status().equals("CONFLICT"))) {
            throw new BusinessException("AI_RESPONSE_INVALID", "AI 응답 형식이 올바르지 않습니다.", HttpStatus.BAD_GATEWAY);
        }
        if (response.status().equals("OK") && (response.suggestions().isEmpty() || response.suggestions().size() > 3)) {
            throw new BusinessException("AI_RESPONSE_INVALID", "AI 응답 형식이 올바르지 않습니다.", HttpStatus.BAD_GATEWAY);
        }
    }

    private void storeCandidateResult(Meeting meeting, AiClient.CandidateResponse response) {
        Map<String, Object> memory = new LinkedHashMap<>(memoryRepository.findById(meeting.getId())
                .map(MeetingMemory::getMemory).orElse(Map.of()));
        int generation = suggestionRepository.findFirstByMeetingIdOrderByGenerationDescRankAsc(meeting.getId())
                .map(value -> value.getGeneration() + 1).orElse(1);
        suggestionRepository.saveAll(response.suggestions().stream()
                .map(value -> new MeetingSuggestion(meeting, generation, value)).toList());
        memory.put("candidateResult", Map.of("generation", generation, "summary", response.summary()));
        memoryRepository.findById(meeting.getId())
                .ifPresentOrElse(value -> value.update(memory), () -> memoryRepository.save(new MeetingMemory(meeting, memory)));
    }

    private void updateGroupMemory(Meeting meeting, MeetingSuggestion suggestion) {
        String previous = groupMemoryRepository.findById(meeting.getGroup().getId()).map(GroupMemory::getSummary).orElse(null);
        String context = memoryRepository.findById(meeting.getId()).map(MeetingMemory::getMemory)
                .map(memory -> String.valueOf(memory.getOrDefault("meetingContext", meeting.getPurpose()))).orElse(meeting.getPurpose());
        AiClient.GroupMemoryResponse response = aiClient.updateGroupMemory(meeting.getGroup().getId(),
                new AiClient.GroupMemoryRequest(previous, new AiClient.ConfirmedMeeting(meeting.getId(), meeting.getRegion(),
                        suggestion.getCategory(), suggestion.getName(), suggestion.getAddress(), suggestion.getProposedStartAt().toString(),
                        suggestion.getProposedEndAt().toString(), context)));
        if (response.updatedGroupSummary() == null || response.updatedGroupSummary().isBlank() || response.updatedGroupSummary().length() > 2000) {
            throw new BusinessException("AI_RESPONSE_INVALID", "AI 응답 형식이 올바르지 않습니다.", HttpStatus.BAD_GATEWAY);
        }
        groupMemoryRepository.findById(meeting.getGroup().getId()).ifPresentOrElse(
                value -> value.update(response.updatedGroupSummary()),
                () -> groupMemoryRepository.save(new GroupMemory(meeting.getGroup(), response.updatedGroupSummary())));
    }

    public record SuggestionResponse(Long id, int generation, String name, String category, String address,
                                     java.time.Instant proposedStartAt, java.time.Instant proposedEndAt, String externalPlaceId,
                                     String externalUrl, String businessHours, boolean businessHoursVerified,
                                     Boolean openAtMeetingTime, List<String> reasons) {}
    public record ChatMessageResponse(Long id, ChatRole role, String content, java.time.Instant createdAt) {}

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
        return responseOf(meeting, participantRepository.findAllByMeetingIdOrderByIdAsc(meeting.getId()));
    }

    private MeetingResponse responseOf(Meeting meeting, List<MeetingParticipant> participants) {
        Map<Long, User> users = userRepository.findAllById(
                participants.stream().map(participant -> participant.getGroupMember().getUserId()).toList()
        ).stream().collect(Collectors.toMap(User::getId, Function.identity()));
        Map<Long, List<LocalDate>> selectedDates = participants.stream().collect(Collectors.toMap(
                MeetingParticipant::getId,
                participant -> availableDateRepository
                        .findAllByMeetingParticipantIdOrderByAvailableDateAsc(participant.getId()).stream()
                        .map(value -> value.getAvailableDate())
                        .toList()
        ));
        return MeetingResponse.of(meeting, participants, users, selectedDates);
    }
}
