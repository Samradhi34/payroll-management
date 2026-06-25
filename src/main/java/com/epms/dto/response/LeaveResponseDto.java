package com.epms.dto.response;

import java.io.Serializable;
import java.time.LocalDate;

import com.epms.constant.LeaveType;
import com.epms.constant.LeaveStatus;

import lombok.Data;

@Data
public class LeaveResponseDto implements Serializable {

	private static final long serialVersionUID = 1L;

	private Long id;
	private Long employeeId;
	private String employeeName;
	private LeaveType leaveType;
	private LocalDate startDate;
	private LocalDate endDate;
	private LeaveStatus leaveStatus;
	private String reason;
	private String approvedBy;
	private Boolean isHalfDay;
}
