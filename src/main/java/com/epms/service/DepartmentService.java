package com.epms.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.epms.dto.request.DepartmentRequest;
import com.epms.dto.request.DepartmentSearchRequest;
import com.epms.dto.response.DepartmentResponse;
import com.epms.exception.ResourceNotFoundException;

public interface DepartmentService {

	/**
	 * Add new department
	 * 
	 * @param request
	 * @return
	 */
	DepartmentResponse createDepartment(DepartmentRequest request);

	/**
	 * modify department details
	 * 
	 * @param id
	 * @param request
	 * @return
	 * @throws ResourceNotFoundException 
	 */
	DepartmentResponse updateDepartment(Long id, DepartmentRequest request) throws ResourceNotFoundException;

	/**
	 * Fetch department details by id
	 * 
	 * @param id
	 * @return
	 * @throws ResourceNotFoundException 
	 */
	DepartmentResponse getDepartmentById(Long id) throws ResourceNotFoundException;

	/**
	 * List all details of department with specifications, pagination and sorting
	 * 
	 * @param request search filters
	 * @param pageable pagination parameters
	 * @return page of department responses
	 */
	Page<DepartmentResponse> getAllDepartment(DepartmentSearchRequest request, Pageable pageable);

	/**
	 * Change status of department(active/de-active)
	 * 
	 * @param id
	 * @param active
	 * @throws ResourceNotFoundException 
	 */
	void changeDepartmentStatus(Long id, Boolean active) throws ResourceNotFoundException;

}
