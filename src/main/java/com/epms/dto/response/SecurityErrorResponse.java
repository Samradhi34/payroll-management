package com.epms.dto.response;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Standardized structure for error messages thrown by the security filters.
 * Having a uniform REST API error format is a crucial production-grade practice.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityErrorResponse implements Serializable {

	private static final long serialVersionUID = 1L;

	private LocalDateTime timestamp;
	private int status;
	private String error;
	private String message;
	private String path;
}
