package com.epms.mapper;

import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import com.epms.dto.request.EmployeeCreateRequest;
import com.epms.dto.response.EmployeeResponse;
import com.epms.entity.Department;
import com.epms.entity.Employee;

@Component
public class EmployeeMapper {

	public Employee dtoToEntity(EmployeeCreateRequest dto, Department department) {

		if (dto == null)
			return null;

		Employee entity = new Employee();
		BeanUtils.copyProperties(dto, entity, "departmentId");
		entity.setDepartment(department);
		return entity;
	}

	public EmployeeResponse entityToDto(Employee entity) {

		if (entity == null)
			return null;

		EmployeeResponse dto = new EmployeeResponse();
		BeanUtils.copyProperties(entity, dto);

		if (entity.getDepartment() != null) {
			dto.setDepartmentName(entity.getDepartment().getName());
		}

		if (entity.getManager() != null) {
			dto.setManagerId(entity.getManager().getId());
			dto.setManagerName(entity.getManager().getFirstName() + " " + entity.getManager().getLastName());
		}

		return dto;
	}

	public List<EmployeeResponse> toDtos(List<Employee> employees) {
		if (employees == null)
			return List.of();

		return employees.stream().map(this::entityToDto).toList();
	}

}
