package com.damoyeo.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;

import com.damoyeo.oauth.api.AuthController;
import com.damoyeo.user.repository.UserRepository;

class AuthControllerTests {

	@Test
	void signedOutSessionDoesNotExposeUserData() {
		OAuth2AuthorizedClientService authorizedClientService = mock(OAuth2AuthorizedClientService.class);
		UserRepository userRepository = mock(UserRepository.class);
		AuthController controller = new AuthController(authorizedClientService, userRepository);

		var response = controller.session(null, null);

		assertThat(response.authenticated()).isFalse();
		assertThat(response.user()).isNull();
		assertThat(response.calendarAuthorized()).isFalse();
		assertThat(response.grantedScopes()).isEmpty();
	}
}
