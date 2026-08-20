package com.damoyeo.preference.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.damoyeo.ai.AiClient;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    @MockitoBean
    private AiClient aiClient;

    @Test
    void storesPreferencesReturnedFromOnboardingChat() {
        User user = userRepository.save(new User("chat-user", "chat@example.com", "온보딩"));
        vocabularyRepository.save(new PreferenceVocabulary("BEEF", "FOOD", null, "소고기"));
        when(aiClient.extractPreferences(List.of("나는 소고기가 좋아"))).thenReturn(
                new AiClient.PreferenceExtractResponse(
                        "말씀해주신 내용을 선호에 반영했어요.",
                        List.of(new AiClient.ExtractedPreference(
                                "BEEF", "소고기", "FOOD", "소고기",
                                "POSITIVE", "MODERATE", "EXACT"
                        ))
                )
        );

        var response = preferenceService.chat(user.getId(), "나는 소고기가 좋아");

        assertThat(response.preferences()).hasSize(1);
        assertThat(response.preferences().getFirst().vocabularyCode()).isEqualTo("BEEF");
        assertThat(preferenceRepository.findAllByUserIdOrderByIdAsc(user.getId())).hasSize(1);
    }

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

    @Test
    void preservesUnmappedPreferencesWithoutVocabulary() {
        User user = userRepository.save(new User("unmapped-user", "unmapped@example.com", "미분류"));

        preferenceService.upsert(user.getId(), new PreferenceUpsertCommand(
                null,
                "루프탑에서 노을 보기",
                PreferenceSentiment.POSITIVE,
                PreferenceStrength.MODERATE,
                PreferenceMappingType.UNMAPPED,
                "나는 루프탑에서 노을 보는 게 좋아"
        ));
        preferenceService.upsert(user.getId(), new PreferenceUpsertCommand(
                null,
                "강아지 동반",
                PreferenceSentiment.POSITIVE,
                PreferenceStrength.STRONG,
                PreferenceMappingType.UNMAPPED,
                "강아지를 데려갈 수 있으면 정말 좋아"
        ));

        var preferences = preferenceRepository.findAllByUserIdOrderByIdAsc(user.getId());
        assertThat(preferences).hasSize(2);
        assertThat(preferences).allSatisfy(preference -> {
            assertThat(preference.getVocabulary()).isNull();
            assertThat(preference.getMappingType()).isEqualTo(PreferenceMappingType.UNMAPPED);
        });
    }
}
