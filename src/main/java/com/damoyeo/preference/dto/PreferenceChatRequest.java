package com.damoyeo.preference.dto;

import jakarta.validation.constraints.NotBlank;

public record PreferenceChatRequest(@NotBlank(message = "message는 비어 있을 수 없습니다.") String message) {}
