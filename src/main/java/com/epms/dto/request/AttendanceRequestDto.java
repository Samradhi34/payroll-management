package com.epms.dto.request;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import com.epms.constant.AttendanceStatus;
import com.epms.constant.ApprovalStatus;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AttendanceRequestDto implements Serializable {

	private static final long serialVersionUID = 1L;

	private Long employeeId;

	@NotNull(message = "{attendance.date.required}")
	private LocalDate attendanceDate;

	private AttendanceStatus attendanceStatus;

	@Digits(integer = 2, fraction = 2, message = "{attendance.hours.invalid.format}")
	@DecimalMin(value = "0.0", inclusive = true, message = "{attendance.invalid.hours}")
	@DecimalMax(value = "24.0", inclusive = true, message = "{attendance.invalid.hours}")
	private BigDecimal workingHours;

	private LocalTime checkInTime;
	private LocalTime checkOutTime;
	private String remarks;
	private String approvedBy;
	private ApprovalStatus approvalStatus;
}
