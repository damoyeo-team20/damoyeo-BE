package com.damoyeo.oauth.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.damoyeo.user.domain.User;
import com.damoyeo.user.repository.UserRepository;

@Service
public class GoogleOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

	private static final int MAX_GOOGLE_SUBJECT_LENGTH = 255;
	private static final int MAX_EMAIL_LENGTH = 320;
	private static final int MAX_NICKNAME_LENGTH = 50;
	private static final int MAX_PROFILE_IMAGE_URL_LENGTH = 2048;

	private final UserRepository userRepository;
	private final OAuth2UserService<OidcUserRequest, OidcUser> delegate;

	@Autowired
	public GoogleOidcUserService(UserRepository userRepository) {
		this(userRepository, new OidcUserService());
	}

	GoogleOidcUserService(
		UserRepository userRepository,
		OAuth2UserService<OidcUserRequest, OidcUser> delegate
	) {
		this.userRepository = userRepository;
		this.delegate = delegate;
	}

	@Override
	@Transactional
	public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
		OidcUser oidcUser = delegate.loadUser(userRequest);
		String googleSubject = requiredValue(oidcUser.getSubject(), "Google 사용자 식별자가 없습니다.");
		String email = requiredValue(oidcUser.getEmail(), "Google 계정 이메일이 없습니다.");

		if (!Boolean.TRUE.equals(oidcUser.getEmailVerified())) {
			throw authenticationException("unverified_email", "인증된 Google 이메일이 필요합니다.");
		}
		if (googleSubject.length() > MAX_GOOGLE_SUBJECT_LENGTH || email.length() > MAX_EMAIL_LENGTH) {
			throw authenticationException("invalid_user_info", "Google 사용자 정보가 허용 길이를 초과했습니다.");
		}

		Optional<User> sameEmailUser = userRepository.findByEmail(email);
		if (sameEmailUser.isPresent()
			&& !sameEmailUser.get().getGoogleSubject().equals(googleSubject)) {
			throw authenticationException(
				"oauth_account_conflict",
				"해당 이메일은 다른 Google 계정에 연결되어 있습니다."
			);
		}

		String nickname = normalizedNickname(oidcUser, email);
		String profileImageUrl = normalizedProfileImageUrl(oidcUser);
		User user = userRepository.findByGoogleSubject(googleSubject)
			.map(existingUser -> {
				existingUser.updateProfile(email, nickname, profileImageUrl);
				return existingUser;
			})
			.orElseGet(() -> new User(googleSubject, email, nickname, profileImageUrl));
		userRepository.save(user);

		return oidcUser;
	}

	private String requiredValue(String value, String message) {
		if (!StringUtils.hasText(value)) {
			throw authenticationException("invalid_user_info", message);
		}
		return value.trim();
	}

	private String normalizedNickname(OidcUser oidcUser, String email) {
		String nickname = StringUtils.hasText(oidcUser.getFullName())
			? oidcUser.getFullName().trim()
			: email.substring(0, email.contains("@") ? email.indexOf('@') : email.length());
		return nickname.substring(0, Math.min(nickname.length(), MAX_NICKNAME_LENGTH));
	}

	private String normalizedProfileImageUrl(OidcUser oidcUser) {
		String picture = oidcUser.getPicture();
		if (!StringUtils.hasText(picture)) {
			return null;
		}
		String normalized = picture.trim();
		return normalized.substring(0, Math.min(normalized.length(), MAX_PROFILE_IMAGE_URL_LENGTH));
	}

	private OAuth2AuthenticationException authenticationException(String code, String message) {
		return new OAuth2AuthenticationException(new OAuth2Error(code, message, null), message);
	}
}
