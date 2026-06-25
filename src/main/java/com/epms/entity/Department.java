package com.epms.entity;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Table(name = "department") 
@Entity 
@Data 
@NoArgsConstructor 
@ToString(callSuper = false)
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class Department extends CommonModel {

	private static final long serialVersionUID = 3916337541399937990L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@EqualsAndHashCode.Include
	private Long id;

	@Column(name = "dept_name", nullable = false, unique = true, length = 100)
	private String name;

	@Column(name = "dept_desc", length = 250)
	private String description;

	@Column(name = "location", nullable = false, length = 100)
	private String location;

	@ToString.Exclude
	@OneToMany(mappedBy = "department", fetch = FetchType.LAZY)
	private List<Employee> employees;

}
