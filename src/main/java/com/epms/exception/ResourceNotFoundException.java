package com.epms.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends BaseRuntimeException {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1078459876172624345L;
	private static final HttpStatus status = HttpStatus.NOT_FOUND;

	/**
	 *
	 */
	public ResourceNotFoundException() {
	}

	/**
	 * @param status
	 * @param message
	 * @param cause
	 */
	public ResourceNotFoundException(final String message, final Throwable cause) {
		super(status, message, cause);
	}

	/**
	 * @param status
	 * @param message
	 */
	public ResourceNotFoundException(final String message) {
		super(status, message);
	}

	/**
	 * @param status
	 * @param cause
	 */
	public ResourceNotFoundException(final Throwable cause) {
		super(status, cause);
	}

}
