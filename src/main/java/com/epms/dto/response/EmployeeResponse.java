package com.epms.dto.response;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

import com.epms.constant.EmployeeStatus;

import lombok.Data;

@Data
public class EmployeeResponse implements Serializable {

	private static final long serialVersionUID = -197242458398721968L;
	
	private Long id;
	private String firstName;
	private String lastName;
	private String email;
	private String phone;
	private String designation;
	private BigDecimal baseSalary;
	private LocalDate joiningDate;
	private EmployeeStatus employeeStatus;
	private String departmentName;
	private String profileImagePath;
	private Long managerId;
	private String managerName;
	private LocalDate resignationDate;
}
