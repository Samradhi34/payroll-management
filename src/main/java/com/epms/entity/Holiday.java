package com.epms.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "holiday")
@Data
@NoArgsConstructor
@ToString(callSuper = false)
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class Holiday extends CommonModel {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@EqualsAndHashCode.Include
	private Long id;

	@Column(name = "name", nullable = false, length = 100)
	private String name;

	@Column(name = "holiday_date", nullable = false, unique = true)
	private LocalDate holidayDate;
}
