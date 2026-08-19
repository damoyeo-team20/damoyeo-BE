package com.damoyeo.preference.domain;

import com.damoyeo.common.domain.BaseEntity;
import com.damoyeo.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "user_preferences",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_preferences_user_vocabulary",
                columnNames = {"user_id", "vocabulary_code"}
        )
)
public class UserPreference extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vocabulary_code", referencedColumnName = "code", nullable = false)
    private PreferenceVocabulary vocabulary;

    @Column(name = "raw_value", nullable = false, length = 255)
    private String rawValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PreferenceSentiment sentiment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PreferenceStrength strength;

    @Enumerated(EnumType.STRING)
    @Column(name = "mapping_type", nullable = false, length = 20)
    private PreferenceMappingType mappingType;

    @Column(name = "source_text", nullable = false, columnDefinition = "TEXT")
    private String sourceText;

    protected UserPreference() {
    }

    public UserPreference(
            User user,
            PreferenceVocabulary vocabulary,
            String rawValue,
            PreferenceSentiment sentiment,
            PreferenceStrength strength,
            PreferenceMappingType mappingType,
            String sourceText
    ) {
        this.user = user;
        this.vocabulary = vocabulary;
        update(rawValue, sentiment, strength, mappingType, sourceText);
    }

    public void update(
            String rawValue,
            PreferenceSentiment sentiment,
            PreferenceStrength strength,
            PreferenceMappingType mappingType,
            String sourceText
    ) {
        this.rawValue = rawValue;
        this.sentiment = sentiment;
        this.strength = strength;
        this.mappingType = mappingType;
        this.sourceText = sourceText;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public PreferenceVocabulary getVocabulary() {
        return vocabulary;
    }

    public String getRawValue() {
        return rawValue;
    }

    public PreferenceSentiment getSentiment() {
        return sentiment;
    }

    public PreferenceStrength getStrength() {
        return strength;
    }

    public PreferenceMappingType getMappingType() {
        return mappingType;
    }

    public String getSourceText() {
        return sourceText;
    }
}
