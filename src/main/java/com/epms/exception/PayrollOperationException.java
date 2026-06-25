package com.epms.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an invalid payroll status transition is attempted,
 * e.g. trying to approve a PAID payroll, or cancel a PAID payroll.
 * Maps to HTTP 422 Unprocessable Entity via the global exception handler.
 */
public class PayrollOperationException extends BaseRuntimeException {

	private static final long serialVersionUID = 9023749012840192834L;

	private static final HttpStatus STATUS = HttpStatus.UNPROCESSABLE_ENTITY;

	/**
	 * @param message human-readable message
	 */
	public PayrollOperationException(final String message) {
		super(STATUS, message);
	}

	/**
	 * @param message human-readable message
	 * @param cause   root cause
	 */
	public PayrollOperationException(final String message, final Throwable cause) {
		super(STATUS, message, cause);
	}

	/**
	 * @param cause root cause
	 */
	public PayrollOperationException(final Throwable cause) {
		super(STATUS, cause);
	}
}
