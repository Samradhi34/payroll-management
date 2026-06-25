package com.epms.dto.request;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

import com.epms.constant.AttendanceStatus;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class AttendanceSearchRequest implements Serializable {

	private static final long serialVersionUID = 1L;

	@Positive(message = "Employee ID must be positive")
	private Long employeeId;

	private String search; // Partial search on Employee first/last name

	private AttendanceStatus status;

	private LocalDate dateFrom;
	private LocalDate dateTo;

	@DecimalMin(value = "0.0", inclusive = true)
	@DecimalMax(value = "24.0", inclusive = true)
	private BigDecimal minWorkingHours;

	@DecimalMin(value = "0.0", inclusive = true)
	@DecimalMax(value = "24.0", inclusive = true)
	private BigDecimal maxWorkingHours;
}
