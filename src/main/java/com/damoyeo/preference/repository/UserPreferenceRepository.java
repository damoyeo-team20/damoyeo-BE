package com.damoyeo.preference.repository;

import com.damoyeo.preference.domain.UserPreference;
import java.util.List;
import java.util.Optional;
import com.damoyeo.preference.dto.UserPreferenceCount;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPreferenceRepository extends JpaRepository<UserPreference, Long> {
    Optional<UserPreference> findByUserIdAndVocabularyCode(Long userId, String vocabularyCode);

    List<UserPreference> findAllByUserIdOrderByIdAsc(Long userId);

    @Query("""
            select new com.damoyeo.preference.dto.UserPreferenceCount(up.user.id, count(up))
            from UserPreference up
            where up.user.id in :userIds
            group by up.user.id
            """)
    List<UserPreferenceCount> countAllByUserIds(@Param("userIds") List<Long> userIds);
}
