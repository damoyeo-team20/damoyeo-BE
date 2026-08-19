package com.damoyeo.oauth;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import com.damoyeo.user.domain.User;
import com.damoyeo.user.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
class OAuthEndpointsIntegrationTests {

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private UserRepository userRepository;

	@BeforeEach
	void clearUsers() {
		userRepository.deleteAll();
	}

	@Test
	void signedOutSessionIsPublic() throws Exception {
		mockMvc.perform(get("/api/auth/session"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.authenticated").value(false))
			.andExpect(jsonPath("$.user").doesNotExist())
			.andExpect(jsonPath("$.calendarAuthorized").value(false));
	}

	@Test
	void googleLoginEndpointStartsAuthorization() throws Exception {
		mockMvc.perform(get("/api/auth/google"))
			.andExpect(status().is3xxRedirection())
			.andExpect(header().string("Location", "/oauth2/authorization/google"));

		mockMvc.perform(get("/oauth2/authorization/google"))
			.andExpect(status().is3xxRedirection())
			.andExpect(header().string("Location", containsString("https://accounts.google.com/o/oauth2/v2/auth")))
			.andExpect(header().string("Location", containsString("access_type=offline")))
			.andExpect(header().string("Location", containsString("prompt=consent")));
	}

	@Test
	void authenticatedSessionReturnsGoogleProfileWithoutTokens() throws Exception {
		User savedUser = userRepository.save(new User("google-user-id", "tester@example.com", "OAuth Tester"));

		mockMvc.perform(get("/api/auth/session").with(oidcLogin()
			.idToken(token -> token
				.subject("google-user-id")
				.claim("email", "tester@example.com")
				.claim("name", "OAuth Tester")
				.claim("picture", "https://example.com/profile.png")
			)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.authenticated").value(true))
			.andExpect(jsonPath("$.user.id").value(savedUser.getId()))
			.andExpect(jsonPath("$.user.googleSubject").value("google-user-id"))
			.andExpect(jsonPath("$.user.email").value("tester@example.com"))
			.andExpect(jsonPath("$.user.nickname").value("OAuth Tester"))
			.andExpect(jsonPath("$.user.onboardingCompleted").value(false))
			.andExpect(jsonPath("$.accessToken").doesNotExist())
			.andExpect(jsonPath("$.refreshToken").doesNotExist());
	}

	@Test
	void authenticatedDomainApiUsesOAuthUserWithoutTemporaryHeader() throws Exception {
		userRepository.save(new User("google-user-id", "tester@example.com", "OAuth Tester"));

		mockMvc.perform(get("/api/groups").with(oidcLogin()
			.idToken(token -> token.subject("google-user-id"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$").isArray());
	}

	@Test
	void protectedApiReturnsUnauthorizedInsteadOfRedirect() throws Exception {
		mockMvc.perform(get("/api/private-test"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void csrfTokenIsPublicAndLogoutRequiresIt() throws Exception {
		mockMvc.perform(get("/api/auth/csrf"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.headerName").isNotEmpty())
			.andExpect(jsonPath("$.token").isNotEmpty());

		mockMvc.perform(post("/api/auth/logout").with(oidcLogin()))
			.andExpect(status().isForbidden());

		mockMvc.perform(post("/api/auth/logout").with(oidcLogin()).with(csrf()))
			.andExpect(status().isNoContent());
	}
}
