package com.damoyeo.calendar;

import com.damoyeo.common.exception.BusinessException;
import com.damoyeo.meeting.domain.CalendarEventStatus;
import com.damoyeo.meeting.domain.Meeting;
import com.damoyeo.meeting.domain.MeetingCalendarEvent;
import com.damoyeo.meeting.domain.MeetingSuggestion;
import com.damoyeo.meeting.repository.MeetingCalendarEventRepository;
import com.damoyeo.user.domain.User;
import com.damoyeo.user.repository.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
public class GoogleCalendarService {

    private static final String GOOGLE_REGISTRATION_ID = "google";
    private static final String CALENDAR_EVENTS_SCOPE = "https://www.googleapis.com/auth/calendar.events";
    private static final String CALENDAR_FREE_BUSY_SCOPE = "https://www.googleapis.com/auth/calendar.freebusy";
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

    /** 일정 제목이나 상세 내용은 읽지 않고 검색 범위에서 바쁜 날짜만 반환한다. */
    public BusyDatesResult findBusyDates(long userId, LocalDate from, LocalDate to) {
        User user = userRepository.findById(userId).orElseThrow();
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                GOOGLE_REGISTRATION_ID, user.getGoogleSubject()
        );
        if (client == null || !client.getAccessToken().getScopes().contains(CALENDAR_FREE_BUSY_SCOPE)) {
            return new BusyDatesResult(false, List.of());
        }
        try {
            Instant timeMin = from.atStartOfDay(SERVICE_ZONE).toInstant();
            Instant timeMax = to.plusDays(1).atStartOfDay(SERVICE_ZONE).toInstant();
            FreeBusyResponse response = restClient.post()
                    .uri("/freeBusy")
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> headers.setBearerAuth(client.getAccessToken().getTokenValue()))
                    .body(new FreeBusyRequest(
                            timeMin.toString(), timeMax.toString(), SERVICE_ZONE.getId(),
                            List.of(new FreeBusyCalendarRequest("primary"))
                    ))
                    .retrieve()
                    .body(FreeBusyResponse.class);
            if (response == null || response.calendars() == null) {
                throw new IllegalStateException("Google Calendar FreeBusy 응답이 비어 있습니다.");
            }
            Set<LocalDate> busyDates = new LinkedHashSet<>();
            FreeBusyCalendar primary = response.calendars().get("primary");
            if (primary != null && primary.busy() != null) {
                for (BusyInterval interval : primary.busy()) {
                    addBusyDates(busyDates, interval, from, to);
                }
            }
            return new BusyDatesResult(true, busyDates.stream().sorted().toList());
        } catch (RestClientResponseException exception) {
            throw new BusinessException(
                    "CALENDAR_READ_FAILED",
                    "Google Calendar 일정 유무를 확인하지 못했습니다.",
                    HttpStatus.BAD_GATEWAY
            );
        } catch (RuntimeException exception) {
            if (exception instanceof BusinessException businessException) {
                throw businessException;
            }
            throw new BusinessException(
                    "CALENDAR_READ_FAILED",
                    "Google Calendar 일정 유무를 확인하지 못했습니다.",
                    HttpStatus.BAD_GATEWAY
            );
        }
    }

    private void addBusyDates(Set<LocalDate> target, BusyInterval interval, LocalDate from, LocalDate to) {
        Instant start = Instant.parse(interval.start());
        Instant end = Instant.parse(interval.end());
        if (!end.isAfter(start)) {
            return;
        }
        LocalDate first = start.atZone(SERVICE_ZONE).toLocalDate();
        LocalDate last = end.minusNanos(1).atZone(SERVICE_ZONE).toLocalDate();
        for (LocalDate date = first; !date.isAfter(last); date = date.plusDays(1)) {
            if (!date.isBefore(from) && !date.isAfter(to)) {
                target.add(date);
            }
        }
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
    private record FreeBusyRequest(String timeMin, String timeMax, String timeZone,
                                   List<FreeBusyCalendarRequest> items) {}
    private record FreeBusyCalendarRequest(String id) {}
    private record FreeBusyResponse(Map<String, FreeBusyCalendar> calendars) {}
    private record FreeBusyCalendar(List<BusyInterval> busy) {}
    private record BusyInterval(String start, String end) {}
    public record BusyDatesResult(boolean calendarConnected, List<LocalDate> busyDates) {}
}
