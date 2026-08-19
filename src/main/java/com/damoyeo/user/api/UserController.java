package com.damoyeo.user.api;

import com.damoyeo.common.auth.CurrentUserProvider;
import com.damoyeo.user.dto.UserResponse;
import com.damoyeo.user.service.UserService;
import com.damoyeo.preference.dto.UserPreferenceResponse;
import com.damoyeo.preference.repository.UserPreferenceRepository;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me")
public class UserController {

    private final CurrentUserProvider currentUserProvider;
    private final UserService userService;
    private final UserPreferenceRepository preferenceRepository;

    public UserController(CurrentUserProvider currentUserProvider, UserService userService, UserPreferenceRepository preferenceRepository) {
        this.currentUserProvider = currentUserProvider;
        this.userService = userService;
        this.preferenceRepository = preferenceRepository;
    }

    @GetMapping
    public UserResponse findMe() {
        return userService.findMe(currentUserProvider.getCurrentUserId());
    }

    @PostMapping("/onboarding/complete")
    public UserResponse completeOnboarding() {
        return userService.completeOnboarding(currentUserProvider.getCurrentUserId());
    }

    @GetMapping("/preferences")
    public List<UserPreferenceResponse> findPreferences() {
        return preferenceRepository.findAllByUserIdOrderByIdAsc(currentUserProvider.getCurrentUserId()).stream()
                .map(UserPreferenceResponse::from)
                .toList();
    }
}
