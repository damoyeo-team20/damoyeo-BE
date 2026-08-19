package com.damoyeo.meeting.api;

import com.damoyeo.common.auth.CurrentUserProvider;
import com.damoyeo.meeting.dto.MeetingResponse;
import com.damoyeo.meeting.dto.UpdateMeetingRequest;
import com.damoyeo.meeting.dto.UpdateParticipantsRequest;
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

    public MeetingController(CurrentUserProvider currentUserProvider, MeetingService meetingService) {
        this.currentUserProvider = currentUserProvider;
        this.meetingService = meetingService;
    }

    @PostMapping("/groups/{groupId}/meetings")
    public ResponseEntity<MeetingResponse> createDraft(@PathVariable long groupId) {
        MeetingResponse response = meetingService.createDraft(currentUserProvider.getCurrentUserId(), groupId);
        return ResponseEntity.created(URI.create("/api/meetings/" + response.id())).body(response);
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
    public MeetingResponse startPlanning(@PathVariable long meetingId) {
        return meetingService.startPlanning(currentUserProvider.getCurrentUserId(), meetingId);
    }
}
