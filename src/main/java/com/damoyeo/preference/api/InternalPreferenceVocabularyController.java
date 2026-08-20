package com.damoyeo.preference.api;

import com.damoyeo.preference.repository.PreferenceVocabularyRepository;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** AI service only. Internal authentication will be applied when the shared scheme is decided. */
@RestController
@RequestMapping("/internal")
public class InternalPreferenceVocabularyController {
    private final PreferenceVocabularyRepository vocabularyRepository;

    public InternalPreferenceVocabularyController(PreferenceVocabularyRepository vocabularyRepository) {
        this.vocabularyRepository = vocabularyRepository;
    }

    @GetMapping("/preference-vocabulary")
    public VocabularyResponse findAll() {
        List<VocabularyEntry> vocabulary = vocabularyRepository.findAll().stream()
                .map(value -> new VocabularyEntry(value.getCode(), value.getDomain(), value.getDisplayName(),
                        value.getParent() == null ? null : value.getParent().getCode()))
                .toList();
        return new VocabularyResponse(vocabulary);
    }

    public record VocabularyResponse(List<VocabularyEntry> vocabulary) {}
    public record VocabularyEntry(String code, String domain, String displayName, String parentCode) {}
}
