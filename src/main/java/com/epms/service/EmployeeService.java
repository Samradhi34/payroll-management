package com.epms.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import com.epms.constant.EmployeeStatus;
import com.epms.dto.request.EmployeeCreateRequest;
import com.epms.dto.request.EmployeeSearchRequest;
import com.epms.dto.request.EmployeeUpdateRequest;
import com.epms.dto.response.EmployeeResponse;

public interface EmployeeService {
	
	/**
	 * Creates a new employee
	 *
	 * @param request the employee creation request data
	 * @return the created employee details
	 */
	EmployeeResponse createEmployee(EmployeeCreateRequest request, MultipartFile file);
	

	/**
	 * Modify employee details
	 * 
	 * @param id
	 * @param request
	 * @return
	 */
	EmployeeResponse updateEmployee(Long id, EmployeeUpdateRequest request);
	
	/**
	 * Modify employee uploaded image
	 * @param id
	 * @param file
	 */
	void updateEmployeeImage(Long id, MultipartFile file);

	/**
	 * Fetch employee details by id
	 * 
	 * @param id
	 * @return
	 */
	EmployeeResponse getEmployeeById(Long id);

	/**
	 * List all employees with specification-based filtering, pagination and sorting
	 * 
	 * @param request the search criteria
	 * @param pageable pagination and sorting parameters
	 * @return a page of matching employee details
	 */
	Page<EmployeeResponse> getAllEmployees(EmployeeSearchRequest request, Pageable pageable);

//	/**
//	 * Change status of employee (active/de-active)
//	 * 
//	 * @param id
//	 * @param status
//	 */
//	void changeEmployeeStatus(Long id, Boolean active);

	/**
	 * Changes the active status of an employee.
	 * 
	 */
	void changeEmployeeStatus(Long id, EmployeeStatus status);


	/**
	 * Download image of an employee 
	 * @param id
	 */
	void downloadEmployeeImage(Long id);

	/**
	 * Fetch employee details by email
	 * 
	 * @param email
	 * @return
	 */
	EmployeeResponse getEmployeeByEmail(String email);

}
