package com.epms.mapper;

import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import com.epms.dto.response.SalarySlipResponse;
import com.epms.entity.SalarySlip;

@Component
public class SalarySlipMapper {

	public SalarySlipResponse entityToDto(SalarySlip entity) {

		if (entity == null)
			return null;

		SalarySlipResponse dto = new SalarySlipResponse();
		BeanUtils.copyProperties(entity, dto);
		return dto;
	}

	public List<SalarySlipResponse> toDtos(List<SalarySlip> slips) {
		if (slips == null)
			return List.of();

		return slips.stream().map(this::entityToDto).toList();
	}

}
