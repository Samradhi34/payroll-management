package com.epms.dto.request;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.epms.constant.PayrollStatus;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class PayrollSearchRequest implements Serializable {

	private static final long serialVersionUID = 1L;

	private String search; // Partial search on Employee first/last name

	@Positive(message = "Employee ID must be positive")
	private Long employeeId;

	@Min(value = 1, message = "Month must be between 1 and 12")
	@Max(value = 12, message = "Month must be between 1 and 12")
	private Integer month;

	@Min(value = 2000, message = "Year must be 2000 or later")
	private Integer year;

	private PayrollStatus status;

	@Positive(message = "Min net salary must be positive")
	private BigDecimal minNetSalary;

	@Positive(message = "Max net salary must be positive")
	private BigDecimal maxNetSalary;

	private LocalDateTime generatedDateFrom;
	private LocalDateTime generatedDateTo;
}
