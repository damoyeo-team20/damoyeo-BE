package com.damoyeo.user.domain;

import com.damoyeo.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "google_subject", nullable = false, unique = true, length = 255)
    private String googleSubject;

    @Column(nullable = false, unique = true, length = 320)
    private String email;

    @Column(length = 50)
    private String nickname;

    @Column(name = "onboarding_completed", nullable = false)
    private boolean onboardingCompleted;

    protected User() {
    }

    public User(String googleSubject, String email, String nickname) {
        this.googleSubject = googleSubject;
        this.email = email;
        this.nickname = nickname;
        this.onboardingCompleted = false;
    }

    public void updateProfile(String email, String nickname) {
        this.email = email;
        this.nickname = nickname;
    }

    public void completeOnboarding() {
        this.onboardingCompleted = true;
    }

    public Long getId() {
        return id;
    }

    public String getGoogleSubject() {
        return googleSubject;
    }

    public String getEmail() {
        return email;
    }

    public String getNickname() {
        return nickname;
    }

    public boolean isOnboardingCompleted() {
        return onboardingCompleted;
    }
}
