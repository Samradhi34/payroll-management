package com.epms.service.implementation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.epms.dto.request.DepartmentRequest;
import com.epms.dto.request.DepartmentSearchRequest;
import com.epms.dto.response.DepartmentResponse;
import com.epms.entity.Department;
import com.epms.exception.DeparmentNotFoundException;
import com.epms.exception.ResourceNotFoundException;
import com.epms.exception.ResourceNotNullException;
import com.epms.exception.ValidationException;
import com.epms.locale.MessageByLocaleService;
import com.epms.mapper.DepartmentMapper;
import com.epms.repository.DepartmentRepository;
import com.epms.service.DepartmentService;
import com.epms.specification.DepartmentSpecification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Throwable.class)
public class DepartmentServiceImpl implements DepartmentService {

	private final MessageByLocaleService messageByLocaleService;
	private final DepartmentRepository departmentRepository;
	private final DepartmentMapper departmentMapper;

	/**
	 * Creates a new department
	 */
	@Override
	public DepartmentResponse createDepartment(DepartmentRequest request) {

		/**
		 * To avoid crash - (NullPointerException), If client sends empty request ->
		 * reject
		 */
		if (request == null) {
			log.error("DepartmentRequest is null");

			throw new ValidationException(messageByLocaleService.getMessage("department.request.null", null));
		}

		log.info("Creating department with name: {}", request.getName());
		
		if (request.getName() == null || request.getName().trim().isEmpty()) {
		    throw new ValidationException(
		        messageByLocaleService.getMessage("department.name.invalid", null)
		    );
		}

		if (request.getLocation() == null || request.getLocation().trim().isEmpty()) {
		    throw new ValidationException(
		        messageByLocaleService.getMessage("department.location.invalid", null)
		    );
		}

		String name = request.getName().trim();
		request.setName(name);

		if (departmentRepository.existsByNameIgnoreCase(name)) {
			log.warn("Duplicate department name: {}", request.getName());

			throw new ValidationException(messageByLocaleService.getMessage("department.name.exists", null));
		}

		Department department = departmentMapper.dtoToEntity(request);
		department.setActive(Boolean.TRUE);

		Department saved = departmentRepository.save(department);

		log.info("Department created successfully. Id: {}", saved.getId());

		return departmentMapper.entityToDto(saved);
	}

	/**
	 * Updates an existing department by ID.
	 */
	@Override
	public DepartmentResponse updateDepartment(Long id, DepartmentRequest request) throws ResourceNotFoundException {

		if (request == null) {
			log.error("DepartmentRequest is null");
			throw new ValidationException(messageByLocaleService.getMessage("department.request.null", null));
		}

		log.info("Updating department. Id: {}", id);

		Department department = departmentRepository.findByIdAndActiveTrue(id).orElseThrow(() -> {
			log.error("Department not found. Id: {}", id);
			return new DeparmentNotFoundException(
					messageByLocaleService.getMessage("department.not.found", new Object[] { id }));
		});

		String name = request.getName().trim();

		if (name.isEmpty()) {
			throw new ValidationException(messageByLocaleService.getMessage("department.name.invalid", null));
		}

		request.setName(name);
		
		/**
		 * User sent same name again, No real update happened
		 */
		if (department.getName().equalsIgnoreCase(name)) {
		    log.info("No name change detected for department id: {}", id);
		}

		if (!department.getName().equalsIgnoreCase(name) && departmentRepository.existsByNameIgnoreCase(name)) {

			log.warn("Duplicate department name during update: {}", request.getName());

			throw new ValidationException(messageByLocaleService.getMessage("department.name.exists", null));
		}

		department.setName(request.getName());

		if (request.getDescription() != null) {
			department.setDescription(request.getDescription().trim());
		}

		if (request.getLocation() == null || request.getLocation().trim().isEmpty()) {
			throw new ValidationException(messageByLocaleService.getMessage("department.location.invalid", null));
		}
		department.setLocation(request.getLocation().trim());

		log.info("Department updated successfully. Id: {}", id);

		return departmentMapper.entityToDto(department);
	}

	/**
	 * Changes the active status of an department.
	 */
	@Override
	public void changeDepartmentStatus(Long id, Boolean active) throws ResourceNotFoundException {

		log.info("Changing department status. Id: {}, Active: {}", id, active);

		if (active == null) {
			log.warn("Active flag is null for department id: {}", id);

			throw new ValidationException(messageByLocaleService.getMessage("department.status.invalid", null));
		}

		Department department = departmentRepository.findById(id).orElseThrow(() -> {
			log.error("Department not found. Id: {}", id);
			return new DeparmentNotFoundException(
					messageByLocaleService.getMessage("department.not.found", new Object[] { id }));
		});

		if (department.getActive().equals(active)) {
			log.warn("Department already in requested status. Id: {}", id);
			throw new ValidationException(messageByLocaleService.getMessage("department.status.same", null));
		}

		/**
		 * Prevent deactivate if employees exist
		 */
		if (!active && department.getEmployees() != null && !department.getEmployees().isEmpty()) {

			log.error("Cannot deactivate department with employees. Id: {}", id);

			throw new ResourceNotNullException(messageByLocaleService.getMessage("department.has.employees", null));
		}

		department.setActive(active);

		log.info("Department status updated successfully. Id: {}", id);
	}

	/**
	 * Get All Departments with specification, pagination and sorting
	 */
	@Override
	public Page<DepartmentResponse> getAllDepartment(DepartmentSearchRequest request, Pageable pageable) {
		log.info("Fetching departments with filter request: {}, pageable: {}", request, pageable);

		org.springframework.data.jpa.domain.Specification<Department> spec = DepartmentSpecification.getSpecification(request);
		Page<Department> departmentPage = departmentRepository.findAll(spec, pageable);

		log.debug("Fetched {} departments on page {}", departmentPage.getNumberOfElements(), departmentPage.getNumber());

		return departmentPage.map(departmentMapper::entityToDto);
	}

	/**
	 * Get Department by Id
	 */
	@Override
	public DepartmentResponse getDepartmentById(Long id) throws ResourceNotFoundException {

		Department department = departmentRepository.findByIdAndActiveTrue(id).orElseThrow(() -> {
			log.error("Department not found. Id: {}", id);
			return new DeparmentNotFoundException(
					messageByLocaleService.getMessage("department.not.found", new Object[] { id }));
		});

		return departmentMapper.entityToDto(department);
	}

}
