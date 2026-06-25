package com.epms.exception;

import org.springframework.http.HttpStatus;

public class ValidationException extends BaseRuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3404298376709145702L;
	private static final HttpStatus status = HttpStatus.BAD_REQUEST;

	/**
	 *
	 */
	public ValidationException() {
	}

	/**
	 * @param status
	 * @param message
	 * @param cause
	 */
	public ValidationException(final String message, final Throwable cause) {
		super(status, message, cause);
	}

	/**
	 * @param status
	 * @param message
	 */
	public ValidationException(final String message) {
		super(status, message);
	}

	/**
	 * @param status
	 * @param cause
	 */
	public ValidationException(final Throwable cause) {
		super(status, cause);
	}

}
