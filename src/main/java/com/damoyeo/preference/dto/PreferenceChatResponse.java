package com.damoyeo.preference.dto;

import java.util.List;

public record PreferenceChatResponse(String reply, List<UserPreferenceResponse> preferences) {}
