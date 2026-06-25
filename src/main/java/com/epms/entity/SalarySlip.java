package com.epms.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "salary_slip")
@Getter
@Setter
@NoArgsConstructor 
@ToString(callSuper = false)
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class SalarySlip extends CommonModel {
	/**
	 * 
	 */
	private static final long serialVersionUID = -3864464149026041374L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@EqualsAndHashCode.Include
	private Long id;

	@Column(name = "slip_path", nullable = false)
	private String slipPath;

	@Column(name = "generated_date", nullable = false)
	private LocalDateTime generatedDate;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "payroll_id", nullable = false, unique = true)
	private Payroll payroll;
}
