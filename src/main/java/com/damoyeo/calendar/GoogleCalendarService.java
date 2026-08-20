package com.damoyeo.calendar;

import com.damoyeo.meeting.domain.CalendarEventStatus;
import com.damoyeo.meeting.domain.Meeting;
import com.damoyeo.meeting.domain.MeetingCalendarEvent;
import com.damoyeo.meeting.domain.MeetingSuggestion;
import com.damoyeo.meeting.repository.MeetingCalendarEventRepository;
import com.damoyeo.user.domain.User;
import com.damoyeo.user.repository.UserRepository;
import java.time.ZoneId;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class GoogleCalendarService {

    private static final String GOOGLE_REGISTRATION_ID = "google";
    private static final String CALENDAR_EVENTS_SCOPE = "https://www.googleapis.com/auth/calendar.events";
    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    private final OAuth2AuthorizedClientService authorizedClientService;
    private final UserRepository userRepository;
    private final MeetingCalendarEventRepository calendarEventRepository;
    private final RestClient restClient;

    public GoogleCalendarService(
            OAuth2AuthorizedClientService authorizedClientService,
            UserRepository userRepository,
            MeetingCalendarEventRepository calendarEventRepository,
            RestClient.Builder restClientBuilder
    ) {
        this.authorizedClientService = authorizedClientService;
        this.userRepository = userRepository;
        this.calendarEventRepository = calendarEventRepository;
        this.restClient = restClientBuilder.clone().baseUrl("https://www.googleapis.com/calendar/v3").build();
    }

    /** 선택된 일정 참여자 전원의 primary calendar에 각각 동일한 이벤트를 등록한다. */
    public void createForParticipants(Meeting meeting, MeetingSuggestion suggestion, List<Long> userIds) {
        userIds.forEach(userId -> createForUser(meeting, suggestion, userId));
    }

    private void createForUser(Meeting meeting, MeetingSuggestion suggestion, long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                GOOGLE_REGISTRATION_ID, user.getGoogleSubject()
        );
        if (client == null || !client.getAccessToken().getScopes().contains(CALENDAR_EVENTS_SCOPE)) {
            calendarEventRepository.save(MeetingCalendarEvent.failure(
                    meeting, userId, CalendarEventStatus.NOT_CONNECTED, "Google Calendar 권한이 없습니다."
            ));
            return;
        }
        try {
            CalendarEventResponse response = restClient.post()
                    .uri("/calendars/primary/events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> headers.setBearerAuth(client.getAccessToken().getTokenValue()))
                    .body(new CalendarEventRequest(
                            meeting.getGroup().getName(),
                            suggestion.getName() + " · " + suggestion.getAddress(),
                            new CalendarEventDateTime(suggestion.getProposedStartAt().toString(), SERVICE_ZONE.getId()),
                            new CalendarEventDateTime(suggestion.getProposedEndAt().toString(), SERVICE_ZONE.getId())
                    ))
                    .retrieve()
                    .body(CalendarEventResponse.class);
            if (response == null || response.id() == null || response.id().isBlank()) {
                throw new IllegalStateException("Google Calendar 응답에 event id가 없습니다.");
            }
            calendarEventRepository.save(MeetingCalendarEvent.success(meeting, userId, response.id()));
        } catch (RuntimeException exception) {
            calendarEventRepository.save(MeetingCalendarEvent.failure(
                    meeting, userId, CalendarEventStatus.FAILED, "Google Calendar 등록에 실패했습니다."
            ));
        }
    }

    private record CalendarEventRequest(String summary, String location,
                                        CalendarEventDateTime start, CalendarEventDateTime end) {}
    private record CalendarEventDateTime(String dateTime, String timeZone) {}
    private record CalendarEventResponse(String id) {}
}
