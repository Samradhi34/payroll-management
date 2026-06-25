package com.epms.dto.request;

import java.io.Serializable;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DepartmentRequest implements Serializable {

	private static final long serialVersionUID = -6098496971844070356L;

	@NotBlank(message = "Department name is required")
	@Size(min = 2, max = 100, message = "Department name must be between 2 and 100 characters")
	private String name;

	@Size(max = 250, message = "Description can be max 250 characters")
	private String description;
	
	@NotBlank(message = "Location is required")
    @Size(max = 100)
    private String location;

}
