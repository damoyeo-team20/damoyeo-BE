package com.damoyeo.meeting.dto;
import jakarta.validation.constraints.NotBlank;
public record MeetingChatRequest(@NotBlank String message) {}
