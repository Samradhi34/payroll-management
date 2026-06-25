package com.epms.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import com.epms.config.security.UserDetailsImpl;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.epms.constant.EmployeeStatus;
import com.epms.dto.request.EmployeeCreateRequest;
import com.epms.dto.request.EmployeeSearchRequest;
import com.epms.dto.request.EmployeeUpdateRequest;
import com.epms.dto.response.EmployeeResponse;
import com.epms.exception.EmployeeNotFoundException;
import com.epms.locale.MessageByLocaleService;
import com.epms.response.GenericResponseHandlers;
import com.epms.service.EmployeeService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller managing employee operations.
 * Secured with method level security.
 */
@RestController
@RequestMapping("/employees")
@RequiredArgsConstructor
@Slf4j
public class EmployeeController {

	private final MessageByLocaleService messageByLocaleService;
	private final EmployeeService employeeService;

	/**
	 * Create Employee
	 * Accessible only to ADMIN and HR.
	 */
	@PostMapping(consumes = "multipart/form-data")
	@PreAuthorize("hasAnyRole('ADMIN', 'HR')")
	public ResponseEntity<?> createEmployee(@ModelAttribute @Valid EmployeeCreateRequest request,
			@RequestPart(value = "image", required = false) MultipartFile image) {

		log.info("REST request to create employee inside employee controller");

		EmployeeResponse employee = employeeService.createEmployee(request, image);

		URI location = ServletUriComponentsBuilder
				.fromCurrentContextPath()
				.path("/employees/{id}")
				.buildAndExpand(employee.getId())
				.toUri();

		return ResponseEntity.created(location)
				.body(new GenericResponseHandlers.Builder()
						.setMessage(messageByLocaleService.getMessage("employee.created", null))
						.setStatus(HttpStatus.CREATED)
						.setData(employee).create().getBody());
	}

	/**
	 * Update Employee
	 * Accessible only to ADMIN and HR.
	 */
	@PutMapping("/{id}")
	@PreAuthorize("hasAnyRole('ADMIN', 'HR')")
	public ResponseEntity<?> updateEmployee(@PathVariable @Positive(message = "Id must be positive") Long id,
			@Valid @RequestBody EmployeeUpdateRequest request) throws EmployeeNotFoundException {

		log.info("REST request to update Employee. Id: {}", id);

		EmployeeResponse response = employeeService.updateEmployee(id, request);

		return new GenericResponseHandlers.Builder().setStatus(HttpStatus.OK)
				.setMessage(messageByLocaleService.getMessage("employee.update.success", new Object[] { id }))
				.setData(response).create();
	}

	/**
	 * Modify uploaded image
	 * Accessible to ADMIN, HR, and EMPLOYEE.
	 */
	@PatchMapping("/{id}/profile-image")
	@PreAuthorize("hasAnyRole('ADMIN', 'HR', 'EMPLOYEE')")
	public ResponseEntity<?> updateEmployeeImage(@PathVariable Long id,
			@RequestPart(value = "image", required = false) MultipartFile image) {

		log.info("Request to update employee image. Id: {}", id);

		employeeService.updateEmployeeImage(id, image);

		return ResponseEntity.ok(messageByLocaleService.getMessage("employee.image.updated", null));
	}

	/**
	 * Download employee image
	 * Accessible to ADMIN, HR, and EMPLOYEE.
	 */
	@GetMapping("/{id}/download-image")
	@PreAuthorize("hasAnyRole('ADMIN', 'HR', 'EMPLOYEE')")
	public ResponseEntity<?> downloadEmployeeImage(@PathVariable @Positive(message = "Id must be positive") Long id) {

		log.info("REST request to download employee image. Id: {}", id);

		employeeService.downloadEmployeeImage(id);

		return new GenericResponseHandlers.Builder().setStatus(HttpStatus.OK)
				.setMessage(messageByLocaleService.getMessage("employee.image.download.success", new Object[] { id }))
				.create();
	}

	/**
	 * Change Employee Status (Active/Inactive)
	 * Restricted exclusively to ADMIN.
	 */
	@PatchMapping("/{id}/status")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> changeEmployeeStatus(@PathVariable @Positive(message = "Id must be positive") Long id,
			@RequestParam EmployeeStatus status) {

		log.info("Change employee status request. Id: {}, Status: {}", id, status);

		employeeService.changeEmployeeStatus(id, status);

		return new GenericResponseHandlers.Builder().setStatus(HttpStatus.OK)
				.setMessage(messageByLocaleService.getMessage("employee.status.update.success", null)).create();
	}

	/**
	 * Get All Employees with specification-based filtering, pagination and sorting.
	 * Restricted to ADMIN and HR.
	 */
	@GetMapping
	@PreAuthorize("hasAnyRole('ADMIN', 'HR')")
	public ResponseEntity<?> getAllEmployees(
			@Valid EmployeeSearchRequest request,
			@PageableDefault(size = 10, sort = "id") Pageable pageable) {

		log.info("REST request to get Employees matching criteria: {}, pageable: {}", request, pageable);

		Page<EmployeeResponse> page = employeeService.getAllEmployees(request, pageable);

		return new GenericResponseHandlers.Builder()
				.setStatus(HttpStatus.OK)
				.setMessage(messageByLocaleService.getMessage("employee.fetch.all.success", null))
				.setData(page.getContent())
				.setPageNumber(page.getNumber() + 1)
				.setPageSize(page.getSize())
				.setTotalPages(page.getTotalPages())
				.setTotalCount(page.getTotalElements())
				.setHasPreviousPage(page.hasPrevious())
				.setHasNextPage(page.hasNext())
				.create();
	}

	/**
	 * Get Employee by ID
	 * Accessible to ADMIN, HR, and EMPLOYEE.
	 */
	@GetMapping("/{id}")
	@PreAuthorize("hasAnyRole('ADMIN', 'HR', 'EMPLOYEE')")
	public ResponseEntity<?> getEmployeeById(@PathVariable @Positive(message = "Id must be positive") Long id)
			throws EmployeeNotFoundException {

		log.debug("REST request to get Employee by Id: {}", id);

		EmployeeResponse response = employeeService.getEmployeeById(id);

		return new GenericResponseHandlers.Builder().setStatus(HttpStatus.OK)
				.setMessage(messageByLocaleService.getMessage("employee.fetch.success", new Object[] { id }))
				.setData(response).create();
	}

	/**
	 * Get current logged in Employee profile
	 */
	@GetMapping("/me")
	@PreAuthorize("hasAnyRole('ADMIN', 'HR', 'EMPLOYEE')")
	public ResponseEntity<?> getMyProfile(Authentication authentication) {
		UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
		log.info("Fetching logged in employee profile for email: {}", userDetails.getEmail());
		EmployeeResponse response = employeeService.getEmployeeByEmail(userDetails.getEmail());
		return new GenericResponseHandlers.Builder().setStatus(HttpStatus.OK)
				.setMessage(messageByLocaleService.getMessage("employee.fetch.success", new Object[] { userDetails.getEmail() }))
				.setData(response).create();
	}
	
}
