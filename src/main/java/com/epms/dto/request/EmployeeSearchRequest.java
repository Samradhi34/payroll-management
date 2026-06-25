package com.epms.dto.request;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

import com.epms.constant.EmployeeStatus;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EmployeeSearchRequest implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7933641774049919894L;

	@Size(max = 50, message = "Search text too long")
	private String search;

	private EmployeeStatus status;

	@Positive(message = "Department ID must be positive")
	private Long departmentId;

	@Digits(integer = 8, fraction = 2)
	@Positive(message = "Min salary must be positive")
	private BigDecimal minSalary;

	@Digits(integer = 8, fraction = 2)
	@Positive(message = "Max salary must be positive")
	private BigDecimal maxSalary;

	private LocalDate joiningDateFrom;
	private LocalDate joiningDateTo;

}
