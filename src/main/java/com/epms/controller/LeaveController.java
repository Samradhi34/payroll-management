package com.epms.controller;

import java.util.List;

import java.net.URI;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

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

import com.epms.config.security.UserDetailsImpl;
import com.epms.dto.request.LeaveRequestDto;
import com.epms.dto.response.LeaveBalanceResponseDto;
import com.epms.dto.response.LeaveResponseDto;
import com.epms.dto.response.EmployeeResponse;
import com.epms.service.EmployeeService;
import com.epms.service.LeaveService;
import com.epms.response.GenericResponseHandlers;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/leaves")
@RequiredArgsConstructor
public class LeaveController {

	private final LeaveService leaveService;
	private final EmployeeService employeeService;

	@PostMapping("/apply")
	@PreAuthorize("hasAnyRole('ADMIN', 'HR', 'EMPLOYEE')")
	public ResponseEntity<?> applyLeave(@Valid @RequestBody LeaveRequestDto dto, Authentication authentication) {
		log.info("API request to apply leave for employeeId: {}", dto.getEmployeeId());

		UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
		boolean isEmployee = userDetails.getAuthorities().stream()
				.anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYEE"));

		if (isEmployee) {
			EmployeeResponse emp = employeeService.getEmployeeByEmail(userDetails.getEmail());
			if (!emp.getId().equals(dto.getEmployeeId())) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Employees can only apply leave for themselves.");
			}
		}

		LeaveResponseDto response = leaveService.applyLeave(dto);

		URI location = ServletUriComponentsBuilder
				.fromCurrentContextPath()
				.path("/leaves/{id}")
				.buildAndExpand(response.getId())
				.toUri();

		return ResponseEntity.created(location)
				.body(new GenericResponseHandlers.Builder()
						.setStatus(HttpStatus.CREATED)
						.setMessage("Leave applied successfully")
						.setData(response)
						.create().getBody());
	}

	@PutMapping("/{id}/approve")
	@PreAuthorize("hasAnyRole('ADMIN', 'HR')")
	public ResponseEntity<?> approveLeave(
			@PathVariable @Positive Long id,
			Authentication authentication) {
		log.info("API request to approve leave ID: {}", id);
		UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
		LeaveResponseDto response = leaveService.approveLeave(id, userDetails.getUsername());
		return new GenericResponseHandlers.Builder()
				.setStatus(HttpStatus.OK)
				.setMessage("Leave approved successfully")
				.setData(response)
				.create();
	}

	@PutMapping("/{id}/reject")
	@PreAuthorize("hasAnyRole('ADMIN', 'HR')")
	public ResponseEntity<?> rejectLeave(
			@PathVariable @Positive Long id,
			Authentication authentication) {
		log.info("API request to reject leave ID: {}", id);
		UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
		LeaveResponseDto response = leaveService.rejectLeave(id, userDetails.getUsername());
		return new GenericResponseHandlers.Builder()
				.setStatus(HttpStatus.OK)
				.setMessage("Leave rejected successfully")
				.setData(response)
				.create();
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAnyRole('ADMIN', 'HR', 'EMPLOYEE')")
	public ResponseEntity<?> getLeaveById(@PathVariable @Positive Long id, Authentication authentication) {
		log.info("API request to fetch leave ID: {}", id);
		LeaveResponseDto response = leaveService.getLeaveById(id);

		UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
		boolean isEmployee = userDetails.getAuthorities().stream()
				.anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYEE"));

		if (isEmployee) {
			EmployeeResponse emp = employeeService.getEmployeeByEmail(userDetails.getEmail());
			if (!emp.getId().equals(response.getEmployeeId())) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied.");
			}
		}

		return new GenericResponseHandlers.Builder()
				.setStatus(HttpStatus.OK)
				.setMessage("Leave fetched successfully")
				.setData(response)
				.create();
	}

	@GetMapping("/employee/{employeeId}")
	@PreAuthorize("hasAnyRole('ADMIN', 'HR', 'EMPLOYEE')")
	public ResponseEntity<?> getLeavesByEmployee(
			@PathVariable @Positive Long employeeId,
			Authentication authentication) {
		log.info("API request to list leaves for employeeId: {}", employeeId);

		UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
		boolean isEmployee = userDetails.getAuthorities().stream()
				.anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYEE"));

		if (isEmployee) {
			EmployeeResponse emp = employeeService.getEmployeeByEmail(userDetails.getEmail());
			if (!emp.getId().equals(employeeId)) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied.");
			}
		}

		List<LeaveResponseDto> list = leaveService.getLeavesByEmployee(employeeId);
		return new GenericResponseHandlers.Builder()
				.setStatus(HttpStatus.OK)
				.setMessage("Leaves fetched successfully")
				.setData(list)
				.create();
	}

	@GetMapping("/balances")
	@PreAuthorize("hasAnyRole('ADMIN', 'HR', 'EMPLOYEE')")
	public ResponseEntity<?> getLeaveBalances(
			@RequestParam @Positive Long employeeId,
			@RequestParam @Positive Integer year,
			Authentication authentication) {
		log.info("API request to fetch leave balances for employeeId: {}, year: {}", employeeId, year);

		UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
		boolean isEmployee = userDetails.getAuthorities().stream()
				.anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYEE"));

		if (isEmployee) {
			EmployeeResponse emp = employeeService.getEmployeeByEmail(userDetails.getEmail());
			if (!emp.getId().equals(employeeId)) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied.");
			}
		}

		LeaveBalanceResponseDto balance = leaveService.getLeaveBalance(employeeId, year);
		return new GenericResponseHandlers.Builder()
				.setStatus(HttpStatus.OK)
				.setMessage("Balances fetched successfully")
				.setData(balance)
				.create();
	}
}
