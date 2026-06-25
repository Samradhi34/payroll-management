package com.epms.mapper;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import com.epms.dto.request.RegisterRequest;
import com.epms.entity.User;

@Component
public class UserMapper {
	
	public User dtoToEntity(RegisterRequest dto, String encodedPassword) {

		if (dto == null)
			return null;

		User entity = new User();
		BeanUtils.copyProperties(dto, entity, "password");
		/**
		 * Manually set secure fields
		 */
		entity.setPasswordHash(encodedPassword);
		entity.setEnabled(true);
		
		return entity;
	}


}
