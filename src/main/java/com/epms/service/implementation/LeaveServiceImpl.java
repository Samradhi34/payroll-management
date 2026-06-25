package com.epms.service.implementation;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.epms.constant.ApprovalStatus;
import com.epms.constant.AttendanceStatus;
import com.epms.constant.LeaveStatus;
import com.epms.constant.LeaveType;
import com.epms.dto.request.LeaveRequestDto;
import com.epms.dto.response.LeaveBalanceResponseDto;
import com.epms.dto.response.LeaveResponseDto;
import com.epms.entity.Attendance;
import com.epms.entity.Employee;
import com.epms.entity.Holiday;
import com.epms.entity.LeaveBalance;
import com.epms.entity.LeaveRequest;
import com.epms.exception.EmployeeNotFoundException;
import com.epms.exception.ResourceNotFoundException;
import com.epms.locale.MessageByLocaleService;
import com.epms.repository.AttendanceRepository;
import com.epms.repository.EmployeeRepository;
import com.epms.repository.HolidayRepository;
import com.epms.repository.LeaveBalanceRepository;
import com.epms.repository.LeaveRequestRepository;
import com.epms.service.LeaveService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Throwable.class)
public class LeaveServiceImpl implements LeaveService {

	private final LeaveRequestRepository leaveRequestRepository;
	private final LeaveBalanceRepository leaveBalanceRepository;
	private final HolidayRepository holidayRepository;
	private final EmployeeRepository employeeRepository;
	private final AttendanceRepository attendanceRepository;
	private final MessageByLocaleService messageByLocaleService;

	@Override
	public LeaveResponseDto applyLeave(LeaveRequestDto dto) {
		log.info("Applying leave for employeeId: {}, type: {}, start: {}, end: {}",
				dto.getEmployeeId(), dto.getLeaveType(), dto.getStartDate(), dto.getEndDate());

		if (dto.getStartDate().isAfter(dto.getEndDate())) {
			throw new IllegalArgumentException("Start date cannot be after end date");
		}

		if (dto.getIsHalfDay() && !dto.getStartDate().equals(dto.getEndDate())) {
			throw new IllegalArgumentException("Half-day leave must start and end on the same day");
		}

		Employee employee = employeeRepository.findById(dto.getEmployeeId())
				.orElseThrow(() -> new EmployeeNotFoundException(
						messageByLocaleService.getMessage("employee.not.found", new Object[]{dto.getEmployeeId()})));

		/**
		 *  Check for overlapping leave requests
		 */
		List<LeaveRequest> existingLeaves = leaveRequestRepository.findByEmployeeIdOrderByStartDateDesc(dto.getEmployeeId());
		for (LeaveRequest existing : existingLeaves) {
			if (existing.getLeaveStatus() != LeaveStatus.REJECTED && existing.getActive()) {
				boolean overlaps = !(dto.getEndDate().isBefore(existing.getStartDate()) || dto.getStartDate().isAfter(existing.getEndDate()));
				if (overlaps) {
					throw new IllegalStateException("Leave request overlaps with an existing leave request: ID " + existing.getId());
				}
			}
		}

		/**
		 *  Calculate net leave days (excluding weekends and holidays)
		 */
		BigDecimal netDays = calculateNetLeaveDays(dto.getStartDate(), dto.getEndDate(), dto.getIsHalfDay());
		if (netDays.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("Requested leave period consists entirely of weekends or holidays");
		}

		/**
		 *  If it's a paid leave, verify balance first
		 */
		if (dto.getLeaveType() != LeaveType.UNPAID_LEAVE) {
			int year = dto.getStartDate().getYear();
			LeaveBalance balance = getOrCreateLeaveBalance(employee, year);
			BigDecimal currentBalance = getBalanceForType(balance, dto.getLeaveType());
			if (currentBalance.compareTo(netDays) < 0) {
				throw new IllegalStateException("Insufficient leave balance. Available: " + currentBalance + ", Requested: " + netDays);
			}
		}

		LeaveRequest request = new LeaveRequest();
		request.setEmployee(employee);
		request.setLeaveType(dto.getLeaveType());
		request.setStartDate(dto.getStartDate());
		request.setEndDate(dto.getEndDate());
		request.setLeaveStatus(LeaveStatus.PENDING);
		request.setReason(dto.getReason());
		request.setIsHalfDay(dto.getIsHalfDay());
		request.setActive(true);

		LeaveRequest saved = leaveRequestRepository.save(request);
		log.info("Leave applied successfully with ID: {}", saved.getId());

		return mapToResponseDto(saved);
	}

	@Override
	public LeaveResponseDto approveLeave(Long leaveId, String approvedBy) {
		log.info("Approving leave ID: {} by {}", leaveId, approvedBy);

		LeaveRequest request = leaveRequestRepository.findById(leaveId)
				.orElseThrow(() -> new ResourceNotFoundException("Leave request not found with ID: " + leaveId));

		if (request.getLeaveStatus() != LeaveStatus.PENDING) {
			throw new IllegalStateException("Leave request is not in PENDING status");
		}

		Employee employee = request.getEmployee();
		BigDecimal netDays = calculateNetLeaveDays(request.getStartDate(), request.getEndDate(), request.getIsHalfDay());

		/**
		 *  Deduct leave balance for paid leaves
		 */
		if (request.getLeaveType() != LeaveType.UNPAID_LEAVE) {
			int year = request.getStartDate().getYear();
			LeaveBalance balance = getOrCreateLeaveBalance(employee, year);
			deductLeaveBalance(balance, request.getLeaveType(), netDays);
			leaveBalanceRepository.save(balance);
		}

		request.setLeaveStatus(LeaveStatus.APPROVED);
		request.setApprovedBy(approvedBy);
		LeaveRequest updated = leaveRequestRepository.save(request);

		/**
		 *  Auto-populate attendance records
		 */
		createAttendanceForApprovedLeave(request);

		log.info("Leave ID: {} approved successfully", leaveId);
		return mapToResponseDto(updated);
	}

	@Override
	public LeaveResponseDto rejectLeave(Long leaveId, String approvedBy) {
		log.info("Rejecting leave ID: {} by {}", leaveId, approvedBy);

		LeaveRequest request = leaveRequestRepository.findById(leaveId)
				.orElseThrow(() -> new ResourceNotFoundException("Leave request not found with ID: " + leaveId));

		if (request.getLeaveStatus() != LeaveStatus.PENDING) {
			throw new IllegalStateException("Leave request is not in PENDING status");
		}

		request.setLeaveStatus(LeaveStatus.REJECTED);
		request.setApprovedBy(approvedBy);
		LeaveRequest updated = leaveRequestRepository.save(request);

		log.info("Leave ID: {} rejected successfully", leaveId);
		return mapToResponseDto(updated);
	}

	@Override
	@Transactional(readOnly = true)
	public LeaveResponseDto getLeaveById(Long leaveId) {
		LeaveRequest request = leaveRequestRepository.findById(leaveId)
				.orElseThrow(() -> new ResourceNotFoundException("Leave request not found with ID: " + leaveId));
		return mapToResponseDto(request);
	}

	@Override
	@Transactional(readOnly = true)
	public List<LeaveResponseDto> getLeavesByEmployee(Long employeeId) {
		if (!employeeRepository.existsById(employeeId)) {
			throw new EmployeeNotFoundException(
					messageByLocaleService.getMessage("employee.not.found", new Object[]{employeeId}));
		}
		List<LeaveRequest> requests = leaveRequestRepository.findByEmployeeIdOrderByStartDateDesc(employeeId);
		return requests.stream().map(this::mapToResponseDto).toList();
	}

	@Override
	public LeaveBalanceResponseDto getLeaveBalance(Long employeeId, Integer year) {
		Employee employee = employeeRepository.findById(employeeId)
				.orElseThrow(() -> new EmployeeNotFoundException(
						messageByLocaleService.getMessage("employee.not.found", new Object[]{employeeId})));

		LeaveBalance balance = getOrCreateLeaveBalance(employee, year);
		return mapToBalanceResponseDto(balance);
	}

	@Override
	public void carryForwardEarnedLeaves(Long employeeId, Integer newYear) {
		log.info("Carrying forward Earned Leaves for employeeId: {} to year: {}", employeeId, newYear);
		Employee employee = employeeRepository.findById(employeeId)
				.orElseThrow(() -> new EmployeeNotFoundException(
						messageByLocaleService.getMessage("employee.not.found", new Object[]{employeeId})));

		int lastYear = newYear - 1;
		LeaveBalance lastYearBalance = getOrCreateLeaveBalance(employee, lastYear);
		LeaveBalance newYearBalance = getOrCreateLeaveBalance(employee, newYear);

		/**
		 *  Carry forward Earned Leaves: max 15 days, max accumulated 30 days
		 */
		BigDecimal cfEl = lastYearBalance.getEarnedLeaveBalance();
		if (cfEl.compareTo(BigDecimal.valueOf(15.0)) > 0) {
			cfEl = BigDecimal.valueOf(15.0);
		}

		BigDecimal baseNewEl = BigDecimal.valueOf(15.0); // standard allocation for the new year
		BigDecimal totalNewEl = baseNewEl.add(cfEl);
		if (totalNewEl.compareTo(BigDecimal.valueOf(30.0)) > 0) {
			totalNewEl = BigDecimal.valueOf(30.0);
		}

		newYearBalance.setEarnedLeaveBalance(totalNewEl);
		newYearBalance.setCasualLeaveBalance(BigDecimal.valueOf(12.0)); // CL reset
		newYearBalance.setSickLeaveBalance(BigDecimal.valueOf(10.0));   // SL reset

		leaveBalanceRepository.save(newYearBalance);
		log.info("Earned Leaves carried forward successfully. New EL Balance: {}", totalNewEl);
	}


	private BigDecimal calculateNetLeaveDays(LocalDate start, LocalDate end, boolean isHalfDay) {
		if (isHalfDay) {
			return BigDecimal.valueOf(0.5);
		}

		long totalDays = ChronoUnit.DAYS.between(start, end) + 1;
		long weekendAndHolidays = 0;

		List<Holiday> holidays = holidayRepository.findByHolidayDateBetween(start, end);
		List<LocalDate> holidayDates = holidays.stream().map(Holiday::getHolidayDate).toList();

		for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
			DayOfWeek day = date.getDayOfWeek();
			boolean isWeekend = (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY);
			boolean isHoliday = holidayDates.contains(date);

			if (isWeekend || isHoliday) {
				weekendAndHolidays++;
			}
		}

		return BigDecimal.valueOf(totalDays - weekendAndHolidays);
	}

	private LeaveBalance getOrCreateLeaveBalance(Employee employee, int year) {
		return leaveBalanceRepository.findByEmployeeIdAndYear(employee.getId(), year)
				.orElseGet(() -> {
					LeaveBalance newBalance = new LeaveBalance();
					newBalance.setEmployee(employee);
					newBalance.setYear(year);
					newBalance.setCasualLeaveBalance(BigDecimal.valueOf(12.0));
					newBalance.setSickLeaveBalance(BigDecimal.valueOf(10.0));
					newBalance.setEarnedLeaveBalance(BigDecimal.valueOf(15.0));
					newBalance.setActive(true);
					return leaveBalanceRepository.save(newBalance);
				});
	}

	private BigDecimal getBalanceForType(LeaveBalance balance, LeaveType type) {
		return switch (type) {
			case CASUAL_LEAVE -> balance.getCasualLeaveBalance();
			case SICK_LEAVE -> balance.getSickLeaveBalance();
			case EARNED_LEAVE -> balance.getEarnedLeaveBalance();
			default -> BigDecimal.ZERO;
		};
	}

	private void deductLeaveBalance(LeaveBalance balance, LeaveType type, BigDecimal amount) {
		switch (type) {
			case CASUAL_LEAVE -> balance.setCasualLeaveBalance(balance.getCasualLeaveBalance().subtract(amount));
			case SICK_LEAVE -> balance.setSickLeaveBalance(balance.getSickLeaveBalance().subtract(amount));
			case EARNED_LEAVE -> balance.setEarnedLeaveBalance(balance.getEarnedLeaveBalance().subtract(amount));
			default -> {}
		}
	}

	private void createAttendanceForApprovedLeave(LeaveRequest request) {
		LocalDate start = request.getStartDate();
		LocalDate end = request.getEndDate();

		List<Holiday> holidays = holidayRepository.findByHolidayDateBetween(start, end);
		List<LocalDate> holidayDates = holidays.stream().map(Holiday::getHolidayDate).toList();

		AttendanceStatus status = (request.getLeaveType() == LeaveType.UNPAID_LEAVE) 
				? AttendanceStatus.UNPAID_LEAVE : AttendanceStatus.PAID_LEAVE;

		for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
			DayOfWeek day = date.getDayOfWeek();
			boolean isWeekend = (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY);
			boolean isHoliday = holidayDates.contains(date);

			if (isWeekend || isHoliday) {
				continue; // Skip weekends/holidays (already Week Off / Holiday status)
			}

			/**
			 *  Upsert daily attendance log
			 */
			final LocalDate targetDate = date;
			Attendance attendance = attendanceRepository.findByEmployeeIdAndAttendanceDate(request.getEmployee().getId(), targetDate)
					.orElseGet(() -> {
						Attendance newAtt = new Attendance();
						newAtt.setEmployee(request.getEmployee());
						newAtt.setAttendanceDate(targetDate);
						newAtt.setActive(true);
						return newAtt;
					});

			if (request.getIsHalfDay()) {
				attendance.setAttendanceStatus(AttendanceStatus.HALF_DAY);
				attendance.setWorkingHours(BigDecimal.valueOf(4.0));
				attendance.setRemarks("Half day approved leave: " + request.getLeaveType().name());
			} else {
				attendance.setAttendanceStatus(status);
				attendance.setWorkingHours(BigDecimal.ZERO);
				attendance.setRemarks("Full day approved leave: " + request.getLeaveType().name());
			}
			attendance.setApprovedBy(request.getApprovedBy());
			attendance.setApprovalStatus(ApprovalStatus.APPROVED);
			attendanceRepository.save(attendance);
		}
	}

	private LeaveResponseDto mapToResponseDto(LeaveRequest entity) {
		LeaveResponseDto dto = new LeaveResponseDto();
		dto.setId(entity.getId());
		dto.setEmployeeId(entity.getEmployee().getId());
		dto.setEmployeeName(entity.getEmployee().getFirstName() + " " + entity.getEmployee().getLastName());
		dto.setLeaveType(entity.getLeaveType());
		dto.setStartDate(entity.getStartDate());
		dto.setEndDate(entity.getEndDate());
		dto.setLeaveStatus(entity.getLeaveStatus());
		dto.setReason(entity.getReason());
		dto.setApprovedBy(entity.getApprovedBy());
		dto.setIsHalfDay(entity.getIsHalfDay());
		return dto;
	}

	private LeaveBalanceResponseDto mapToBalanceResponseDto(LeaveBalance entity) {
		LeaveBalanceResponseDto dto = new LeaveBalanceResponseDto();
		dto.setId(entity.getId());
		dto.setEmployeeId(entity.getEmployee().getId());
		dto.setEmployeeName(entity.getEmployee().getFirstName() + " " + entity.getEmployee().getLastName());
		dto.setYear(entity.getYear());
		dto.setCasualLeaveBalance(entity.getCasualLeaveBalance());
		dto.setSickLeaveBalance(entity.getSickLeaveBalance());
		dto.setEarnedLeaveBalance(entity.getEarnedLeaveBalance());
		return dto;
	}
}
