package com.damoyeo.meeting.dto;

import jakarta.validation.constraints.NotNull;

public record ConfirmMeetingRequest(@NotNull Long suggestionId) {}
