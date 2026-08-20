package com.damoyeo.meeting.dto;
import java.util.List;
public record MeetingChatResponse(String reply, String meetingContext, List<String> excludedExternalPlaceIds,
                                  List<RevisionChatResponse.UiChangeRequest> uiChangeRequests) {}
