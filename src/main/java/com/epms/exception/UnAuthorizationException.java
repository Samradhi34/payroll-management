package com.epms.exception;

import org.springframework.http.HttpStatus;

public class UnAuthorizationException extends BaseException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5393966066293603049L;
	private static final HttpStatus status = HttpStatus.UNAUTHORIZED;

	/**
	 *
	 */
	public UnAuthorizationException() {
	}

	/**
	 * @param status
	 * @param message
	 * @param cause
	 */
	public UnAuthorizationException(final String message, final Throwable cause) {
		super(status, message, cause);
	}

	/**
	 * @param status
	 * @param message
	 */
	public UnAuthorizationException(final String message) {
		super(status, message);
	}

	/**
	 * @param status
	 * @param cause
	 */
	public UnAuthorizationException(final Throwable cause) {
		super(status, cause);
	}

}
