package com.damoyeo.preference.service;

import com.damoyeo.preference.domain.PreferenceMappingType;
import com.damoyeo.preference.domain.PreferenceSentiment;
import com.damoyeo.preference.domain.PreferenceStrength;

public record PreferenceUpsertCommand(
        String vocabularyCode,
        String rawValue,
        PreferenceSentiment sentiment,
        PreferenceStrength strength,
        PreferenceMappingType mappingType,
        String sourceText
) {
}
