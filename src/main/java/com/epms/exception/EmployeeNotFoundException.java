package com.epms.exception;

import org.springframework.http.HttpStatus;

public class EmployeeNotFoundException extends BaseRuntimeException{

	private static final long serialVersionUID = 5862984011481240167L;

	private static final HttpStatus status = HttpStatus.NOT_FOUND;

	/**
	 * @param status
	 * @param message
	 * @param cause
	 */
	public EmployeeNotFoundException(final String message, final Throwable cause) {
		super(status, message, cause);
	}

	/**
	 * @param status
	 * @param message
	 */
	public EmployeeNotFoundException(final String message) {
		super(status, message);
	}

	/**
	 * @param status
	 * @param cause
	 */
	public EmployeeNotFoundException(final Throwable cause) {
		super(status, cause);
	}
	
    
}