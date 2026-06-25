package com.epms.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.epms.constant.EmployeeStatus;

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
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "employee")
@Data
@NoArgsConstructor 
@ToString(callSuper = false)
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class Employee extends CommonModel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2494289906313579309L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@EqualsAndHashCode.Include
	private Long id;

	@Column(name = "first_name", length = 50, nullable = false)
	private String firstName;

	@Column(name = "last_name", length = 50, nullable = false)
	private String lastName;

	@Column(name = "email", unique = true, length = 120, nullable = false)
	private String email;

	@Column(name = "phone", unique = true, length = 15, nullable = false)
	private String phone;

	@Column(name = "designation", nullable = false, length = 80)
	private String designation;

	@Column(name = "base_salary", precision = 10, scale = 2, nullable = false)
	private BigDecimal baseSalary;

	@Column(name = "joining_date", nullable = false)
	private LocalDate joiningDate;

	@Column(name = "profile_image_path")
	private String profileImagePath;

	@Enumerated(EnumType.STRING)
	@Column(name = "employee_status", nullable = false)
	private EmployeeStatus employeeStatus;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "department_id", nullable = false)
	private Department department;

	@Column(name = "resignation_date")
	private LocalDate resignationDate;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "manager_id")
	private Employee manager;

}
