package com.damoyeo.user.api;

import com.damoyeo.common.auth.CurrentUserProvider;
import com.damoyeo.user.dto.UserResponse;
import com.damoyeo.user.service.UserService;
import com.damoyeo.preference.dto.UserPreferenceResponse;
import com.damoyeo.preference.dto.PreferenceChatRequest;
import com.damoyeo.preference.dto.PreferenceChatResponse;
import com.damoyeo.preference.repository.UserPreferenceRepository;
import com.damoyeo.preference.service.UserPreferenceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
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
    private final UserPreferenceService preferenceService;

    public UserController(CurrentUserProvider currentUserProvider, UserService userService, UserPreferenceRepository preferenceRepository, UserPreferenceService preferenceService) {
        this.currentUserProvider = currentUserProvider;
        this.userService = userService;
        this.preferenceRepository = preferenceRepository;
        this.preferenceService = preferenceService;
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

    @PostMapping("/preferences/chat")
    public PreferenceChatResponse chatPreferences(@Valid @RequestBody PreferenceChatRequest request) {
        return preferenceService.chat(currentUserProvider.getCurrentUserId(), request.message());
    }
}
