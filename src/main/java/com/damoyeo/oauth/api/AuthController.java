package com.damoyeo.oauth.api;

import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import com.damoyeo.common.exception.BusinessException;
import com.damoyeo.user.domain.User;
import com.damoyeo.user.repository.UserRepository;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private static final String GOOGLE_REGISTRATION_ID = "google";
	private static final String CALENDAR_EVENTS_SCOPE = "https://www.googleapis.com/auth/calendar.events";
	private static final String CALENDAR_FREE_BUSY_SCOPE = "https://www.googleapis.com/auth/calendar.freebusy";

	private final OAuth2AuthorizedClientService authorizedClientService;
	private final UserRepository userRepository;

	public AuthController(
		OAuth2AuthorizedClientService authorizedClientService,
		UserRepository userRepository
	) {
		this.authorizedClientService = authorizedClientService;
		this.userRepository = userRepository;
	}

	@GetMapping("/google")
	public RedirectView googleLogin() {
		return new RedirectView("/oauth2/authorization/google");
	}

	@GetMapping("/session")
	public AuthSessionResponse session(
		Authentication authentication,
		@AuthenticationPrincipal OidcUser user
	) {
		if (authentication == null || user == null || !authentication.isAuthenticated()) {
			return AuthSessionResponse.signedOut();
		}

		OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(
			GOOGLE_REGISTRATION_ID,
			authentication.getName()
		);
		Set<String> scopes = authorizedClient == null
			? Set.of()
			: authorizedClient.getAccessToken().getScopes();
		User savedUser = userRepository.findByGoogleSubject(user.getSubject())
			.orElseThrow(() -> new BusinessException(
				"AUTH_USER_NOT_FOUND",
				"로그인 사용자 정보가 저장되어 있지 않습니다.",
				HttpStatus.UNAUTHORIZED
			));

		return new AuthSessionResponse(
			true,
			new GoogleUserResponse(
				savedUser.getId(),
				savedUser.getGoogleSubject(),
				savedUser.getEmail(),
				savedUser.getNickname(),
				savedUser.getProfileImageUrl() != null
					? savedUser.getProfileImageUrl()
					: user.getClaimAsString("picture"),
				savedUser.isOnboardingCompleted()
			),
			scopes.contains(CALENDAR_EVENTS_SCOPE) && scopes.contains(CALENDAR_FREE_BUSY_SCOPE),
			scopes
		);
	}

	@GetMapping("/csrf")
	public CsrfResponse csrf(CsrfToken csrfToken) {
		return new CsrfResponse(
			csrfToken.getHeaderName(),
			csrfToken.getParameterName(),
			csrfToken.getToken()
		);
	}

	public record AuthSessionResponse(
		boolean authenticated,
		GoogleUserResponse user,
		boolean calendarAuthorized,
		Set<String> grantedScopes
	) {
		static AuthSessionResponse signedOut() {
			return new AuthSessionResponse(false, null, false, Set.of());
		}
	}

	public record GoogleUserResponse(
		long id,
		String googleSubject,
		String email,
		String nickname,
		String picture,
		boolean onboardingCompleted
	) {
	}

	public record CsrfResponse(
		String headerName,
		String parameterName,
		String token
	) {
	}
}
