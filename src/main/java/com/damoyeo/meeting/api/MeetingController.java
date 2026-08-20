package com.damoyeo.meeting.api;

import com.damoyeo.common.auth.CurrentUserProvider;
import com.damoyeo.meeting.dto.MeetingResponse;
import com.damoyeo.meeting.dto.MeetingListItemResponse;
import java.util.List;
import org.springframework.web.bind.annotation.RequestParam;
import com.damoyeo.meeting.dto.UpdateMeetingRequest;
import com.damoyeo.meeting.dto.UpdateParticipantsRequest;
import com.damoyeo.meeting.dto.SubmitAvailabilityRequest;
import com.damoyeo.meeting.dto.AvailabilityResponse;
import com.damoyeo.meeting.dto.CoordinationStatusResponse;
import com.damoyeo.meeting.dto.ConfirmMeetingRequest;
import com.damoyeo.meeting.dto.MeetingChatRequest;
import com.damoyeo.meeting.dto.MeetingChatResponse;
import com.damoyeo.meeting.service.MeetingAvailabilityService;
import com.damoyeo.meeting.service.MeetingService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class MeetingController {

    private final CurrentUserProvider currentUserProvider;
    private final MeetingService meetingService;
    private final MeetingAvailabilityService availabilityService;

    public MeetingController(
            CurrentUserProvider currentUserProvider,
            MeetingService meetingService,
            MeetingAvailabilityService availabilityService
    ) {
        this.currentUserProvider = currentUserProvider;
        this.meetingService = meetingService;
        this.availabilityService = availabilityService;
    }

    @PostMapping("/groups/{groupId}/meetings")
    public ResponseEntity<MeetingResponse> createDraft(@PathVariable long groupId) {
        MeetingResponse response = meetingService.createDraft(currentUserProvider.getCurrentUserId(), groupId);
        return ResponseEntity.created(URI.create("/api/meetings/" + response.id())).body(response);
    }

    @GetMapping("/groups/{groupId}/meetings")
    public Object findGroupMeetings(
            @PathVariable long groupId,
            @RequestParam(defaultValue = "ALL") String timing
    ) {
        return meetingService.findByGroup(currentUserProvider.getCurrentUserId(), groupId, timing);
    }

    @GetMapping("/meetings/{meetingId}")
    public MeetingResponse find(@PathVariable long meetingId) {
        return meetingService.find(currentUserProvider.getCurrentUserId(), meetingId);
    }

    @PutMapping("/meetings/{meetingId}/conditions")
    public MeetingResponse update(
            @PathVariable long meetingId,
            @Valid @RequestBody UpdateMeetingRequest request
    ) {
        return meetingService.update(currentUserProvider.getCurrentUserId(), meetingId, request);
    }

    @PutMapping("/meetings/{meetingId}/participants")
    public MeetingResponse updateParticipants(
            @PathVariable long meetingId,
            @Valid @RequestBody UpdateParticipantsRequest request
    ) {
        return meetingService.updateParticipants(
                currentUserProvider.getCurrentUserId(),
                meetingId,
                request.groupMemberIds()
        );
    }

    @PostMapping("/meetings/{meetingId}/submit")
    public MeetingResponse submit(@PathVariable long meetingId) {
        return meetingService.submit(currentUserProvider.getCurrentUserId(), meetingId);
    }

    @PostMapping("/meetings/{meetingId}/plan")
    public MeetingResponse prepareForChat(@PathVariable long meetingId) {
        return meetingService.prepareForChat(currentUserProvider.getCurrentUserId(), meetingId);
    }

    @PostMapping("/meetings/{meetingId}/generate")
    public MeetingResponse generate(@PathVariable long meetingId) {
        return meetingService.startPlanning(currentUserProvider.getCurrentUserId(), meetingId);
    }

    @PostMapping("/meetings/{meetingId}/regenerate")
    public MeetingResponse regenerate(@PathVariable long meetingId) {
        return meetingService.regenerate(currentUserProvider.getCurrentUserId(), meetingId);
    }

    @PostMapping("/meetings/{meetingId}/confirm")
    public MeetingResponse confirm(@PathVariable long meetingId, @Valid @RequestBody ConfirmMeetingRequest request) {
        return meetingService.confirm(currentUserProvider.getCurrentUserId(), meetingId, request.suggestionId());
    }

    @GetMapping("/meetings/{meetingId}/suggestions")
    public Object findSuggestions(@PathVariable long meetingId) {
        return meetingService.findSuggestions(currentUserProvider.getCurrentUserId(), meetingId);
    }

    @GetMapping("/meetings/{meetingId}/calendar-events")
    public Object findCalendarEvents(@PathVariable long meetingId) {
        return meetingService.findCalendarEvents(currentUserProvider.getCurrentUserId(), meetingId);
    }

    @GetMapping("/meetings/{meetingId}/chat/messages")
    public Object findChatMessages(@PathVariable long meetingId) {
        return meetingService.findChatMessages(currentUserProvider.getCurrentUserId(), meetingId);
    }

    @PostMapping("/meetings/{meetingId}/chat/messages")
    public MeetingChatResponse chat(@PathVariable long meetingId, @Valid @RequestBody MeetingChatRequest request) {
        return meetingService.chat(currentUserProvider.getCurrentUserId(), meetingId, request.message());
    }

    @PutMapping({"/meetings/{meetingId}/my-availability", "/meetings/{meetingId}/availability"})
    public AvailabilityResponse submitAvailability(
            @PathVariable long meetingId,
            @Valid @RequestBody SubmitAvailabilityRequest request
    ) {
        return availabilityService.submit(
                currentUserProvider.getCurrentUserId(),
                meetingId,
                request.selectedDates()
        );
    }

    @GetMapping("/meetings/{meetingId}/availability/me")
    public AvailabilityResponse findMyAvailability(@PathVariable long meetingId) {
        return availabilityService.findMine(currentUserProvider.getCurrentUserId(), meetingId);
    }

    @GetMapping("/meetings/{meetingId}/coordination")
    public CoordinationStatusResponse findCoordinationStatus(@PathVariable long meetingId) {
        return availabilityService.findCoordinationStatus(currentUserProvider.getCurrentUserId(), meetingId);
    }
}
