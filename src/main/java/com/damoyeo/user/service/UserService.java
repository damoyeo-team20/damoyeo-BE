package com.damoyeo.user.service;

import com.damoyeo.common.exception.BusinessException;
import com.damoyeo.user.domain.User;
import com.damoyeo.user.dto.UserResponse;
import com.damoyeo.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse findMe(long userId) {
        return UserResponse.from(requireUser(userId));
    }

    @Transactional
    public UserResponse completeOnboarding(long userId) {
        User user = requireUser(userId);
        user.completeOnboarding();
        return UserResponse.from(user);
    }

    private User requireUser(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
    }
}
