package com.damoyeo.oauth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import com.damoyeo.user.domain.User;
import com.damoyeo.user.repository.UserRepository;

class GoogleOidcUserServiceTest {

	private UserRepository userRepository;
	private OAuth2UserService<OidcUserRequest, OidcUser> delegate;
	private GoogleOidcUserService userService;
	private OidcUserRequest userRequest;
	private OidcUser oidcUser;

	@BeforeEach
	@SuppressWarnings("unchecked")
	void setUp() {
		userRepository = mock(UserRepository.class);
		delegate = mock(OAuth2UserService.class);
		userService = new GoogleOidcUserService(userRepository, delegate);
		userRequest = mock(OidcUserRequest.class);
		oidcUser = mock(OidcUser.class);

		when(delegate.loadUser(userRequest)).thenReturn(oidcUser);
		when(oidcUser.getSubject()).thenReturn("google-subject");
		when(oidcUser.getEmail()).thenReturn("tester@example.com");
		when(oidcUser.getEmailVerified()).thenReturn(true);
		when(oidcUser.getFullName()).thenReturn("OAuth Tester");
		when(userRepository.findByEmail("tester@example.com")).thenReturn(Optional.empty());
	}

	@Test
	void createsUserOnFirstGoogleLogin() {
		when(userRepository.findByGoogleSubject("google-subject")).thenReturn(Optional.empty());
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		OidcUser result = userService.loadUser(userRequest);

		ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(captor.capture());
		assertThat(result).isSameAs(oidcUser);
		assertThat(captor.getValue().getGoogleSubject()).isEqualTo("google-subject");
		assertThat(captor.getValue().getEmail()).isEqualTo("tester@example.com");
		assertThat(captor.getValue().getNickname()).isEqualTo("OAuth Tester");
	}

	@Test
	void updatesProfileOnReturningGoogleLogin() {
		User existingUser = new User("google-subject", "old@example.com", "Old Name");
		when(userRepository.findByGoogleSubject("google-subject")).thenReturn(Optional.of(existingUser));

		userService.loadUser(userRequest);

		assertThat(existingUser.getEmail()).isEqualTo("tester@example.com");
		assertThat(existingUser.getNickname()).isEqualTo("OAuth Tester");
		verify(userRepository).save(existingUser);
	}

	@Test
	void rejectsEmailAlreadyConnectedToAnotherGoogleSubject() {
		User otherUser = new User("other-google-subject", "tester@example.com", "Other User");
		when(userRepository.findByEmail("tester@example.com")).thenReturn(Optional.of(otherUser));

		assertThatThrownBy(() -> userService.loadUser(userRequest))
			.isInstanceOf(OAuth2AuthenticationException.class)
			.hasMessageContaining("다른 Google 계정");
	}
}
