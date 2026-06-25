package com.epms.config.security;

import java.io.IOException;
import java.time.LocalDateTime;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.epms.dto.response.SecurityErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * CustomAuthenticationEntryPoint handles authentication exceptions (HTTP 401 Unauthorized).
 * By default, Spring Security serves an ugly HTML basic-auth challenge or error page.
 * In a production REST backend, we must return clean JSON payloads describing the failure.
 */
@Slf4j
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
						 AuthenticationException authException) throws IOException, ServletException {
		log.warn("Unauthorized access attempt to: {} - Message: {}", request.getRequestURI(), authException.getMessage());

		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

		SecurityErrorResponse errorResponse = SecurityErrorResponse.builder()
				.timestamp(LocalDateTime.now())
				.status(HttpServletResponse.SC_UNAUTHORIZED)
				.error("Unauthorized")
				.message(authException.getMessage() != null ? authException.getMessage() : "Full authentication is required to access this resource")
				.path(request.getRequestURI())
				.build();

		ObjectMapper mapper = new ObjectMapper();
		// Register JavaTimeModule to handle Java 8 LocalDateTime serialization
		mapper.registerModule(new JavaTimeModule());
		mapper.writeValue(response.getOutputStream(), errorResponse);
	}
}
