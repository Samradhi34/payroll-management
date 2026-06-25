package com.epms.dto.response;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.epms.constant.PayrollStatus;

import lombok.Data;

@Data
public class PayrollResponse implements Serializable {

	private static final long serialVersionUID = -4080045880293579602L;
	
	private Long id;
	private Long employeeId;
	private String employeeName;
	private Integer month;
	private Integer year;
	private BigDecimal baseSalary;
	private BigDecimal grossSalary;
	private Integer totalWorkingDays;
	private Integer absentDays;
	private Integer halfDays;
	private Integer unpaidLeaves;
	private Integer paidLeaves;
	private BigDecimal attendanceDeduction;
	private BigDecimal bonus;
	private BigDecimal tax;
	private BigDecimal pf;
	private BigDecimal deductions;
	private BigDecimal netSalary;
	private PayrollStatus payrollStatus;
	private LocalDateTime generatedDate;

}
