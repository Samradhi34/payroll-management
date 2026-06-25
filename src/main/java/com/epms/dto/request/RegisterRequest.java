package com.epms.dto.request;

import java.io.Serializable;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 146010683852130608L;

	@NotBlank(message = "Username is required")
	@Size(min = 2, max = 50, message = "Username must be between 2 and 50 characters")
	private String username;

	@Email(message = "Invalid email format")
	@NotBlank(message = "Email is required")
	private String email;

	@NotBlank(message = "Password is required")
	@Size(min = 6, message = "Password must be at least 6 characters")
	private String password;

}
