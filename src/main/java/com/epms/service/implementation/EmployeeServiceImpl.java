package com.epms.service.implementation;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.epms.constant.EmployeeStatus;
import com.epms.dto.request.EmployeeCreateRequest;
import com.epms.dto.request.EmployeeSearchRequest;
import com.epms.dto.request.EmployeeUpdateRequest;
import com.epms.dto.response.EmployeeResponse;
import com.epms.entity.Department;
import com.epms.entity.Employee;
import com.epms.exception.DeparmentNotFoundException;
import com.epms.exception.EmployeeNotFoundException;
import com.epms.exception.ResourceNotFoundException;
import com.epms.exception.ValidationException;
import com.epms.locale.MessageByLocaleService;
import com.epms.mapper.EmployeeMapper;
import com.epms.repository.DepartmentRepository;
import com.epms.repository.EmployeeRepository;
import com.epms.repository.UserRepository;
import com.epms.service.EmployeeService;
import com.epms.service.FileStorageService;
import com.epms.specification.EmployeeSpecification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Throwable.class)
public class EmployeeServiceImpl implements EmployeeService {

	private final MessageByLocaleService messageByLocaleService;

	private final EmployeeRepository employeeRepository;
	private final DepartmentRepository departmentRepository;
	private final UserRepository userRepository;

	private final FileStorageService fileStorageService;

	private final EmployeeMapper employeeMapper;

	/**
	 * Creates a new employee
	 *
	 * @param request the employee creation request data
	 * @return the created employee details
	 */
	@Override
	public EmployeeResponse createEmployee(EmployeeCreateRequest request, MultipartFile file) {

		if (request == null) {
			log.error("EmployeeCreateRequest is null");
			throw new ValidationException(messageByLocaleService.getMessage("employee.request.null", null));
		}

		log.info("Creating employee with email: {}", request.getEmail());

		String email = request.getEmail() != null ? request.getEmail().trim() : null;
		String phone = request.getPhone() != null ? request.getPhone().trim() : null;

		request.setEmail(email);
		request.setPhone(phone);

		if (employeeRepository.existsByEmailIgnoreCase(email)) {
			log.warn("Duplicate email: {}", email);
			throw new ValidationException(messageByLocaleService.getMessage("employee.email.exists", null));
		}

		if (employeeRepository.existsByPhone(phone)) {
			log.warn("Duplicate phone: {}", phone);
			throw new ValidationException(messageByLocaleService.getMessage("employee.phone.exists", null));
		}

		Department department = departmentRepository.findById(request.getDepartmentId()).orElseThrow(() -> {
			log.error("Department not found. Id: {}", request.getDepartmentId());
			throw new DeparmentNotFoundException(messageByLocaleService.getMessage("department.not.found",
					new Object[] { request.getDepartmentId() }));
		});

		/**
		 * If department is inactive, then it shows error. i.e cannot add employee to
		 * closed department.
		 */
		if (Boolean.FALSE.equals(department.getActive())) {
			log.error("Department inactive. Id: {}", department.getId());
			throw new ValidationException(messageByLocaleService.getMessage("department.inactive", null));
		}

		Employee employee = employeeMapper.dtoToEntity(request, department);

		if (request.getManagerId() != null) {
			Employee manager = employeeRepository.findById(request.getManagerId())
					.orElseThrow(() -> new ResourceNotFoundException("Manager not found with ID: " + request.getManagerId()));
			employee.setManager(manager);
		}

		/**
		 * set default status
		 */
		if (employee.getEmployeeStatus() == null) {
			employee.setEmployeeStatus(EmployeeStatus.ACTIVE);
		}

		Employee saved = employeeRepository.save(employee);

		if (file != null && !file.isEmpty()) {

			String imageUrl = fileStorageService.uploadFile(file, saved.getId());

			saved.setProfileImagePath(imageUrl);

			employeeRepository.save(saved);

			log.debug("Profile image uploaded for employeeId: {}", saved.getId());

		} else {
			log.info("No profile image provided for employee");
		}

		log.info("Employee created successfully. Id: {}, Status: {}", saved.getId(), saved.getEmployeeStatus());

		return employeeMapper.entityToDto(saved);
	}

	/**
	 * Updates an existing employee by ID.
	 * 
	 * @param id      the employee ID
	 * @param request the employee update request data
	 * @return the updated employee details
	 */
	@Override
	public EmployeeResponse updateEmployee(Long id, EmployeeUpdateRequest request) {

		if (request == null) {
			log.error("EmployeeUpdateRequest is null");
			throw new ValidationException(messageByLocaleService.getMessage("employee.request.null", null));
		}

		boolean isAllFieldsEmpty = (request.getFirstName() == null || request.getFirstName().trim().isEmpty())
				&& (request.getLastName() == null || request.getLastName().trim().isEmpty())
				&& (request.getEmail() == null || request.getEmail().trim().isEmpty())
				&& (request.getPhone() == null || request.getPhone().trim().isEmpty())
				&& (request.getDesignation() == null || request.getDesignation().trim().isEmpty())
				&& request.getBaseSalary() == null;

		if (isAllFieldsEmpty) {
			log.warn("Empty update request for employee id: {}", id);
			throw new ValidationException(messageByLocaleService.getMessage("employee.update.fields.required", null));
		}

		log.info("Updating employee. Id: {}", id);

		Employee employee = employeeRepository.findById(id).orElseThrow(() -> {
			log.error("Employee not found. Id: {}", id);
			return new EmployeeNotFoundException(
					messageByLocaleService.getMessage("employee.not.found", new Object[] { id }));
		});

		log.debug("Current employee data. Id: {}, Email: {}, Phone: {}", id, employee.getEmail(), employee.getPhone());

		if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {

			String newEmail = request.getEmail().trim();

			if (!newEmail.equals(employee.getEmail()) && employeeRepository.existsByEmailIgnoreCase(newEmail)) {

				log.warn("Duplicate email during update: {}", newEmail);
				throw new ValidationException(messageByLocaleService.getMessage("employee.email.exists", null));
			}

			employee.setEmail(newEmail);
		}

		if (request.getPhone() != null && !request.getPhone().trim().isEmpty()) {

			String newPhone = request.getPhone().trim();

			if (!newPhone.equals(employee.getPhone()) && employeeRepository.existsByPhone(newPhone)) {

				log.warn("Duplicate phone during update: {}", newPhone);
				throw new ValidationException(messageByLocaleService.getMessage("employee.phone.exists", null));
			}

			employee.setPhone(newPhone);
		}

		if (request.getFirstName() != null && !request.getFirstName().trim().isEmpty()) {
			employee.setFirstName(request.getFirstName().trim());
		}

		if (request.getLastName() != null && !request.getLastName().trim().isEmpty()) {
			employee.setLastName(request.getLastName().trim());
		}

		if (request.getDesignation() != null && !request.getDesignation().trim().isEmpty()) {
			employee.setDesignation(request.getDesignation().trim());
		}

		if (request.getBaseSalary() != null) {
			employee.setBaseSalary(request.getBaseSalary());
		}

		if (request.getManagerId() != null) {
			Employee manager = employeeRepository.findById(request.getManagerId())
					.orElseThrow(() -> new ResourceNotFoundException("Manager not found with ID: " + request.getManagerId()));
			employee.setManager(manager);
		}

		if (request.getDepartmentId() != null) {
			Department dept = departmentRepository.findById(request.getDepartmentId())
					.orElseThrow(() -> new com.epms.exception.DeparmentNotFoundException("Department not found with ID: " + request.getDepartmentId()));
			employee.setDepartment(dept);
		}

		if (request.getEmployeeStatus() != null) {
			employee.setEmployeeStatus(request.getEmployeeStatus());
		}

		if (request.getResignationDate() != null) {
			employee.setResignationDate(request.getResignationDate());
		}

		log.info("Employee updated successfully. Id: {}, Email: {}", id, employee.getEmail());

		return employeeMapper.entityToDto(employee);
	}

	/**
	 * Update employee profile image
	 */
	@Override
	public void updateEmployeeImage(Long id, MultipartFile file) {

		Employee employee = employeeRepository.findById(id).orElseThrow(() -> new EmployeeNotFoundException(
				messageByLocaleService.getMessage("employee.not.found", new Object[] { id })));

		String newUrl = fileStorageService.updateFile(employee.getProfileImagePath(), file, id);

		/**
		 * Replace old image URL with new one
		 */
		employee.setProfileImagePath(newUrl);

		log.info("Employee image updated. Id: {}, URL: {}", id, newUrl);

	}

	/**
	 * Download employee image from URL. Save it in your system -> Downloads folder
	 */
	@Override
	public void downloadEmployeeImage(Long id) {

		log.info("Start downloading employee image. Id: {}", id);

		Employee employee = employeeRepository.findById(id).orElseThrow(() -> new EmployeeNotFoundException(
				messageByLocaleService.getMessage("employee.not.found", new Object[] { id })));

		String imageUrl = employee.getProfileImagePath();

		if (imageUrl == null || imageUrl.isBlank()) {
			log.warn("No image found for employee. Id: {}", id);
			throw new ValidationException(messageByLocaleService.getMessage("employee.image.not.found", null));
		}

		try {
			HttpClient client = HttpClient.newHttpClient();

			HttpRequest request = HttpRequest.newBuilder().uri(URI.create(imageUrl)).GET().build();

			HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

			if (response.statusCode() != 200) {
				log.error("If not success - failed to download image. Status: {}", response.statusCode());
				throw new ValidationException(
						messageByLocaleService.getMessage("employee.image.download.failed", null));
			}

			String contentType = response.headers().firstValue("Content-Type").orElse("");

			if (!contentType.startsWith("image/")) {
				log.error("Invalid content type: {}", contentType);
				throw new ValidationException(messageByLocaleService.getMessage("file.invalid.type", null));
			}

			/**
			 * Safe extension extraction
			 */
			String cleanUrl = imageUrl.split("\\?")[0];
			int dotIndex = cleanUrl.lastIndexOf(".");
			String extension = (dotIndex != -1) ? cleanUrl.substring(dotIndex) : ".jpg";

			/**
			 * OS independent path
			 */
			String downloadPath = System.getProperty("user.home") + File.separator + "Downloads" + File.separator;

			Path dirPath = Paths.get(downloadPath);

			if (!Files.exists(dirPath)) {
				Files.createDirectories(dirPath);

				log.info("Created download directory: {}", dirPath);
			}

			String fileName = "employee_" + id + extension;

			Path path = dirPath.resolve(fileName);

			/**
			 * try-with-resources (auto close)
			 */
			try (InputStream in = response.body()) {
				Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
			}

			log.info("Image downloaded successfully. Path: {}", path);

		} catch (Exception e) {
			log.error("Failed to download image for employee id: {}", id, e);
			throw new ValidationException(messageByLocaleService.getMessage("employee.image.download.failed", null));
		}
	}

	/**
	 * Retrieves an employee by ID.
	 */
	@Override
	public EmployeeResponse getEmployeeById(Long id) {
		log.debug("Fetching employee by id: {}", id);
		Employee employee = employeeRepository.findById(id).orElseThrow(() -> {
			log.error("Employee not found. Id: {}", id);
			return new EmployeeNotFoundException(
					messageByLocaleService.getMessage("employee.not.found", new Object[] { id }));
		});
		return employeeMapper.entityToDto(employee);
	}

	/**
	 * Retrieves employees based on filter, pagination, and sorting parameters.
	 */
	@Override
	public Page<EmployeeResponse> getAllEmployees(EmployeeSearchRequest request, Pageable pageable) {

		log.info("Fetching employees with filter request: {}, pageable: {}", request, pageable);

		Specification<Employee> spec = EmployeeSpecification.getSpecification(request);
		Page<Employee> employeePage = employeeRepository.findAll(spec, pageable);

		log.debug("Fetched {} employees on page {}", employeePage.getNumberOfElements(), employeePage.getNumber());

		return employeePage.map(employeeMapper::entityToDto);
	}

	/**
	 * Changes the active status of an employee.
	 * 
	 */
	@Override
	public void changeEmployeeStatus(Long id, EmployeeStatus status) {

		log.info("Request received to change employee status, Id: {}, Status: {}", id, status);

		/**
		 * status must not be null
		 */
		if (status == null) {
			log.warn("Status is null for employee id: {}", id);
			throw new ValidationException(messageByLocaleService.getMessage("employee.status.invalid", null));
		}

		Employee employee = employeeRepository.findById(id).orElseThrow(() -> {
			log.error("Employee not found with id: {}", id);
			return new EmployeeNotFoundException(
					messageByLocaleService.getMessage("employee.not.found", new Object[] { id }));
		});

		log.debug("Current status: {}", employee.getEmployeeStatus());

		/**
		 * Already same status
		 */
		final EmployeeStatus currentStatus = employee.getEmployeeStatus();

		if (currentStatus == null) {
			log.error("Employee status is null in DB. Id: {}", id);
			throw new ValidationException(messageByLocaleService.getMessage("employee.corrupted", null));
		}

		if (employee.getDepartment() == null) {
			log.error("Employee department is null. Id: {}", id);
			throw new ValidationException(messageByLocaleService.getMessage("employee.dept.missing", null));
		}

		if (Objects.equals(currentStatus, status)) {
			log.warn("Employee already in requested status. Id: {}", id);
			throw new ValidationException(messageByLocaleService.getMessage("employee.status.same", null));
		}
		/**
		 * Employee cannot be activated if the department is inactive
		 */
		if (EmployeeStatus.ACTIVE.equals(status) && !Boolean.TRUE.equals(employee.getDepartment().getActive())) {
			log.warn("Cannot activate employee because department is inactive. Id: {}", id);
			throw new ValidationException(messageByLocaleService.getMessage("department.inactive", null));
		}

		switch (currentStatus) {

		case TERMINATED:
			if (!EmployeeStatus.TERMINATED.equals(status)) {
				log.warn("No change allowed after termination. Id: {}", id);
				throw new ValidationException(
						messageByLocaleService.getMessage("employee.status.change.not.allowed", null));
			}
			break;
		case RESIGNED:
			if (EmployeeStatus.ACTIVE.equals(status)) {
				log.warn("Cannot reactivate resigned employee. Id: {}", id);
				throw new ValidationException(messageByLocaleService.getMessage("employee.resigned.reactivate", null));
			}
			break;

		default:
			break;

		}

		employee.setEmployeeStatus(status);

		// Look up user by email and enable/disable login access based on the new status
		if (employee.getEmail() != null) {
			userRepository.findByEmailIgnoreCase(employee.getEmail().trim()).ifPresent(user -> {
				if (EmployeeStatus.ACTIVE.equals(status)) {
					user.setEnabled(true);
					log.info("Enabled user login access for email: {}", user.getEmail());
				} else if (EmployeeStatus.INACTIVE.equals(status) || 
				           EmployeeStatus.TERMINATED.equals(status) || 
				           EmployeeStatus.RESIGNED.equals(status)) {
					user.setEnabled(false);
					log.info("Disabled user login access for email: {}", user.getEmail());
				}
				userRepository.save(user);
			});
		}

		log.info("Employee status changed successfully for EmpId={}, From={}, To={}", id, currentStatus, status);

	}

	@Override
	@Transactional(readOnly = true)
	public EmployeeResponse getEmployeeByEmail(String email) {
		log.debug("Fetching employee by email: {}", email);
		Employee employee = employeeRepository.findByEmailIgnoreCase(email)
				.orElseThrow(() -> new EmployeeNotFoundException(
						messageByLocaleService.getMessage("employee.not.found", new Object[] { email })));
		return employeeMapper.entityToDto(employee);
	}

}
