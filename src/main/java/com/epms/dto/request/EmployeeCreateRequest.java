package com.epms.dto.request;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

import com.epms.constant.EmployeeStatus;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EmployeeCreateRequest implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 316881380232135973L;

	@NotBlank(message = "First name is required")
	@Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
	private String firstName;

	@NotBlank(message = "Last name is required")
	@Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
	private String lastName;

	@Email(message = "Invalid email format")
	@NotBlank(message = "Email is required")
	@Size(max = 120, message = "Email must not exceed 120 characters")
	private String email;

	@NotBlank(message = "Phone is required")
	@Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Phone number must contain 10 to 15 digits")
	private String phone;

	@NotBlank(message = "Designation is required")
	@Size(max = 80, message = "Designation must not exceed 80 characters")
	private String designation;

	@NotNull(message = "Base salary is required")
	@Positive(message = "Salary must be greater than zero")
	@Digits(integer = 8, fraction = 2)
	private BigDecimal baseSalary;

	@NotNull(message = "Joining date is required")
	@PastOrPresent(message = "Joining date cannot be in the future")
	private LocalDate joiningDate;

	@NotNull(message = "Employee Status is required")
	private EmployeeStatus employeeStatus;

	@NotNull(message = "Department ID is required")
	@Positive(message = "Department ID must be positive")
	private Long departmentId;

	private Long managerId;

}
