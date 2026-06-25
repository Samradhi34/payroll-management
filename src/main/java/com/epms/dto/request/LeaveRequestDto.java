package com.epms.dto.request;

import java.io.Serializable;
import java.time.LocalDate;

import com.epms.constant.LeaveType;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LeaveRequestDto implements Serializable {

	private static final long serialVersionUID = 1L;

	@NotNull(message = "Employee ID is required")
	private Long employeeId;

	@NotNull(message = "Leave type is required")
	private LeaveType leaveType;

	@NotNull(message = "Start date is required")
	private LocalDate startDate;

	@NotNull(message = "End date is required")
	private LocalDate endDate;

	private String reason;

	private Boolean isHalfDay = false;
}
