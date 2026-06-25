package com.epms.dto.request;

import java.io.Serializable;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -3226395987925588876L;

	@NotBlank(message = "Username is required")
	private String username;

	@NotBlank(message = "Password is required")
	private String password;

}
