package com.epms.exception;

import org.springframework.security.access.AccessDeniedException;
import java.util.stream.Collectors;

import org.springframework.security.core.AuthenticationException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.epms.locale.MessageByLocaleService;
import com.epms.response.GenericResponseHandlers;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalHandlerExceptionController {

	private final MessageByLocaleService messageByLocaleService;

	/**
	 * Central exception handler and generate common custom response
	 *
	 * @param request
	 * @param exception
	 * @return
	 */

	@ExceptionHandler(Throwable.class)
	ResponseEntity<?> handleControllerException(final HttpServletRequest request, final Throwable exception) {
		HttpStatus status = null;
		String message = null;

		log.info("*****Global Exception Handler*****");

		log.error("Exception class: {}", exception.getClass().getName());
		log.error("Exception message: {}", exception.getMessage());

		/**
		 * Custom exceptions
		 */
		if (exception instanceof BaseException baseException) {
			status = baseException.getStatus();
			message = baseException.getMessage();
			log.warn("Checked business exception occurred: {}", message);
		}

		else if (exception instanceof BaseRuntimeException baseRuntimeException) {
			status = baseRuntimeException.getStatus();
			message = baseRuntimeException.getMessage();
			log.warn("Business exception occurred: {}", message);
		}
		else if (exception instanceof MethodArgumentNotValidException validationException) {
			status = HttpStatus.BAD_REQUEST;
			message = validationException.getFieldErrors().stream().map(FieldError::getDefaultMessage)
					.collect(Collectors.joining(", "));
			log.warn("Validation failed: {}", message);
		} 
		else if (exception instanceof org.springframework.web.method.annotation.HandlerMethodValidationException validationException) {
			status = HttpStatus.BAD_REQUEST;
			message = validationException.getParameterValidationResults().stream()
					.flatMap(result -> result.getResolvableErrors().stream())
					.map(org.springframework.context.MessageSourceResolvable::getDefaultMessage)
					.collect(Collectors.joining(", "));
			log.warn("Validation failed (method): {}", message);
		}
		else if (exception instanceof org.springframework.data.mapping.PropertyReferenceException propertyException) {
			status = HttpStatus.BAD_REQUEST;
			message = messageByLocaleService.getMessage("sort.param.invalid", new Object[] { propertyException.getPropertyName() });
			log.warn("Invalid sort property: {}", propertyException.getPropertyName());
		}
		else if (exception instanceof MissingServletRequestParameterException
				|| exception instanceof MissingRequestHeaderException
				|| exception instanceof HttpMessageNotReadableException
				|| exception instanceof MethodArgumentTypeMismatchException) {
			status = HttpStatus.BAD_REQUEST;
			message = messageByLocaleService.getMessage("request.param.invalid", null);
		} 
		else if (exception instanceof AccessDeniedException) {
			status = HttpStatus.FORBIDDEN;
			message = "Access denied: You do not have permission to access this resource";
			log.warn("Access denied: {}", exception.getMessage());
		}
		else if (exception instanceof AuthenticationException) {
			status = HttpStatus.UNAUTHORIZED;
			message = messageByLocaleService.getMessage("unauthorized", null);
			log.warn("Authentication failed: {}", exception.getMessage());
		}
		else if (exception instanceof IllegalArgumentException || exception instanceof IllegalStateException) {
			status = HttpStatus.BAD_REQUEST;
			message = exception.getMessage();
			log.warn("Invalid request argument/state: {}", message);
		}
		else if (exception instanceof HttpClientErrorException || exception instanceof HttpServerErrorException) {
			if (exception.getMessage().contains("401")) {
				status = HttpStatus.UNAUTHORIZED;
				message = messageByLocaleService.getMessage("invalid.username.password", null);
			} else {
				status = HttpStatus.INTERNAL_SERVER_ERROR;
				message = exception.getMessage();
			}
		} else {
			status = HttpStatus.INTERNAL_SERVER_ERROR;
			message = messageByLocaleService.getMessage("common.error", null);
			StringBuffer requestedURL = request.getRequestURL();
			log.info("Requested URL:{}", requestedURL);
			log.error("exception : {}", exception);
		}

		return new GenericResponseHandlers.Builder()
				.setStatus(status)
				.setMessage(message)
				.create();
	}

}
