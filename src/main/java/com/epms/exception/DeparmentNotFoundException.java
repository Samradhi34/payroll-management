package com.epms.exception;

import org.springframework.http.HttpStatus;

public class DeparmentNotFoundException extends BaseRuntimeException{

	private static final long serialVersionUID = 5862984011481240167L;

	private static final HttpStatus status = HttpStatus.NOT_FOUND;

	/**
	 * @param status
	 * @param message
	 * @param cause
	 */
	public DeparmentNotFoundException(final String message, final Throwable cause) {
		super(status, message, cause);
	}

	/**
	 * @param status
	 * @param message
	 */
	public DeparmentNotFoundException(final String message) {
		super(status, message);
	}

	/**
	 * @param status
	 * @param cause
	 */
	public DeparmentNotFoundException(final Throwable cause) {
		super(status, cause);
	}
	
    
}