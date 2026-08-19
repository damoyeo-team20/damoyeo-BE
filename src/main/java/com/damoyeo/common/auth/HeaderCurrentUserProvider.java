package com.damoyeo.common.auth;

import com.damoyeo.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class HeaderCurrentUserProvider implements CurrentUserProvider {

    public static final String USER_ID_HEADER = "X-User-Id";

    private final HttpServletRequest request;

    public HeaderCurrentUserProvider(HttpServletRequest request) {
        this.request = request;
    }

    @Override
    public long getCurrentUserId() {
        String value = request.getHeader(USER_ID_HEADER);
        if (value == null || value.isBlank()) {
            throw new BusinessException("UNAUTHORIZED", "X-User-Id 헤더가 필요합니다.", HttpStatus.UNAUTHORIZED);
        }
        try {
            long userId = Long.parseLong(value);
            if (userId <= 0) {
                throw new NumberFormatException();
            }
            return userId;
        } catch (NumberFormatException exception) {
            throw new BusinessException("INVALID_USER_ID", "X-User-Id는 양의 정수여야 합니다.", HttpStatus.BAD_REQUEST);
        }
    }
}
