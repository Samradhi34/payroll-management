package com.epms.mapper;

import java.util.List;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import com.epms.dto.request.DepartmentRequest;
import com.epms.dto.response.DepartmentResponse;
import com.epms.entity.Department;

@Component
public class DepartmentMapper {

	public Department dtoToEntity(DepartmentRequest dto) {

		if (dto == null)
			return null;

		Department entity = new Department();
		BeanUtils.copyProperties(dto, entity);
		return entity;
	}

	public DepartmentResponse entityToDto(Department entity) {

		if (entity == null)
			return null;

		DepartmentResponse dto = new DepartmentResponse();
		BeanUtils.copyProperties(entity, dto);
		return dto;
	}

	public List<DepartmentResponse> toDtos(List<Department> departments) {
		if (departments == null)
			return List.of();

		return departments.stream().map(this::entityToDto).toList();
	}

}
