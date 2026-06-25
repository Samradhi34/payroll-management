package com.epms.dto.response;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import com.epms.constant.AttendanceStatus;
import com.epms.constant.ApprovalStatus;

import lombok.Data;

@Data 
public class AttendanceResponseDto implements Serializable{
	
	private static final long serialVersionUID = 265113178488999049L;
	
	private Long id;
	private LocalDate attendanceDate;
	private AttendanceStatus attendanceStatus;
	private BigDecimal workingHours;
	private Long employeeId;
	private String employeeName;
	private LocalTime checkInTime;
	private LocalTime checkOutTime;
	private String remarks;
	private String approvedBy;
	private ApprovalStatus approvalStatus;

}
