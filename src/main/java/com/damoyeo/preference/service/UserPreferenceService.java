package com.damoyeo.preference.service;

import com.damoyeo.ai.AiClient;
import com.damoyeo.common.exception.BusinessException;
import com.damoyeo.preference.domain.PreferenceVocabulary;
import com.damoyeo.preference.domain.UserPreference;
import com.damoyeo.preference.repository.PreferenceVocabularyRepository;
import com.damoyeo.preference.repository.UserPreferenceRepository;
import com.damoyeo.preference.dto.PreferenceChatResponse;
import com.damoyeo.preference.dto.UserPreferenceResponse;
import com.damoyeo.preference.domain.PreferenceMappingType;
import com.damoyeo.preference.domain.PreferenceSentiment;
import com.damoyeo.preference.domain.PreferenceStrength;
import com.damoyeo.user.domain.User;
import com.damoyeo.user.repository.UserRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserPreferenceService {

    private static final Logger log = LoggerFactory.getLogger(UserPreferenceService.class);

    private final UserRepository userRepository;
    private final PreferenceVocabularyRepository vocabularyRepository;
    private final UserPreferenceRepository preferenceRepository;
    private final AiClient aiClient;

    public UserPreferenceService(
            UserRepository userRepository,
            PreferenceVocabularyRepository vocabularyRepository,
            UserPreferenceRepository preferenceRepository,
            AiClient aiClient
    ) {
        this.userRepository = userRepository;
        this.vocabularyRepository = vocabularyRepository;
        this.preferenceRepository = preferenceRepository;
        this.aiClient = aiClient;
    }

    @Transactional
    public UserPreference upsert(long userId, PreferenceUpsertCommand command) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        if (command.mappingType() == PreferenceMappingType.UNMAPPED) {
            return preferenceRepository.save(new UserPreference(
                    user,
                    null,
                    command.rawValue(),
                    command.sentiment(),
                    command.strength(),
                    command.mappingType(),
                    command.sourceText()
            ));
        }
        PreferenceVocabulary vocabulary = vocabularyRepository.findByCode(command.vocabularyCode())
                .orElseThrow(() -> new BusinessException(
                        "VOCABULARY_NOT_FOUND",
                        "등록되지 않은 선호 코드입니다.",
                        HttpStatus.BAD_REQUEST
                ));

        UserPreference preference = preferenceRepository
                .findByUserIdAndVocabularyCode(userId, command.vocabularyCode())
                .orElseGet(() -> new UserPreference(
                        user,
                        vocabulary,
                        command.rawValue(),
                        command.sentiment(),
                        command.strength(),
                        command.mappingType(),
                        command.sourceText()
                ));

        preference.update(
                command.rawValue(),
                command.sentiment(),
                command.strength(),
                command.mappingType(),
                command.sourceText()
        );
        return preferenceRepository.save(preference);
    }

    public List<UserPreference> findAll(long userId) {
        if (!userRepository.existsById(userId)) {
            throw new BusinessException("USER_NOT_FOUND", "사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
        }
        return preferenceRepository.findAllByUserIdOrderByIdAsc(userId);
    }

    @Transactional
    public void delete(long userId, long preferenceId) {
        UserPreference preference = preferenceRepository.findById(preferenceId)
                .orElseThrow(() -> new BusinessException("PREFERENCE_NOT_FOUND", "선호를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        if (!preference.getUser().getId().equals(userId)) {
            throw new BusinessException("PREFERENCE_NOT_FOUND", "선호를 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
        }
        preferenceRepository.delete(preference);
    }

    @Transactional
    public PreferenceChatResponse chat(long userId, String message) {
        String sourceText = message.trim();
        AiClient.PreferenceExtractResponse response = aiClient.extractPreferences(List.of(sourceText));
        if (response.reply() == null || response.reply().isBlank() || response.extractedPreferences() == null) {
            throw invalidAiResponse();
        }
        log.info("AI preference extraction: userId={}, extractedCount={}, mappings={}",
                userId,
                response.extractedPreferences().size(),
                response.extractedPreferences().stream()
                        .map(value -> value.mappingType() + ":" + value.vocabularyCode())
                        .toList());
        for (AiClient.ExtractedPreference extracted : response.extractedPreferences()) {
            validateExtractedPreference(extracted);
            upsert(userId, new PreferenceUpsertCommand(
                    extracted.vocabularyCode(), extracted.rawValue(),
                    PreferenceSentiment.valueOf(extracted.sentiment()),
                    PreferenceStrength.valueOf(extracted.strength()),
                    PreferenceMappingType.valueOf(extracted.mappingType()), sourceText
            ));
        }
        return new PreferenceChatResponse(response.reply(), findAll(userId).stream().map(UserPreferenceResponse::from).toList());
    }

    private void validateExtractedPreference(AiClient.ExtractedPreference extracted) {
        try {
            if (extracted.rawValue() == null || extracted.rawValue().isBlank() || extracted.rawValue().length() > 255) {
                throw new IllegalArgumentException();
            }
            PreferenceSentiment.valueOf(extracted.sentiment());
            PreferenceStrength.valueOf(extracted.strength());
            PreferenceMappingType mappingType = PreferenceMappingType.valueOf(extracted.mappingType());
            if (mappingType == PreferenceMappingType.UNMAPPED && extracted.vocabularyCode() != null) {
                throw new IllegalArgumentException();
            }
            if (mappingType != PreferenceMappingType.UNMAPPED && extracted.vocabularyCode() == null) {
                throw new IllegalArgumentException();
            }
        } catch (RuntimeException exception) {
            throw invalidAiResponse();
        }
    }

    private BusinessException invalidAiResponse() {
        return new BusinessException("AI_RESPONSE_INVALID", "AI 응답 형식이 올바르지 않습니다.", HttpStatus.BAD_GATEWAY);
    }
}
