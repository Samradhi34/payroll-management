package com.epms.controller;

import java.net.URI;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.epms.dto.request.DepartmentRequest;
import com.epms.dto.request.DepartmentSearchRequest;
import com.epms.dto.response.DepartmentResponse;
import com.epms.exception.DeparmentNotFoundException;
import com.epms.locale.MessageByLocaleService;
import com.epms.response.GenericResponseHandlers;
import com.epms.service.DepartmentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller managing department APIs.
 * Secured via method-level security with @PreAuthorize.
 */
@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/departments")
public class DepartmentController {
	
	private final MessageByLocaleService messageByLocaleService;
	private final DepartmentService departmentService;

	/**
	 * Create Department
	 * Restricted to ADMIN role.
	 */
	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> createDepartment(@Valid @RequestBody DepartmentRequest request) {
		log.info("REST request to create Department");
		DepartmentResponse response = departmentService.createDepartment(request);

		URI location = ServletUriComponentsBuilder
				.fromCurrentContextPath()
				.path("/departments/{id}")
				.buildAndExpand(response.getId())
				.toUri();

		return ResponseEntity.created(location)
				.body(new GenericResponseHandlers.Builder()
						.setStatus(HttpStatus.CREATED)
						.setMessage(messageByLocaleService.getMessage("department.create.success", null))
						.setData(response)
						.create().getBody());
	}

	/**
	 * Update Department
	 * Restricted to ADMIN role.
	 */
	@PutMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> updateDepartment(@PathVariable Long id,
											  @Valid @RequestBody DepartmentRequest request) throws DeparmentNotFoundException {
		log.info("REST request to update Department. Id: {}", id);
		DepartmentResponse response = departmentService.updateDepartment(id, request);

		return new GenericResponseHandlers.Builder()
				.setStatus(HttpStatus.OK)
				.setMessage(messageByLocaleService.getMessage("department.update.success", null))
				.setData(response)
				.create();
	}

	/**
	 * Change Department Status
	 * Restricted to ADMIN role.
	 */
	@PatchMapping("/{id}/status")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> changeDepartmentStatus(@PathVariable Long id,
													 @RequestParam Boolean active) throws DeparmentNotFoundException {
		log.info("REST request to change Department status. Id: {}, Active: {}", id, active);
		departmentService.changeDepartmentStatus(id, active);

		return new GenericResponseHandlers.Builder()
				.setStatus(HttpStatus.OK)
				.setMessage(messageByLocaleService.getMessage("department.status.update.success", null))
				.create();
	}

	/**
	 * Get All Departments with specification, pagination and sorting.
	 * Accessible to all authenticated users (ADMIN, HR, EMPLOYEE).
	 */
	@GetMapping
	@PreAuthorize("hasAnyRole('ADMIN', 'HR', 'EMPLOYEE')")
	public ResponseEntity<?> getAllDepartments(
			@Valid DepartmentSearchRequest request,
			@PageableDefault(size = 10, sort = "id") Pageable pageable) {
		log.info("REST request to get Departments with criteria: {}, pageable: {}", request, pageable);

		Page<DepartmentResponse> page = departmentService.getAllDepartment(request, pageable);

		return new GenericResponseHandlers.Builder()
				.setStatus(HttpStatus.OK)
				.setMessage(messageByLocaleService.getMessage("department.fetch.all.success", null))
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
	 * Get Department by ID
	 * Accessible to all authenticated users (ADMIN, HR, EMPLOYEE).
	 */
	@GetMapping("/{id}")
	@PreAuthorize("hasAnyRole('ADMIN', 'HR', 'EMPLOYEE')")
	public ResponseEntity<?> getDepartmentById(@PathVariable Long id) throws DeparmentNotFoundException {
		log.info("REST request to get Department by Id: {}", id);
		DepartmentResponse response = departmentService.getDepartmentById(id);

		return new GenericResponseHandlers.Builder()
				.setStatus(HttpStatus.OK)
				.setMessage(messageByLocaleService.getMessage("department.fetch.success", new Object[]{id}))
				.setData(response)
				.create();
	}
}
