package com.damoyeo.user.dto;

import com.damoyeo.user.domain.User;
import java.time.Instant;

public record UserResponse(
        Long id,
        String email,
        String nickname,
        boolean onboardingCompleted,
        Instant createdAt,
        Instant updatedAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.isOnboardingCompleted(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
