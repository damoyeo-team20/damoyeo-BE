package com.damoyeo.preference.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "preference_vocabulary")
public class PreferenceVocabulary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @Column(nullable = false, length = 50)
    private String domain;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_code", referencedColumnName = "code")
    private PreferenceVocabulary parent;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    protected PreferenceVocabulary() {
    }

    public PreferenceVocabulary(
            String code,
            String domain,
            PreferenceVocabulary parent,
            String displayName
    ) {
        this.code = code;
        this.domain = domain;
        this.parent = parent;
        this.displayName = displayName;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getDomain() {
        return domain;
    }

    public PreferenceVocabulary getParent() {
        return parent;
    }

    public String getDisplayName() {
        return displayName;
    }
}
