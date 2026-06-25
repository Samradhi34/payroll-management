package com.epms.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an attempt is made to generate a payroll that already exists
 * for the same employee, month, and year.
 * Maps to HTTP 409 Conflict via the global exception handler.
 */
public class PayrollAlreadyExistsException extends BaseRuntimeException {

	private static final long serialVersionUID = 1984023748103984712L;

	private static final HttpStatus STATUS = HttpStatus.CONFLICT;

	/**
	 * @param message human-readable message
	 */
	public PayrollAlreadyExistsException(final String message) {
		super(STATUS, message);
	}

	/**
	 * @param message human-readable message
	 * @param cause   root cause
	 */
	public PayrollAlreadyExistsException(final String message, final Throwable cause) {
		super(STATUS, message, cause);
	}

	/**
	 * @param cause root cause
	 */
	public PayrollAlreadyExistsException(final Throwable cause) {
		super(STATUS, cause);
	}
}
