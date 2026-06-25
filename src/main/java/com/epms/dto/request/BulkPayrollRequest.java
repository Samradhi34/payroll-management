package com.epms.dto.request;

import java.io.Serializable;
import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class BulkPayrollRequest implements Serializable {
	private static final long serialVersionUID = 1L;

	@NotNull(message = "Payroll month is required")
	@Min(value = 1, message = "Month must be between 1 and 12")
	@Max(value = 12, message = "Month must be between 1 and 12")
	private Integer month;

	@NotNull(message = "Payroll year is required")
	@Min(value = 2000, message = "Year must be valid")
	private Integer year;

	@PositiveOrZero(message = "Bonus cannot be negative")
	@Digits(integer = 8, fraction = 2)
	private BigDecimal bonus;

	@NotNull(message = "Deductions amount is required")
	@PositiveOrZero(message = "Deductions cannot be negative")
	@Digits(integer = 8, fraction = 2)
	private BigDecimal deductions;
}
