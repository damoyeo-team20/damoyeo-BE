package com.damoyeo.ai;

import com.damoyeo.common.exception.BusinessException;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/** Back → AI boundary. Controllers and domain services never call the AI server directly. */
@Component
public class AiClient {

    private final RestClient restClient;

    public AiClient(
            RestClient.Builder builder,
            @Value("${app.ai.base-url:http://localhost:8000}") String baseUrl,
            @Value("${INTERNAL_API_KEY:}") String internalApiKey
    ) {
        RestClient.Builder configured = builder.baseUrl(baseUrl);
        if (!internalApiKey.isBlank()) {
            configured.defaultHeader("X-Internal-Api-Key", internalApiKey);
        }
        this.restClient = configured.build();
    }

    public PreferenceExtractResponse extractPreferences(List<String> messages) {
        return post("/ai/preferences/extract", new PreferenceExtractRequest(messages), PreferenceExtractResponse.class);
    }

    public MeetingContextResponse summarizeContext(long meetingId, List<String> messages, String currentPurpose) {
        return post("/ai/meetings/" + meetingId + "/context", new MeetingContextRequest(messages, currentPurpose), MeetingContextResponse.class);
    }

    public CandidateResponse generateCandidates(long meetingId, CandidateRequest request) {
        return post("/ai/meetings/" + meetingId + "/candidates", request, CandidateResponse.class);
    }

    public RevisionResponse revise(long meetingId, RevisionRequest request) {
        return post("/ai/meetings/" + meetingId + "/revise", request, RevisionResponse.class);
    }

    public MeetingChatResponse chat(long meetingId, MeetingChatRequest request) {
        return post("/ai/meetings/" + meetingId + "/chat", request, MeetingChatResponse.class);
    }

    public GroupMemoryResponse updateGroupMemory(long groupId, GroupMemoryRequest request) {
        return post("/ai/groups/" + groupId + "/memory", request, GroupMemoryResponse.class);
    }

    private <T> T post(String path, Object body, Class<T> responseType) {
        try {
            T response = restClient.post().uri(path).body(body).retrieve().body(responseType);
            if (response == null) {
                throw invalidResponse();
            }
            return response;
        } catch (BusinessException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            throw new BusinessException("AI_SERVICE_ERROR", "AI 서비스 요청에 실패했습니다.", HttpStatus.BAD_GATEWAY);
        } catch (ResourceAccessException exception) {
            throw new BusinessException("AI_UNAVAILABLE", "AI 서비스에 연결할 수 없습니다.", HttpStatus.SERVICE_UNAVAILABLE);
        } catch (RuntimeException exception) {
            throw invalidResponse();
        }
    }

    private BusinessException invalidResponse() {
        return new BusinessException("AI_RESPONSE_INVALID", "AI 응답 형식이 올바르지 않습니다.", HttpStatus.BAD_GATEWAY);
    }

    public record PreferenceExtractRequest(List<String> messages) {}
    public record PreferenceExtractResponse(String reply, List<ExtractedPreference> extractedPreferences) {}
    public record ExtractedPreference(String vocabularyCode, String displayName, String domain, String rawValue,
                                      String sentiment, String strength, String mappingType) {}
    public record MeetingContextRequest(List<String> messages, String currentPurpose) {}
    public record MeetingContextResponse(String reply, String purpose) {}
    public record CandidateRequest(String contractVersion, String requestId, CandidateMeeting meeting,
                                   List<CandidateParticipant> participants, Object meetingMemory, Object groupMemory,
                                   List<String> excludedExternalPlaceIds) {}
    public record CandidateMeeting(long id, String purpose, String region, String scheduleSearchFrom,
                                   String scheduleSearchTo, String preferredTimeOfDay, Integer durationMinutes,
                                   String timezone) {}
    public record CandidateParticipant(long userId, List<String> selectedDates, List<CandidatePreference> preferences) {}
    public record CandidatePreference(String vocabularyCode, String sentiment, String strength, String rawValue) {}
    public record CandidateResponse(String requestId, String status, Integer appliedDurationMinutes, String summary,
                                    List<CandidateSuggestion> suggestions, ActionRequired actionRequired,
                                    Boolean verificationTimedOut) {}
    public record CandidateSuggestion(Integer rank, String category, String placeProvider, String externalPlaceId,
                                      String name, String address, Double latitude, Double longitude, String externalUrl,
                                      String proposedStartAt, String proposedEndAt, String businessHours,
                                      Boolean businessHoursVerified, Boolean openAtMeetingTime,
                                      List<String> matchedPreferenceDomains, List<String> reasons,
                                      List<String> sourceUrls, String checkedAt) {}
    public record ActionRequired(String type, String message, String hostRequest, List<String> conflictingPreferenceCodes) {}
    public record RevisionRequest(List<String> messages, String currentDraftPurpose,
                                  List<CurrentSuggestion> currentSuggestions, List<String> excludedExternalPlaceIds) {}
    public record CurrentSuggestion(Integer rank, String externalPlaceId, String name, String category,
                                    String proposedStartAt, String proposedEndAt, List<String> reasons) {}
    public record RevisionResponse(String reply, String draftPurpose, List<String> excludedExternalPlaceIds,
                                   List<UiChangeRequest> uiChangeRequests) {}
    public record UiChangeRequest(String field, String mentionedValue, String question) {}
    public record GroupMemoryRequest(String previousGroupSummary, ConfirmedMeeting confirmedMeeting) {}
    public record ConfirmedMeeting(long meetingId, String region, String category, String placeName,
                                   String address, String startAt, String endAt, String meetingContext) {}
    public record GroupMemoryResponse(String updatedGroupSummary) {}
    public record MeetingChatRequest(List<String> messages, String currentContext,
                                     List<CurrentSuggestion> currentSuggestions, List<String> excludedExternalPlaceIds) {}
    public record MeetingChatResponse(String reply, String updatedContext, List<String> excludedExternalPlaceIds,
                                      List<UiChangeRequest> uiChangeRequests) {}
}
