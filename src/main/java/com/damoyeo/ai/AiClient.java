package com.damoyeo.ai;

import com.damoyeo.common.exception.BusinessException;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/** Back → AI boundary. Controllers and domain services never call the AI server directly. */
@Component
public class AiClient {

    private static final Logger log = LoggerFactory.getLogger(AiClient.class);

    private final RestClient restClient;

    public AiClient(
            RestClient.Builder builder,
            @Value("${app.ai.base-url:http://localhost:8000}") String baseUrl,
            @Value("${INTERNAL_API_KEY:}") String internalApiKey
    ) {
        RestClient.Builder configured = builder.clone().baseUrl(baseUrl);
        if (!internalApiKey.isBlank()) {
            configured.defaultHeader("X-Internal-Api-Key", internalApiKey);
        }
        configured.requestFactory(new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory()));
        this.restClient = configured.build();
    }

    public PreferenceExtractResponse extractPreferences(List<String> messages) {
        return post("/ai/preferences/extract", new PreferenceExtractRequest(messages), PreferenceExtractResponse.class);
    }

    public MeetingContextResponse summarizeContext(long meetingId, List<ChatTurn> history) {
        return post("/ai/meetings/" + meetingId + "/context", new MeetingContextRequest(history), MeetingContextResponse.class);
    }

    public ScheduleResolutionResponse resolveSchedule(long meetingId, ScheduleResolutionRequest request) {
        return post("/ai/meetings/" + meetingId + "/schedule", request, ScheduleResolutionResponse.class);
    }

    public CandidateResponse generateCandidates(long meetingId, CandidateRequest request) {
        return post("/ai/meetings/" + meetingId + "/candidates", request, CandidateResponse.class);
    }

    public MeetingChatResponse chat(long meetingId, List<ChatTurn> history, String message) {
        return post("/ai/meetings/" + meetingId + "/context/messages",
                new MeetingChatRequest(history, message), MeetingChatResponse.class);
    }

    private <T> T post(String path, Object body, Class<T> responseType) {
        try {
            T response = restClient.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(responseType);
            if (response == null) {
                throw invalidResponse();
            }
            return response;
        } catch (BusinessException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            log.warn("AI request failed: path={}, status={}, response={}",
                    path, exception.getStatusCode(), sanitizedResponse(exception.getResponseBodyAsString()));
            throw new BusinessException("AI_SERVICE_ERROR", "AI 서비스 요청에 실패했습니다.", HttpStatus.BAD_GATEWAY);
        } catch (ResourceAccessException exception) {
            log.warn("AI service unavailable: path={}, cause={}, rootCause={}", path, exception.getMessage(),
                    exception.getCause());
            throw new BusinessException("AI_UNAVAILABLE", "AI 서비스에 연결할 수 없습니다.", HttpStatus.SERVICE_UNAVAILABLE);
        } catch (RuntimeException exception) {
            throw invalidResponse();
        }
    }

    private String sanitizedResponse(String response) {
        if (response == null || response.isBlank()) {
            return "<empty>";
        }
        return response.substring(0, Math.min(response.length(), 1000))
                .replaceAll("[\\r\\n]", " ");
    }

    private BusinessException invalidResponse() {
        return new BusinessException("AI_RESPONSE_INVALID", "AI 응답 형식이 올바르지 않습니다.", HttpStatus.BAD_GATEWAY);
    }

    public record PreferenceExtractRequest(List<String> messages) {}
    public record PreferenceExtractResponse(String reply, List<ExtractedPreference> extractedPreferences) {}
    public record ExtractedPreference(String vocabularyCode, String displayName, String domain, String rawValue,
                                      String sentiment, String strength, String mappingType) {}
    public record ChatTurn(String role, String content) {}
    public record MeetingContextRequest(List<ChatTurn> history) {}
    public record MeetingContextResponse(String reply, String purpose) {}
    public record ScheduleResolutionRequest(List<String> commonAvailableDates, String preferredTimeOfDay,
                                            Integer durationMinutes, String timezone) {}
    public record ScheduleResolutionResponse(java.time.Instant resolvedStartAt, java.time.Instant resolvedEndAt,
                                             String reason) {}
    public record CandidateRequest(String contractVersion, String requestId, CandidateMeeting meeting,
                                   ConfirmedSlot confirmedSlot, List<CandidateParticipant> participants, Object meetingMemory,
                                   List<String> excludedExternalPlaceIds) {}
    public record CandidateMeeting(long id, String purpose, String region) {}
    public record ConfirmedSlot(String confirmedStartAt, String confirmedEndAt) {}
    public record CandidateParticipant(long userId, List<CandidatePreference> preferences) {}
    public record CandidatePreference(String vocabularyCode, String sentiment, String strength, String rawValue) {}
    public record CandidateResponse(String requestId, String status, Integer appliedDurationMinutes, String summary,
                                    List<Tag> meetingTags, List<CandidateSuggestion> suggestions, ActionRequired actionRequired,
                                    Boolean verificationTimedOut) {}
    public record CandidateSuggestion(Integer rank, String category, String placeProvider, String externalPlaceId,
                                      String name, String address, Double latitude, Double longitude, String externalUrl,
                                      String proposedStartAt, String proposedEndAt, String businessHours,
                                      Boolean businessHoursVerified, Boolean openAtMeetingTime,
                                      List<String> matchedPreferenceDomains, List<String> reasons,
                                      List<Tag> tags, List<String> sourceUrls, String checkedAt) {}
    public record Tag(String code, String label) {}
    public record ActionRequired(String type, String message, String hostRequest, List<String> conflictingPreferenceCodes) {}
    public record MeetingChatRequest(List<ChatTurn> history, String message) {}
    public record MeetingChatResponse(String reply) {}
}
