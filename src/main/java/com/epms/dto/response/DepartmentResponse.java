package com.epms.dto.response;

import java.io.Serializable;

import lombok.Data;

@Data
public class DepartmentResponse implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5851714644857888485L;

	private Long id;
	private String name;
	private String description;
	private String location;
	private Boolean active;

}
