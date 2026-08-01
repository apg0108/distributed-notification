package com.sagant.distributednotification.config.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sagant.distributednotification.config.property.ApiKeyProperties;
import com.sagant.distributednotification.domain.model.ErrorResponse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ApiKeyAuthFilter extends OncePerRequestFilter {

   public static final String API_KEY_HEADER = "X-API-KEY";

   private final ApiKeyProperties apiKeyProperties;

   private final ObjectMapper objectMapper;

   @Override
   protected boolean shouldNotFilter(final HttpServletRequest request) {
      final String pathWithinApplication = request.getRequestURI().substring(request.getContextPath().length());
      return pathWithinApplication.startsWith("/actuator") || pathWithinApplication.startsWith("/swagger-ui")
            || pathWithinApplication.startsWith("/v3/api-docs");
   }

   @Override
   protected void doFilterInternal(final HttpServletRequest request, @NonNull final HttpServletResponse response,
         @NonNull final FilterChain filterChain) throws ServletException, IOException {
      if (!isValid(request.getHeader(API_KEY_HEADER))) {
         writeUnauthorized(response);
         return;
      }

      SecurityContextHolder
            .getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken("api-client", null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
      filterChain.doFilter(request, response);
   }

   private boolean isValid(final String providedKey) {
      final String expectedKey = apiKeyProperties.apiKey();
      if (expectedKey == null || expectedKey.isBlank() || providedKey == null) {
         return false;
      }
      return MessageDigest.isEqual(providedKey.getBytes(StandardCharsets.UTF_8), expectedKey.getBytes(StandardCharsets.UTF_8));
   }

   private void writeUnauthorized(final HttpServletResponse response) throws IOException {
      response.setStatus(HttpStatus.UNAUTHORIZED.value());
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      final ErrorResponse body = ErrorResponse.of(HttpStatus.UNAUTHORIZED.value(), "Unauthorized", "Missing or invalid API key");
      response.getWriter().write(objectMapper.writeValueAsString(body));
   }
}
