package com.epms.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "leave_balance", uniqueConstraints = {
		@UniqueConstraint(columnNames = { "employee_id", "year" }) })
@Data
@NoArgsConstructor
@ToString(callSuper = false)
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class LeaveBalance extends CommonModel {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@EqualsAndHashCode.Include
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "employee_id", nullable = false)
	private Employee employee;

	@Column(name = "year", nullable = false)
	private Integer year;

	@Column(name = "casual_leave_balance", nullable = false, precision = 5, scale = 1)
	private BigDecimal casualLeaveBalance = BigDecimal.valueOf(12.0);

	@Column(name = "sick_leave_balance", nullable = false, precision = 5, scale = 1)
	private BigDecimal sickLeaveBalance = BigDecimal.valueOf(10.0);

	@Column(name = "earned_leave_balance", nullable = false, precision = 5, scale = 1)
	private BigDecimal earnedLeaveBalance = BigDecimal.valueOf(15.0);
}
