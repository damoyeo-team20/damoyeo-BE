package com.damoyeo.meeting.dto;

import java.util.List;

public record RevisionChatResponse(String reply, String draftPurpose, List<String> excludedExternalPlaceIds,
                                   List<UiChangeRequest> uiChangeRequests) {
    public record UiChangeRequest(String field, String mentionedValue, String question) {}
}
