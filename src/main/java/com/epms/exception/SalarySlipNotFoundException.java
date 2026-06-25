package com.epms.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a SalarySlip record cannot be found by the given identifier.
 * Maps to HTTP 404 Not Found via the global exception handler.
 */
public class SalarySlipNotFoundException extends BaseRuntimeException {

	private static final long serialVersionUID = 7210384710293846712L;

	private static final HttpStatus STATUS = HttpStatus.NOT_FOUND;

	/**
	 * @param message human-readable message (typically from messages.properties)
	 */
	public SalarySlipNotFoundException(final String message) {
		super(STATUS, message);
	}

	/**
	 * @param message human-readable message
	 * @param cause   root cause
	 */
	public SalarySlipNotFoundException(final String message, final Throwable cause) {
		super(STATUS, message, cause);
	}

	/**
	 * @param cause root cause
	 */
	public SalarySlipNotFoundException(final Throwable cause) {
		super(STATUS, cause);
	}
}
