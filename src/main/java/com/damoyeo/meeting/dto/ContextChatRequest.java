package com.damoyeo.meeting.dto;

import jakarta.validation.constraints.NotBlank;

public record ContextChatRequest(@NotBlank(message = "message는 비어 있을 수 없습니다.") String message) {}
