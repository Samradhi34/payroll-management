package com.epms.dto.request;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

import com.epms.constant.EmployeeStatus;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EmployeeUpdateRequest implements Serializable {

	private static final long serialVersionUID = 6432606009706375291L;

	@Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
	private String firstName;

	@Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
	private String lastName;

	@Email(message = "Invalid email format")
	@Size(max = 120)
	private String email;

	@Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Phone number must contain 10 to 15 digits")
	private String phone;

	@Size(max = 80, message = "Designation must not exceed 80 characters")
	private String designation;

	@Positive(message = "Salary must be greater than zero")
	@Digits(integer = 8, fraction = 2)
	private BigDecimal baseSalary;

	private Long managerId;
	private Long departmentId;
	private EmployeeStatus employeeStatus;
	private LocalDate resignationDate;
}
