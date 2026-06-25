package com.epms.mapper;

import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import com.epms.dto.request.PayrollGenerateRequest;
import com.epms.dto.response.PayrollResponse;
import com.epms.entity.Employee;
import com.epms.entity.Payroll;

@Component
public class PayrollMapper {

	public Payroll dtoToEntity(PayrollGenerateRequest dto, Employee employee) {

		if (dto == null)
			return null;

		Payroll entity = new Payroll();
		BeanUtils.copyProperties(dto, entity, "employeeId");

		entity.setEmployee(employee);
		entity.setBaseSalary(employee.getBaseSalary());

		return entity;
	}

	public PayrollResponse entityToDto(Payroll entity) {

		if (entity == null)
			return null;

		PayrollResponse dto = new PayrollResponse();
		BeanUtils.copyProperties(entity, dto);

		if (entity.getEmployee() != null) {
			Employee employee = entity.getEmployee();
			dto.setEmployeeId(employee.getId());
			dto.setEmployeeName(employee.getFirstName() + " " + employee.getLastName());
		}

		return dto;
	}

	public List<PayrollResponse> toDtos(List<Payroll> payrolls) {
		if (payrolls == null)
			return List.of();

		return payrolls.stream().map(this::entityToDto).toList();
	}

}
