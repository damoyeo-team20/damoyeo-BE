package com.damoyeo.preference.dto;

import com.damoyeo.preference.domain.PreferenceSentiment;
import com.damoyeo.preference.domain.PreferenceStrength;
import com.damoyeo.preference.domain.UserPreference;

public record UserPreferenceResponse(
        String vocabularyCode,
        String displayName,
        String domain,
        String rawValue,
        PreferenceSentiment sentiment,
        PreferenceStrength strength
) {
    public static UserPreferenceResponse from(UserPreference preference) {
        return new UserPreferenceResponse(
                preference.getVocabulary().getCode(),
                preference.getVocabulary().getDisplayName(),
                preference.getVocabulary().getDomain(),
                preference.getRawValue(),
                preference.getSentiment(),
                preference.getStrength()
        );
    }
}
