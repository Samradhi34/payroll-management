package com.epms.controller;

import java.io.IOException;
import java.net.URI;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import com.epms.config.security.UserDetailsImpl;
import com.epms.dto.response.EmployeeResponse;
import com.epms.service.EmployeeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import com.epms.dto.request.BulkPayrollRequest;
import com.epms.dto.request.PayrollSearchRequest;

import com.epms.dto.request.PayrollGenerateRequest;
import com.epms.dto.response.BulkPayrollResponse;
import com.epms.dto.response.PayrollResponse;
import com.epms.dto.response.SalarySlipResponse;
import com.epms.locale.MessageByLocaleService;
import com.epms.response.GenericResponseHandlers;
import com.epms.service.PayrollService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST Controller for Payroll and SalarySlip operations.
 *
 * <p>Exposes endpoints to manage the full payroll lifecycle:
 * <ul>
 *   <li>Generate a new payroll record</li>
 *   <li>Approve, pay, and cancel payrolls</li>
 *   <li>Query payrolls by ID, employee, or month/year</li>
 *   <li>Generate and retrieve salary slips</li>
 * </ul>
 *
 * <p><b>Role-Based Access Control (RBAC):</b>
 * <ul>
 *   <li>{@code ADMIN} — Full access to all endpoints</li>
 *   <li>{@code HR} — Can generate payrolls, read payrolls, generate and read salary slips</li>
 *   <li>{@code EMPLOYEE} — Can only read their own payrolls and salary slips</li>
 * </ul>
 *
 * <p>Base path: {@code /payrolls}
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/payrolls")
public class PayrollController {

	private final PayrollService payrollService;
	private final MessageByLocaleService messageByLocaleService;
	private final EmployeeService employeeService;

	@Value("${app.salary-slip.storage-dir:salary-slips}")
	private String storageDir;

	/**
	 * Generate a new payroll for an employee.
	 *
	 * <p>Validates employee is ACTIVE, prevents duplicate payrolls for the same period,
	 * and calculates net salary automatically.
	 *
	 * <p>Access: ADMIN, HR
	 *
	 * @param request the payroll generation request body (validated)
	 * @return 201 Created with the generated payroll
	 */
	@PostMapping
	@PreAuthorize("hasAnyRole('ADMIN', 'HR')")
	public ResponseEntity<?> generatePayroll(@Valid @RequestBody PayrollGenerateRequest request) {
		log.info("[PayrollController] generatePayroll() - employeeId={}, month={}, year={}",
				request.getEmployeeId(), request.getMonth(), request.getYear());

		PayrollResponse response = payrollService.generatePayroll(request);

		URI location = ServletUriComponentsBuilder
				.fromCurrentContextPath()
				.path("/payrolls/{id}")
				.buildAndExpand(response.getId())
				.toUri();

		return ResponseEntity.created(location)
				.body(new GenericResponseHandlers.Builder()
						.setStatus(HttpStatus.CREATED)
						.setMessage(messageByLocaleService.getMessage("payroll.generated.success", null))
						.setData(response)
						.create().getBody());
	}

	/**
	 * Generate payroll for all active employees for a given month and year.
	 *
	 * <p>Skips employees who already have payroll for the period.
	 * Returns a summary of generated, skipped, and failed counts.
	 *
	 * <p>Access: ADMIN, HR
	 */
	@PostMapping("/generate-all")
	@PreAuthorize("hasAnyRole('ADMIN', 'HR')")
	public ResponseEntity<?> generatePayrollForAll(@Valid @RequestBody BulkPayrollRequest request) {
		log.info("[PayrollController] generatePayrollForAll() - month={}, year={}",
				request.getMonth(), request.getYear());

		BulkPayrollResponse response = payrollService.generatePayrollForAll(request);

		return new GenericResponseHandlers.Builder()
				.setStatus(HttpStatus.OK)
				.setMessage(String.format("Bulk payroll complete: %d generated, %d skipped, %d failed",
						response.getGenerated(), response.getSkipped(), response.getFailed()))
				.setData(response)
				.create();
	}

	

	/**
	 * Approve a GENERATED payroll (moves status to APPROVED).
	 *
	 * <p>Access: ADMIN only
	 *
	 * @param id the payroll ID (must be positive)
	 * @return 200 OK with the updated payroll
	 */
	@PatchMapping("/{id}/approve")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> approvePayroll(
			@PathVariable @Positive(message = "Payroll ID must be a positive number") Long id) {

		log.info("[PayrollController] approvePayroll() - payrollId={}", id);

		PayrollResponse response = payrollService.approvePayroll(id);

		return new GenericResponseHandlers.Builder()
				.setStatus(HttpStatus.OK)
				.setMessage(messageByLocaleService.getMessage("payroll.approved.success", new Object[]{id}))
				.setData(response)
				.create();
	}

	/**
	 * Mark an APPROVED payroll as PAID (salary has been disbursed).
	 *
	 * <p>Access: ADMIN only
	 *
	 * @param id the payroll ID (must be positive)
	 * @return 200 OK with the updated payroll
	 */
	@PatchMapping("/{id}/pay")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> markPayrollAsPaid(
			@PathVariable @Positive(message = "Payroll ID must be a positive number") Long id) {

		log.info("[PayrollController] markPayrollAsPaid() - payrollId={}", id);

		PayrollResponse response = payrollService.markPayrollAsPaid(id);

		return new GenericResponseHandlers.Builder()
				.setStatus(HttpStatus.OK)
				.setMessage(messageByLocaleService.getMessage("payroll.paid.success", new Object[]{id}))
				.setData(response)
				.create();
	}

	/**
	 * Cancel a GENERATED or APPROVED payroll.
	 *
	 * <p>PAID payrolls cannot be cancelled as they represent completed financial transactions.
	 *
	 * <p>Access: ADMIN only
	 *
	 * @param id the payroll ID (must be positive)
	 * @return 200 OK with the updated payroll
	 */
	@PatchMapping("/{id}/cancel")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> cancelPayroll(
			@PathVariable @Positive(message = "Payroll ID must be a positive number") Long id) {

		log.info("[PayrollController] cancelPayroll() - payrollId={}", id);

		PayrollResponse response = payrollService.cancelPayroll(id);

		return new GenericResponseHandlers.Builder()
				.setStatus(HttpStatus.OK)
				.setMessage(messageByLocaleService.getMessage("payroll.cancelled.success", new Object[]{id}))
				.setData(response)
				.create();
	}

	

	/**
	 * Retrieve a specific payroll record by its ID.
	 *
	 * <p>Access: ADMIN, HR, EMPLOYEE (own payrolls only)
	 *
	 * @param id the payroll ID (must be positive)
	 * @return 200 OK with the payroll data
	 */
	@GetMapping("/{id}")
	@PreAuthorize("hasAnyRole('ADMIN', 'HR', 'EMPLOYEE')")
	public ResponseEntity<?> getPayrollById(
			@PathVariable @Positive(message = "Payroll ID must be a positive number") Long id,
			Authentication authentication) {

		log.debug("[PayrollController] getPayrollById() - payrollId={}", id);

		PayrollResponse response = payrollService.getPayrollById(id);

		ResponseEntity<?> denied = verifyEmployeeOwnsPayroll(response.getEmployeeId(), authentication);
		if (denied != null) {
			return denied;
		}

		return new GenericResponseHandlers.Builder()
				.setStatus(HttpStatus.OK)
				.setMessage(messageByLocaleService.getMessage("payroll.fetch.success", new Object[]{id}))
				.setData(response)
				.create();
	}

	/**
	 * Retrieve all payroll records in the system.
	 *
	 * <p>Access: ADMIN, HR
	 *
	 * @return 200 OK with list of all payrolls
	 */
	@GetMapping
	@PreAuthorize("hasAnyRole('ADMIN', 'HR')")
	public ResponseEntity<?> getAllPayrolls(
			@Valid PayrollSearchRequest request,
			@PageableDefault(size = 10, sort = "id") Pageable pageable) {
		log.info("[PayrollController] getAllPayrolls() matching criteria: {}, pageable: {}", request, pageable);

		Page<PayrollResponse> page = payrollService.getAllPayrolls(request, pageable);

		return new GenericResponseHandlers.Builder()
				.setStatus(HttpStatus.OK)
				.setMessage(messageByLocaleService.getMessage("payroll.fetch.all.success", null))
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
	 * Retrieve all payrolls for a specific employee, ordered from most recent to oldest.
	 *
	 * <p>An EMPLOYEE can only view their own payrolls — enforce this check at the service or
	 * via Spring Security expression {@code @PostAuthorize} if needed for finer-grained control.
	 *
	 * <p>Access: ADMIN, HR, EMPLOYEE
	 *
	 * @param employeeId the employee's primary key (must be positive)
	 * @return 200 OK with the employee's payroll list
	 */
	@GetMapping("/employee/{employeeId}")
	@PreAuthorize("hasAnyRole('ADMIN', 'HR', 'EMPLOYEE')")
	public ResponseEntity<?> getPayrollsByEmployee(
			@PathVariable @Positive(message = "Employee ID must be a positive number") Long employeeId,
			Authentication authentication) {

		UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
		boolean isEmployee = userDetails.getAuthorities().stream()
				.anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYEE"));

		if (isEmployee) {
			ResponseEntity<?> denied = verifyEmployeeOwnsPayroll(employeeId, authentication);
			if (denied != null) {
				return denied;
			}
		}

		log.debug("[PayrollController] getPayrollsByEmployee() - employeeId={}", employeeId);

		List<PayrollResponse> response = payrollService.getPayrollsByEmployee(employeeId);

		return new GenericResponseHandlers.Builder()
				.setStatus(HttpStatus.OK)
				.setMessage(messageByLocaleService.getMessage("payroll.fetch.employee.success",
						new Object[]{employeeId}))
				.setData(response)
				.create();
	}

	/**
	 * Retrieve all payrolls generated for a specific month and year (payroll run view).
	 *
	 * <p>Access: ADMIN, HR
	 *
	 * @param month the payroll month (1-12)
	 * @param year  the payroll year (2000+)
	 * @return 200 OK with the list of payrolls
	 */
	@GetMapping("/month-year")
	@PreAuthorize("hasAnyRole('ADMIN', 'HR')")
	public ResponseEntity<?> getPayrollsByMonthAndYear(
			@RequestParam @Min(value = 1, message = "Month must be between 1 and 12")
			@Max(value = 12, message = "Month must be between 1 and 12") Integer month,
			@RequestParam @Min(value = 2000, message = "Year must be 2000 or later") Integer year) {

		log.debug("[PayrollController] getPayrollsByMonthAndYear() - month={}, year={}", month, year);

		List<PayrollResponse> response = payrollService.getPayrollsByMonthAndYear(month, year);

		return new GenericResponseHandlers.Builder()
				.setStatus(HttpStatus.OK)
				.setMessage(messageByLocaleService.getMessage("payroll.fetch.month.year.success",
						new Object[]{month, year}))
				.setData(response)
				.create();
	}


	/**
	 * Generate a salary slip for a specific payroll record.
	 *
	 * <p>The payroll must be in APPROVED or PAID status. Only one slip can be
	 * generated per payroll.
	 *
	 * <p>Access: ADMIN, HR
	 *
	 * @param id the payroll ID (must be positive)
	 * @return 201 Created with the generated salary slip
	 */
	@PostMapping("/{id}/salary-slip")
	@PreAuthorize("hasAnyRole('ADMIN', 'HR')")
	public ResponseEntity<?> generateSalarySlip(
			@PathVariable @Positive(message = "Payroll ID must be a positive number") Long id) {

		log.info("[PayrollController] generateSalarySlip() - payrollId={}", id);

		SalarySlipResponse response = payrollService.generateSalarySlip(id);

		URI location = ServletUriComponentsBuilder
				.fromCurrentContextPath()
				.path("/payrolls/{id}/salary-slip")
				.buildAndExpand(id)
				.toUri();

		return ResponseEntity.created(location)
				.body(new GenericResponseHandlers.Builder()
						.setStatus(HttpStatus.CREATED)
						.setMessage(messageByLocaleService.getMessage("salary.slip.generated.success",
								new Object[]{id}))
						.setData(response)
						.create().getBody());
	}

	/**
	 * Retrieve the salary slip linked to a specific payroll.
	 *
	 * <p>Access: ADMIN, HR, EMPLOYEE
	 *
	 * @param id the payroll ID (must be positive)
	 * @return 200 OK with the salary slip data
	 */
	@GetMapping("/{id}/salary-slip")
	@PreAuthorize("hasAnyRole('ADMIN', 'HR', 'EMPLOYEE')")
	public ResponseEntity<?> getSalarySlipByPayroll(
			@PathVariable @Positive(message = "Payroll ID must be a positive number") Long id,
			Authentication authentication) {

		log.debug("[PayrollController] getSalarySlipByPayroll() - payrollId={}", id);

		PayrollResponse payroll = payrollService.getPayrollById(id);
		ResponseEntity<?> denied = verifyEmployeeOwnsPayroll(payroll.getEmployeeId(), authentication);
		if (denied != null) {
			return denied;
		}

		SalarySlipResponse response = payrollService.getSalarySlipByPayroll(id);

		return new GenericResponseHandlers.Builder()
				.setStatus(HttpStatus.OK)
				.setMessage(messageByLocaleService.getMessage("salary.slip.fetch.success",
						new Object[]{id}))
				.setData(response)
				.create();
	}

	/**
	 * List all salary slips generated for an employee across all payroll periods.
	 *
	 * <p>Access: ADMIN, HR, EMPLOYEE
	 *
	 * @param employeeId the employee's primary key (must be positive)
	 * @return 200 OK with list of salary slips
	 */
	@GetMapping("/employee/{employeeId}/salary-slips")
	@PreAuthorize("hasAnyRole('ADMIN', 'HR', 'EMPLOYEE')")
	public ResponseEntity<?> getSalarySlipsByEmployee(
			@PathVariable @Positive(message = "Employee ID must be a positive number") Long employeeId,
			Authentication authentication) {

		ResponseEntity<?> denied = verifyEmployeeOwnsPayroll(employeeId, authentication);
		if (denied != null) {
			return denied;
		}

		log.debug("[PayrollController] getSalarySlipsByEmployee() - employeeId={}", employeeId);

		List<SalarySlipResponse> response = payrollService.getSalarySlipsByEmployee(employeeId);

		return new GenericResponseHandlers.Builder()
				.setStatus(HttpStatus.OK)
				.setMessage(messageByLocaleService.getMessage("salary.slip.fetch.employee.success",
						new Object[]{employeeId}))
				.setData(response)
				.create();
	}

	/**
	 * Download the PDF salary slip file for a given payroll.
	 *
	 * <p>Access: ADMIN, HR, EMPLOYEE
	 *
	 * @param id the payroll ID (must be positive)
	 * @return the PDF as a downloadable attachment
	 */
	@GetMapping("/{id}/salary-slip/download")
	@PreAuthorize("hasAnyRole('ADMIN', 'HR', 'EMPLOYEE')")
	public ResponseEntity<Resource> downloadSalarySlip(
			@PathVariable @jakarta.validation.constraints.Positive Long id,
			Authentication authentication) {

		log.info("[PayrollController] downloadSalarySlip() - payrollId={}", id);

		PayrollResponse payroll = payrollService.getPayrollById(id);
		ResponseEntity<?> denied = verifyEmployeeOwnsPayroll(payroll.getEmployeeId(), authentication);
		if (denied != null) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}

		// Fetch slip metadata from DB to get the stored relative path; generate it on-the-fly if missing
		SalarySlipResponse slip;
		try {
			slip = payrollService.getSalarySlipByPayroll(id);
		} catch (com.epms.exception.SalarySlipNotFoundException e) {
			log.info("[PayrollController] downloadSalarySlip() - slip not found, generating on the fly for payrollId={}", id);
			slip = payrollService.generateSalarySlip(id);
		}

		Path filePath = Paths.get(storageDir).resolve(slip.getSlipPath()).toAbsolutePath();

		if (!Files.exists(filePath)) {
			log.warn("[PayrollController] downloadSalarySlip() - file not found on disk: {}", filePath);
			return ResponseEntity.notFound().build();
		}

		try {
			byte[] bytes = Files.readAllBytes(filePath);
			String filename = "salary-slip-" + id + ".pdf";

			return ResponseEntity.ok()
					.contentType(MediaType.APPLICATION_PDF)
					.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
					.contentLength(bytes.length)
					.body(new ByteArrayResource(bytes));

		} catch (IOException e) {
			log.error("[PayrollController] downloadSalarySlip() - read error for payrollId={}", id, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	/**
	 * Ensures an EMPLOYEE user can only access their own payroll records.
	 *
	 * @return a 403 response when access is denied, or {@code null} when allowed
	 */
	private ResponseEntity<?> verifyEmployeeOwnsPayroll(Long payrollEmployeeId, Authentication authentication) {
		UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
		boolean isEmployee = userDetails.getAuthorities().stream()
				.anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYEE"));

		if (!isEmployee) {
			return null;
		}

		EmployeeResponse emp = employeeService.getEmployeeByEmail(userDetails.getEmail());
		if (!emp.getId().equals(payrollEmployeeId)) {
			log.warn("Access denied: User {} tried to access payroll of employee ID {}",
					userDetails.getEmail(), payrollEmployeeId);
			return ResponseEntity.status(HttpStatus.FORBIDDEN)
					.body("Access denied: You do not have permission to access this payroll record.");
		}

		return null;
	}
}
