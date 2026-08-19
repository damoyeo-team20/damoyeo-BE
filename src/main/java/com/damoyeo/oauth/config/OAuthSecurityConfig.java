package com.damoyeo.oauth.config;

import java.net.URI;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.damoyeo.oauth.service.GoogleOidcUserService;

@Configuration
public class OAuthSecurityConfig {

	private static final String AUTHORIZATION_BASE_URI = "/oauth2/authorization";
	private static final Set<String> SUPPORTED_REDIRECT_SCHEMES = Set.of("http", "https");

	@Bean
	SecurityFilterChain securityFilterChain(
		HttpSecurity http,
		ClientRegistrationRepository clientRegistrationRepository,
		GoogleOidcUserService googleOidcUserService,
		@Value("${spring.security.oauth2.client.registration.google.redirect-uri}") String googleRedirectUri,
		@Value("${app.oauth.login-success-uri}") String loginSuccessUri
	) throws Exception {
		String callbackPath = callbackPath(googleRedirectUri);
		var authorizationRequestResolver = new DefaultOAuth2AuthorizationRequestResolver(
			clientRegistrationRepository,
			AUTHORIZATION_BASE_URI
		);
		authorizationRequestResolver.setAuthorizationRequestCustomizer(builder ->
			builder.additionalParameters(parameters -> {
				parameters.put("access_type", "offline");
				parameters.put("prompt", "consent");
				parameters.put("include_granted_scopes", "true");
			})
		);

		AuthenticationEntryPoint apiAuthenticationEntryPoint = new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);

		return http
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers(
					"/",
					"/error",
					"/actuator/health",
					"/actuator/info",
					"/api/auth/google",
					"/api/auth/session",
					"/api/auth/csrf",
					"/oauth2/**",
					"/login/**",
					callbackPath
				).permitAll()
				.anyRequest().authenticated()
			)
			.oauth2Login(oauth -> oauth
				.authorizationEndpoint(endpoint -> endpoint
					.authorizationRequestResolver(authorizationRequestResolver)
				)
				.userInfoEndpoint(userInfo -> userInfo.oidcUserService(googleOidcUserService))
				.redirectionEndpoint(endpoint -> endpoint.baseUri(callbackPath))
				.defaultSuccessUrl(loginSuccessUri, true)
				.failureUrl("/api/auth/session?oauthError=true")
			)
			.oauth2Client(Customizer.withDefaults())
			.exceptionHandling(exceptions -> exceptions
				.defaultAuthenticationEntryPointFor(
					apiAuthenticationEntryPoint,
					PathPatternRequestMatcher.pathPattern("/api/**")
				)
			)
			.logout(logout -> logout
				.logoutUrl("/api/auth/logout")
				.invalidateHttpSession(true)
				.clearAuthentication(true)
				.deleteCookies("JSESSIONID", "XSRF-TOKEN")
				.logoutSuccessHandler((request, response, authentication) ->
					response.setStatus(HttpStatus.NO_CONTENT.value())
				)
			)
			.csrf(csrf -> csrf
				.csrfTokenRepository(org.springframework.security.web.csrf.CookieCsrfTokenRepository.withHttpOnlyFalse())
			)
			.cors(Customizer.withDefaults())
			.build();
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource(
		@Value("${app.cors.allowed-origin}") String allowedOrigin
	) {
		URI frontendOrigin = validatedOrigin(allowedOrigin);

		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(List.of(frontendOrigin.toString()));
		configuration.setAllowedMethods(List.of(
			HttpMethod.GET.name(),
			HttpMethod.POST.name(),
			HttpMethod.PUT.name(),
			HttpMethod.PATCH.name(),
			HttpMethod.DELETE.name(),
			HttpMethod.OPTIONS.name()
		));
		configuration.setAllowedHeaders(List.of(
			HttpHeaders.ACCEPT,
			HttpHeaders.CONTENT_TYPE,
			"X-XSRF-TOKEN",
			"X-Requested-With"
		));
		configuration.setAllowCredentials(true);
		configuration.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

	private URI validatedOrigin(String origin) {
		URI uri = URI.create(origin);
		if (!SUPPORTED_REDIRECT_SCHEMES.contains(uri.getScheme())
			|| uri.getHost() == null
			|| (uri.getPath() != null && !uri.getPath().isBlank() && !"/".equals(uri.getPath()))) {
			throw new IllegalArgumentException(
				"FRONTEND_ORIGIN은 path가 없는 http 또는 https origin이어야 합니다."
			);
		}
		return URI.create("%s://%s".formatted(uri.getScheme(), uri.getAuthority()));
	}

	private String callbackPath(String redirectUri) {
		return validatedRedirectUri(redirectUri).getPath();
	}

	private URI validatedRedirectUri(String redirectUri) {
		URI uri = URI.create(redirectUri);
		if (!SUPPORTED_REDIRECT_SCHEMES.contains(uri.getScheme())
			|| uri.getHost() == null
			|| uri.getPath() == null
			|| uri.getPath().isBlank()) {
			throw new IllegalArgumentException(
				"GOOGLE_REDIRECT_URI는 http 또는 https scheme과 host, path를 포함한 전체 URI여야 합니다."
			);
		}
		return uri;
	}
}
