package com.epms.config.security;

import java.io.IOException;
import java.time.LocalDateTime;

import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.epms.dto.response.SecurityErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * CustomAccessDeniedHandler handles authorization failures (HTTP 403 Forbidden).
 * This triggers when a user is authenticated but lacks the necessary roles/privileges.
 * In a production REST API, returning JSON is critical for programmatic API clients.
 */
@Slf4j
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
					   AccessDeniedException accessDeniedException) throws IOException, ServletException {
		log.warn("Forbidden access attempt to: {} - Message: {}", request.getRequestURI(), accessDeniedException.getMessage());

		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setStatus(HttpServletResponse.SC_FORBIDDEN);

		SecurityErrorResponse errorResponse = SecurityErrorResponse.builder()
				.timestamp(LocalDateTime.now())
				.status(HttpServletResponse.SC_FORBIDDEN)
				.error("Forbidden")
				.message("Access denied: You do not have permission to access this resource")
				.path(request.getRequestURI())
				.build();

		ObjectMapper mapper = new ObjectMapper();
		// Register JavaTimeModule to handle Java 8 LocalDateTime serialization
		mapper.registerModule(new JavaTimeModule());
		mapper.writeValue(response.getOutputStream(), errorResponse);
	}
}
