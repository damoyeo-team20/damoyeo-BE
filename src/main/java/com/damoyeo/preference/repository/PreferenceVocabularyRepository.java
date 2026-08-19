package com.damoyeo.preference.repository;

import com.damoyeo.preference.domain.PreferenceVocabulary;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PreferenceVocabularyRepository extends JpaRepository<PreferenceVocabulary, Long> {
    Optional<PreferenceVocabulary> findByCode(String code);

    boolean existsByCode(String code);
}
