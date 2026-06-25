package com.epms.exception;

import org.springframework.http.HttpStatus;

public class FileOperationException extends BaseException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7014198876950037786L;
	private static final HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

	/**
	 *
	 */
	public FileOperationException() {
	}

	/**
	 * @param status
	 * @param message
	 * @param cause
	 */
	public FileOperationException(final String message, final Throwable cause) {
		super(status, message, cause);
	}

	/**
	 * @param status
	 * @param message
	 */
	public FileOperationException(final String message) {
		super(status, message);
	}

	/**
	 * @param status
	 * @param cause
	 */
	public FileOperationException(final Throwable cause) {
		super(status, cause);
	}

}
