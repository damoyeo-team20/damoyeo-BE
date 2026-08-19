package com.damoyeo.oauth.service;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.damoyeo.common.auth.CurrentUserProvider;
import com.damoyeo.common.exception.BusinessException;
import com.damoyeo.user.domain.User;
import com.damoyeo.user.repository.UserRepository;

@Component
@Profile("!header-auth")
public class OAuthCurrentUserProvider implements CurrentUserProvider {

	private final UserRepository userRepository;

	public OAuthCurrentUserProvider(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public long getCurrentUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null
			|| !authentication.isAuthenticated()
			|| !(authentication.getPrincipal() instanceof OidcUser oidcUser)) {
			throw new BusinessException("UNAUTHORIZED", "Google 로그인이 필요합니다.", HttpStatus.UNAUTHORIZED);
		}

		return userRepository.findByGoogleSubject(oidcUser.getSubject())
			.map(User::getId)
			.orElseThrow(() -> new BusinessException(
				"AUTH_USER_NOT_FOUND",
				"로그인 사용자 정보가 저장되어 있지 않습니다.",
				HttpStatus.UNAUTHORIZED
			));
	}
}
