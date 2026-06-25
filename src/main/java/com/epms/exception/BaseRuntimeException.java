package com.epms.exception;

import org.springframework.http.HttpStatus;


public class BaseRuntimeException extends RuntimeException {

	
	private static final long serialVersionUID = -3696641006321904368L;
	
	private final HttpStatus status;

	/**
	 *Default constructor - If someone throws exception without giving status
	 */
	public BaseRuntimeException() {
		status = HttpStatus.INTERNAL_SERVER_ERROR;
	}

	/**
	 * Constructor with custom message + cause
	 * @param message
	 * @param cause
	 */
	public BaseRuntimeException(final HttpStatus status, final String message, final Throwable cause) {
		super(message, cause);
		this.status = status;
	}

	/**
	 * Constructor with only status + message
	 * @param message
	 */
	public BaseRuntimeException(final HttpStatus status, final String message) {
		super(message);
		this.status = status;
	}

	/**
	 * Constructor with status + cause
	 * @param cause
	 */
	public BaseRuntimeException(final HttpStatus status, final Throwable cause) {
		super(cause);
		this.status = status;
	}

	public HttpStatus getStatus() {
		return status;
	}
}
