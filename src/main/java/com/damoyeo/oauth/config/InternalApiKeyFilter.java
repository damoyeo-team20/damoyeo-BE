package com.damoyeo.oauth.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Protects the AI-only /internal contract independently of browser OAuth. */
@Component
public class InternalApiKeyFilter extends OncePerRequestFilter {
    private static final String HEADER_NAME = "X-Internal-Api-Key";
    private final String apiKey;

    public InternalApiKeyFilter(@Value("${INTERNAL_API_KEY:}") String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/internal/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String provided = request.getHeader(HEADER_NAME);
        if (apiKey.isBlank() || provided == null || !MessageDigest.isEqual(
                apiKey.getBytes(StandardCharsets.UTF_8), provided.getBytes(StandardCharsets.UTF_8))) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Internal API authentication failed.");
            return;
        }
        chain.doFilter(request, response);
    }
}
