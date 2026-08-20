package com.damoyeo.preference.dto;

import com.damoyeo.preference.domain.PreferenceSentiment;
import com.damoyeo.preference.domain.PreferenceStrength;
import com.damoyeo.preference.domain.PreferenceMappingType;
import com.damoyeo.preference.domain.UserPreference;

public record UserPreferenceResponse(
        Long id,
        String vocabularyCode,
        String displayName,
        String domain,
        String rawValue,
        PreferenceSentiment sentiment,
        PreferenceStrength strength,
        PreferenceMappingType mappingType
) {
    public static UserPreferenceResponse from(UserPreference preference) {
        var vocabulary = preference.getVocabulary();
        return new UserPreferenceResponse(
                preference.getId(),
                vocabulary == null ? null : vocabulary.getCode(),
                vocabulary == null ? null : vocabulary.getDisplayName(),
                vocabulary == null ? null : vocabulary.getDomain(),
                preference.getRawValue(),
                preference.getSentiment(),
                preference.getStrength(),
                preference.getMappingType()
        );
    }
}
