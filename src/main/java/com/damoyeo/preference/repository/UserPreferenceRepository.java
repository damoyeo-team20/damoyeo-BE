package com.damoyeo.preference.repository;

import com.damoyeo.preference.domain.UserPreference;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPreferenceRepository extends JpaRepository<UserPreference, Long> {
    Optional<UserPreference> findByUserIdAndVocabularyCode(Long userId, String vocabularyCode);

    List<UserPreference> findAllByUserIdOrderByIdAsc(Long userId);
}
