package com.epms.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.epms.constant.AttendanceStatus;

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
@Table(name = "attendance", uniqueConstraints = {
		@UniqueConstraint(columnNames = { "employee_id", "attendance_date" }) })
@Data
@NoArgsConstructor 
@ToString(callSuper = false)
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class Attendance extends CommonModel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 3152412644198161371L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@EqualsAndHashCode.Include
	private Long id;

	@Column(name = "attendance_date", nullable = false)
	private LocalDate attendanceDate;

	@Enumerated(EnumType.STRING)
	@Column(name = "attendance_status", nullable = false)
	private AttendanceStatus attendanceStatus;

	@Column(name = "working_hours", nullable = false, precision = 5, scale = 2)
	private BigDecimal workingHours;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "employee_id", nullable = false)
	private Employee employee;

	@Column(name = "check_in_time")
	private java.time.LocalTime checkInTime;

	@Column(name = "check_out_time")
	private java.time.LocalTime checkOutTime;

	@Column(name = "remarks", length = 255)
	private String remarks;

	@Column(name = "approved_by", length = 120)
	private String approvedBy;

	@Enumerated(EnumType.STRING)
	@Column(name = "approval_status")
	private com.epms.constant.ApprovalStatus approvalStatus = com.epms.constant.ApprovalStatus.APPROVED;
}
