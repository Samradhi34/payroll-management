package com.epms.dto.request;

import java.io.Serializable;
import lombok.Data;

@Data
public class DepartmentSearchRequest implements Serializable {

	private static final long serialVersionUID = 1L;

	private String search; // Partial search on department name, description, or location
	private Boolean active;
}
