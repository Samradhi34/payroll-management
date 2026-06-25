package com.epms.mapper;

import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import com.epms.dto.request.AttendanceRequestDto;
import com.epms.dto.response.AttendanceResponseDto;
import com.epms.entity.Attendance;
import com.epms.entity.Employee;

@Component
public class AttendanceMapper {

	public Attendance dtoToEntity(AttendanceRequestDto dto, Employee employee) {
		Attendance entity = new Attendance();
		BeanUtils.copyProperties(dto, entity);
		entity.setEmployee(employee);
		return entity;
	}

	public AttendanceResponseDto entityToDto(Attendance entity) {
		AttendanceResponseDto dto = new AttendanceResponseDto();
		BeanUtils.copyProperties(entity, dto);

		dto.setEmployeeId(entity.getEmployee().getId());
		dto.setEmployeeName(entity.getEmployee().getFirstName() + " " + entity.getEmployee().getLastName());

		return dto;
	}

	public List<AttendanceResponseDto> toDtos(List<Attendance> attendances) {
		if(attendances == null) 
			return List.of();
		
		return attendances.stream().map(this::entityToDto).toList();
	}

}
