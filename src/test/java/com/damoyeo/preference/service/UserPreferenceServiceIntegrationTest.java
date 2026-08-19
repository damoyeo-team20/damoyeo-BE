package com.damoyeo.preference.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.damoyeo.preference.domain.PreferenceMappingType;
import com.damoyeo.preference.domain.PreferenceSentiment;
import com.damoyeo.preference.domain.PreferenceStrength;
import com.damoyeo.preference.domain.PreferenceVocabulary;
import com.damoyeo.preference.repository.PreferenceVocabularyRepository;
import com.damoyeo.preference.repository.UserPreferenceRepository;
import com.damoyeo.user.domain.User;
import com.damoyeo.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class UserPreferenceServiceIntegrationTest {

    @Autowired
    private UserPreferenceService preferenceService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PreferenceVocabularyRepository vocabularyRepository;

    @Autowired
    private UserPreferenceRepository preferenceRepository;

    @Test
    void upsertsByUserAndVocabularyCode() {
        User user = userRepository.save(new User("preference-user", "preference@example.com", "선호"));
        PreferenceVocabulary meat = vocabularyRepository.save(
                new PreferenceVocabulary("MEAT", "FOOD", null, "고기")
        );
        vocabularyRepository.save(new PreferenceVocabulary("LAMB", "FOOD", meat, "양고기"));

        preferenceService.upsert(user.getId(), new PreferenceUpsertCommand(
                "MEAT",
                "양고기",
                PreferenceSentiment.POSITIVE,
                PreferenceStrength.STRONG,
                PreferenceMappingType.GENERALIZED,
                "나는 양고기를 정말 좋아해"
        ));
        preferenceService.upsert(user.getId(), new PreferenceUpsertCommand(
                "MEAT",
                "고기",
                PreferenceSentiment.NEGATIVE,
                PreferenceStrength.MODERATE,
                PreferenceMappingType.EXACT,
                "요즘은 고기가 별로야"
        ));

        var preferences = preferenceRepository.findAllByUserIdOrderByIdAsc(user.getId());
        assertThat(preferences).hasSize(1);
        assertThat(preferences.getFirst().getRawValue()).isEqualTo("고기");
        assertThat(preferences.getFirst().getSentiment()).isEqualTo(PreferenceSentiment.NEGATIVE);
        assertThat(preferences.getFirst().getSourceText()).isEqualTo("요즘은 고기가 별로야");
        assertThat(vocabularyRepository.findByCode("LAMB").orElseThrow().getParent().getCode()).isEqualTo("MEAT");
    }
}
