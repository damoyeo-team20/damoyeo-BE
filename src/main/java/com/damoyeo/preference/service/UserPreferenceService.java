package com.damoyeo.preference.service;

import com.damoyeo.common.exception.BusinessException;
import com.damoyeo.preference.domain.PreferenceVocabulary;
import com.damoyeo.preference.domain.UserPreference;
import com.damoyeo.preference.repository.PreferenceVocabularyRepository;
import com.damoyeo.preference.repository.UserPreferenceRepository;
import com.damoyeo.user.domain.User;
import com.damoyeo.user.repository.UserRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserPreferenceService {

    private final UserRepository userRepository;
    private final PreferenceVocabularyRepository vocabularyRepository;
    private final UserPreferenceRepository preferenceRepository;

    public UserPreferenceService(
            UserRepository userRepository,
            PreferenceVocabularyRepository vocabularyRepository,
            UserPreferenceRepository preferenceRepository
    ) {
        this.userRepository = userRepository;
        this.vocabularyRepository = vocabularyRepository;
        this.preferenceRepository = preferenceRepository;
    }

    @Transactional
    public UserPreference upsert(long userId, PreferenceUpsertCommand command) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
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
}
