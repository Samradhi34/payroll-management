package com.epms.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a Payroll record cannot be found by the given identifier.
 * Maps to HTTP 404 Not Found via the global exception handler.
 */
public class PayrollNotFoundException extends BaseRuntimeException {

	private static final long serialVersionUID = 3912748029152341987L;

	private static final HttpStatus STATUS = HttpStatus.NOT_FOUND;

	/**
	 * @param message human-readable message (typically from messages.properties)
	 */
	public PayrollNotFoundException(final String message) {
		super(STATUS, message);
	}

	/**
	 * @param message human-readable message
	 * @param cause   root cause
	 */
	public PayrollNotFoundException(final String message, final Throwable cause) {
		super(STATUS, message, cause);
	}

	/**
	 * @param cause root cause
	 */
	public PayrollNotFoundException(final Throwable cause) {
		super(STATUS, cause);
	}
}
