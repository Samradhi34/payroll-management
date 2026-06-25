package com.epms.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.epms.constant.PayrollStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "payroll", uniqueConstraints = { @UniqueConstraint(columnNames = { "employee_id", "month", "year" }) })
@Data
@NoArgsConstructor 
@ToString(callSuper = false)
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class Payroll extends CommonModel {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7297789957232165605L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@EqualsAndHashCode.Include
	private Long id;

	@Column(name = "month", nullable = false)
	private Integer month;

	@Column(name = "year", nullable = false)
	private Integer year;

	@Column(name = "base_salary", nullable = false, precision = 10, scale = 2)
	private BigDecimal baseSalary;

	@Column(name = "gross_salary", precision = 10, scale = 2)
	private BigDecimal grossSalary;

	@Column(name = "total_working_days")
	private Integer totalWorkingDays;

	@Column(name = "absent_days")
	private Integer absentDays;

	@Column(name = "half_days")
	private Integer halfDays;

	@Column(name = "unpaid_leaves")
	private Integer unpaidLeaves;

	@Column(name = "paid_leaves")
	private Integer paidLeaves;

	@Column(name = "attendance_deduction", precision = 10, scale = 2)
	private BigDecimal attendanceDeduction;

	@Column(name = "bonus", nullable = false, precision = 10, scale = 2)
	private BigDecimal bonus;

	@Column(name = "tax", precision = 10, scale = 2)
	private BigDecimal tax;

	@Column(name = "pf", precision = 10, scale = 2)
	private BigDecimal pf;

	@Column(name = "deductions", nullable = false, precision = 10, scale = 2)
	private BigDecimal deductions;

	@Column(name = "net_salary", nullable = false, precision = 10, scale = 2)
	private BigDecimal netSalary;

	@Column(name = "generated_date", nullable = false)
	private LocalDateTime generatedDate;

	@Enumerated(EnumType.STRING)
	@Column(name = "payroll_status", nullable = false)
	private PayrollStatus payrollStatus;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "employee_id", nullable = false)
	private Employee employee;

}
