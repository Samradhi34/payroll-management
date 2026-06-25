package com.epms.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.epms.config.security.UserDetailsImpl;
import com.epms.dto.request.AttendanceRequestDto;
import com.epms.dto.request.AttendanceSearchRequest;
import com.epms.dto.response.AttendanceResponseDto;
import com.epms.dto.response.EmployeeResponse;
import com.epms.locale.MessageByLocaleService;
import com.epms.response.GenericResponseHandlers;
import com.epms.service.AttendanceService;
import com.epms.service.EmployeeService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller managing employee attendance.
 * Secured with Spring Security method level security.
 */
@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/attendances")
public class AttendanceController {
	
	private final AttendanceService attendanceService;
	private final MessageByLocaleService messageByLocaleService;
	private final EmployeeService employeeService;
	
	/**
	 * Mark Attendance
	 * Accessible to all authenticated users (ADMIN, HR, EMPLOYEE).
	 */
	@PostMapping("/mark")
	@PreAuthorize("hasAnyRole('ADMIN', 'HR', 'EMPLOYEE')")
	public ResponseEntity<?> markAttendance(
			@Valid @RequestBody AttendanceRequestDto dto) {

		log.info("API request to mark attendance for empId: {}", dto.getEmployeeId());

		attendanceService.markAttendance(dto);

		return new GenericResponseHandlers.Builder()
				.setStatus(HttpStatus.CREATED)
				.setMessage(messageByLocaleService.getMessage("attendance.created",
						new Object[] { dto.getEmployeeId() }))
				.create();
	}

	/**
	 * Get all attendance records with filtering, pagination, and sorting.
	 * Restricted to ADMIN and HR.
	 */
	@GetMapping
	@PreAuthorize("hasAnyRole('ADMIN', 'HR', 'EMPLOYEE')")
	public ResponseEntity<?> getAllAttendance(
			@Valid AttendanceSearchRequest request,
			@PageableDefault(size = 10, sort = "attendanceDate") Pageable pageable,
			Authentication authentication) {

		UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
		boolean isEmployee = userDetails.getAuthorities().stream()
				.anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYEE"));

		if (isEmployee) {
			log.info("Restricting attendance search request to logged-in employee: {}", userDetails.getEmail());
			EmployeeResponse emp = employeeService.getEmployeeByEmail(userDetails.getEmail());
			request.setEmployeeId(emp.getId());
			request.setSearch(null); // Clear search field to prevent query injection
		}

		log.info("API request to list attendance with filters: {}, pageable: {}", request, pageable);

		Page<AttendanceResponseDto> page = attendanceService.getAllAttendance(request, pageable);

		return new GenericResponseHandlers.Builder()
				.setStatus(HttpStatus.OK)
				.setMessage(messageByLocaleService.getMessage("attendance.fetch.all.success", null))
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
	 * Get attendance record by ID.
	 * Accessible to ADMIN, HR, and EMPLOYEE.
	 */
	@GetMapping("/{id}")
	@PreAuthorize("hasAnyRole('ADMIN', 'HR', 'EMPLOYEE')")
	public ResponseEntity<?> getAttendanceById(
			@PathVariable @Positive(message = "Id must be positive") Long id) {

		log.info("API request to fetch attendance ID: {}", id);

		AttendanceResponseDto response = attendanceService.getAttendance(id);

		return new GenericResponseHandlers.Builder()
				.setStatus(HttpStatus.OK)
				.setMessage(messageByLocaleService.getMessage("employee.fetch.success", new Object[] { id }))
				.setData(response)
				.create();
	}

	/**
	 * Update attendance record.
	 * Restricted to ADMIN and HR.
	 */
	@PutMapping("/{id}")
	@PreAuthorize("hasAnyRole('ADMIN', 'HR')")
	public ResponseEntity<?> updateAttendance(
			@PathVariable @Positive(message = "Id must be positive") Long id,
			@Valid @RequestBody AttendanceRequestDto dto) {

		log.info("API request to update attendance ID: {}", id);

		attendanceService.updateAttendance(id, dto);

		return new GenericResponseHandlers.Builder()
				.setStatus(HttpStatus.OK)
				.setMessage(messageByLocaleService.getMessage("employee.update.success", new Object[] { id }))
				.create();
	}

	/**
	 * Upload attendance CSV.
	 * Restricted to ADMIN and HR.
	 */
	@PostMapping(value = "/upload", consumes = "multipart/form-data")
	@PreAuthorize("hasAnyRole('ADMIN', 'HR')")
	public ResponseEntity<?> uploadAttendanceCsv(
			@RequestParam("file") MultipartFile file) {

		log.info("API request to upload attendance CSV: {}", file.getOriginalFilename());

		if (file.isEmpty()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(messageByLocaleService.getMessage("attendance.csv.empty", null));
		}

		attendanceService.uploadAttendanceCsv(file);

		return new GenericResponseHandlers.Builder()
				.setStatus(HttpStatus.OK)
				.setMessage(messageByLocaleService.getMessage("attendance.csv.upload.success", null))
				.create();
	}

	/**
	 * Employee Check-in
	 */
	@PostMapping("/check-in")
	@PreAuthorize("hasAnyRole('ADMIN', 'HR', 'EMPLOYEE')")
	public ResponseEntity<?> checkIn(Authentication authentication) {
		log.info("API request for check-in");
		UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
		EmployeeResponse emp = employeeService.getEmployeeByEmail(userDetails.getEmail());
		attendanceService.checkIn(emp.getId());
		return new GenericResponseHandlers.Builder()
				.setStatus(HttpStatus.OK)
				.setMessage("Check-in successful")
				.create();
	}

	/**
	 * Employee Check-out
	 */
	@PostMapping("/check-out")
	@PreAuthorize("hasAnyRole('ADMIN', 'HR', 'EMPLOYEE')")
	public ResponseEntity<?> checkOut(Authentication authentication) {
		log.info("API request for check-out");
		UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
		EmployeeResponse emp = employeeService.getEmployeeByEmail(userDetails.getEmail());
		attendanceService.checkOut(emp.getId());
		return new GenericResponseHandlers.Builder()
				.setStatus(HttpStatus.OK)
				.setMessage("Check-out successful")
				.create();
	}

	/**
	 * Get today's attendance for the logged-in employee.
	 */
	@GetMapping("/my/today")
	@PreAuthorize("hasAnyRole('ADMIN', 'HR', 'EMPLOYEE')")
	public ResponseEntity<?> getMyTodayAttendance(Authentication authentication) {
		log.info("API request for today's attendance");
		UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
		EmployeeResponse emp = employeeService.getEmployeeByEmail(userDetails.getEmail());
		AttendanceResponseDto response = attendanceService.getTodayAttendance(emp.getId());
		return new GenericResponseHandlers.Builder()
				.setStatus(HttpStatus.OK)
				.setMessage("Today's attendance fetched successfully")
				.setData(response)
				.create();
	}

	/**
	 * Approve Attendance (HR/Admin only)
	 */
	@PutMapping("/{id}/approve")
	@PreAuthorize("hasAnyRole('ADMIN', 'HR')")
	public ResponseEntity<?> approveAttendance(
			@PathVariable @Positive(message = "Id must be positive") Long id,
			Authentication authentication) {
		log.info("API request to approve attendance ID: {}", id);
		UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
		attendanceService.approveAttendance(id, userDetails.getUsername());
		return new GenericResponseHandlers.Builder()
				.setStatus(HttpStatus.OK)
				.setMessage("Attendance approved successfully")
				.create();
	}

}
