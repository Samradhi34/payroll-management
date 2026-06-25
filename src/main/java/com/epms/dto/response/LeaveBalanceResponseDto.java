package com.epms.dto.response;

import java.io.Serializable;
import java.math.BigDecimal;

import lombok.Data;

@Data
public class LeaveBalanceResponseDto implements Serializable {

	private static final long serialVersionUID = 1L;

	private Long id;
	private Long employeeId;
	private String employeeName;
	private Integer year;
	private BigDecimal casualLeaveBalance;
	private BigDecimal sickLeaveBalance;
	private BigDecimal earnedLeaveBalance;
}
