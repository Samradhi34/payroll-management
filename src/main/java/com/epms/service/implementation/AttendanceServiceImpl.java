package com.epms.service.implementation;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.epms.constant.ApprovalStatus;
import com.epms.constant.AttendanceStatus;
import com.epms.constant.EmployeeStatus;
import com.epms.dto.request.AttendanceRequestDto;
import com.epms.dto.request.AttendanceSearchRequest;
import com.epms.dto.response.AttendanceResponseDto;
import com.epms.entity.Attendance;
import com.epms.entity.Employee;
import com.epms.exception.ResourceNotFoundException;
import com.epms.locale.MessageByLocaleService;
import com.epms.mapper.AttendanceMapper;
import com.epms.repository.AttendanceRepository;
import com.epms.repository.EmployeeRepository;
import com.epms.service.AttendanceService;
import com.epms.specification.AttendanceSpecification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Throwable.class)
public class AttendanceServiceImpl implements AttendanceService {

	private final AttendanceRepository attendanceRepository;
	private final AttendanceMapper attendanceMapper;
	private final EmployeeRepository employeeRepository;
	private final MessageByLocaleService messageByLocaleService;

	@Override
	public void checkIn(Long employeeId) {
		LocalDate today = LocalDate.now();
		log.info("Checking in employeeId: {} on date: {}", employeeId, today);

		Employee employee = employeeRepository.findById(employeeId)
				.orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + employeeId));

		if (employee.getEmployeeStatus() != EmployeeStatus.ACTIVE) {
			throw new IllegalStateException("Employee is not active");
		}

		attendanceRepository.findByEmployeeIdAndAttendanceDate(employeeId, today)
				.ifPresent(a -> {
					if (a.getCheckInTime() != null) {
						throw new IllegalStateException("Employee is already checked in for today");
					}
				});

		Attendance attendance = attendanceRepository.findByEmployeeIdAndAttendanceDate(employeeId, today)
				.orElseGet(() -> {
					Attendance newAtt = new Attendance();
					newAtt.setEmployee(employee);
					newAtt.setAttendanceDate(today);
					newAtt.setActive(true);
					return newAtt;
				});

		attendance.setCheckInTime(LocalTime.now());
		attendance.setWorkingHours(BigDecimal.ZERO);
		attendance.setAttendanceStatus(AttendanceStatus.PRESENT);
		attendance.setApprovalStatus(ApprovalStatus.APPROVED);
		attendanceRepository.save(attendance);
		log.info("Checked in employeeId: {} successfully at {}", employeeId, attendance.getCheckInTime());
	}

	@Override
	public void checkOut(Long employeeId) {
		LocalDate today = LocalDate.now();
		log.info("Checking out employeeId: {} on date: {}", employeeId, today);

		Attendance attendance = attendanceRepository.findByEmployeeIdAndAttendanceDate(employeeId, today)
				.orElseThrow(() -> new IllegalStateException("No check-in record found for today"));

		if (attendance.getCheckInTime() == null) {
			throw new IllegalStateException("No check-in time found for today");
		}

		if (attendance.getCheckOutTime() != null) {
			throw new IllegalStateException("Employee is already checked out for today");
		}

		LocalTime checkout = LocalTime.now();
		attendance.setCheckOutTime(checkout);

		// Compute working hours
		Duration duration = Duration.between(attendance.getCheckInTime(), checkout);
		double hours = duration.toMinutes() / 60.0;
		BigDecimal workingHours = BigDecimal.valueOf(hours).setScale(2, RoundingMode.HALF_UP);
		attendance.setWorkingHours(workingHours);

		// Resolve status based on configurable rules:
		// >= 8 hours -> PRESENT
		// 4 to 8 hours -> HALF_DAY
		// < 4 hours -> ABSENT
		if (workingHours.compareTo(BigDecimal.valueOf(8.0)) >= 0) {
			attendance.setAttendanceStatus(AttendanceStatus.PRESENT);
		} else if (workingHours.compareTo(BigDecimal.valueOf(4.0)) >= 0) {
			attendance.setAttendanceStatus(AttendanceStatus.HALF_DAY);
		} else {
			attendance.setAttendanceStatus(AttendanceStatus.ABSENT);
		}

		attendanceRepository.save(attendance);
		log.info("Checked out employeeId: {} successfully. Hours: {}, Status: {}", employeeId, workingHours, attendance.getAttendanceStatus());
	}

	@Override
	public void approveAttendance(Long id, String approvedBy) {
		log.info("Approving attendance record ID: {} by {}", id, approvedBy);
		Attendance attendance = attendanceRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Attendance record not found with ID: " + id));

		attendance.setApprovalStatus(ApprovalStatus.APPROVED);
		attendance.setApprovedBy(approvedBy);
		attendanceRepository.save(attendance);
		log.info("Attendance record ID: {} approved successfully", id);
	}

	@Override
	public void markAttendance(AttendanceRequestDto dto) {
		log.info("Marking attendance for empId: {} date: {}", dto.getEmployeeId(), dto.getAttendanceDate());

		if (dto.getEmployeeId() == null) {
			throw new IllegalArgumentException(messageByLocaleService.getMessage("attendance.employee.id.required", null));
		}

		if (dto.getAttendanceDate().isAfter(LocalDate.now())) {
			throw new IllegalArgumentException(messageByLocaleService.getMessage("attendance.date.invalid", null));
		}

		Employee emp = employeeRepository.findById(dto.getEmployeeId()).orElseThrow(() -> {
			return new ResourceNotFoundException(
					messageByLocaleService.getMessage("employee.not.found", new Object[] { dto.getEmployeeId() }));
		});

		if (emp.getEmployeeStatus() != EmployeeStatus.ACTIVE) {
			throw new IllegalStateException(messageByLocaleService.getMessage("attendance.employee.not.active", null));
		}

		// Prevent duplicate manual marking
		attendanceRepository.findByEmployeeIdAndAttendanceDate(dto.getEmployeeId(), dto.getAttendanceDate())
				.ifPresent(a -> {
					throw new IllegalStateException(messageByLocaleService.getMessage("attendance.already.exists",
							new Object[] { dto.getEmployeeId(), dto.getAttendanceDate() }));
				});

		validateAttendanceBusinessRules(dto);

		Attendance entity = attendanceMapper.dtoToEntity(dto, emp);
		if (entity.getApprovalStatus() == null) {
			entity.setApprovalStatus(ApprovalStatus.APPROVED); // Default for manual marking
		}
		if (dto.getWorkingHours() == null) {
			// If not specified, default to 8.0 for PRESENT/WFH, 4.0 for HALF_DAY, 0 for ABSENT/Leaves
			if (dto.getAttendanceStatus() == AttendanceStatus.PRESENT || dto.getAttendanceStatus() == AttendanceStatus.WORK_FROM_HOME) {
				entity.setWorkingHours(BigDecimal.valueOf(8.0));
			} else if (dto.getAttendanceStatus() == AttendanceStatus.HALF_DAY) {
				entity.setWorkingHours(BigDecimal.valueOf(4.0));
			} else {
				entity.setWorkingHours(BigDecimal.ZERO);
			}
		}

		attendanceRepository.save(entity);
		log.info("Attendance marked successfully for empId: {}", dto.getEmployeeId());
	}

	@Override
	public void uploadAttendanceCsv(MultipartFile file) {
		log.info("Uploading attendance CSV");
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
			String line;
			reader.readLine(); // Skip header
			while ((line = reader.readLine()) != null) {
				String[] data = line.split(",");
				Long empId = Long.parseLong(data[0].trim());
				LocalDate date = LocalDate.parse(data[1].trim());
				AttendanceStatus status = AttendanceStatus.valueOf(data[2].trim());
				BigDecimal hours = new BigDecimal(data[3].trim());

				AttendanceRequestDto dto = new AttendanceRequestDto();
				dto.setEmployeeId(empId);
				dto.setAttendanceDate(date);
				dto.setAttendanceStatus(status);
				dto.setWorkingHours(hours);

				try {
					markAttendance(dto);
				} catch (Exception e) {
					log.error("Error inserting CSV row for empId {} date {}: {}", empId, date, e.getMessage());
				}
			}
		} catch (Exception e) {
			throw new RuntimeException("CSV processing failed: " + e.getMessage());
		}
	}

	@Override
	public void updateAttendance(Long id, AttendanceRequestDto dto) {
		log.info("Updating attendance id: {}", id);

		Attendance attendance = attendanceRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException(
						messageByLocaleService.getMessage("attendance.not.found", new Object[] { id })));

		if (dto.getAttendanceDate().isAfter(LocalDate.now())) {
			throw new IllegalArgumentException(messageByLocaleService.getMessage("attendance.date.invalid", null));
		}

		validateAttendanceBusinessRules(dto);

		attendance.setAttendanceDate(dto.getAttendanceDate());
		if (dto.getAttendanceStatus() != null) {
			attendance.setAttendanceStatus(dto.getAttendanceStatus());
		}
		if (dto.getWorkingHours() != null) {
			attendance.setWorkingHours(dto.getWorkingHours());
		}
		if (dto.getCheckInTime() != null) {
			attendance.setCheckInTime(dto.getCheckInTime());
		}
		if (dto.getCheckOutTime() != null) {
			attendance.setCheckOutTime(dto.getCheckOutTime());
		}
		if (dto.getRemarks() != null) {
			attendance.setRemarks(dto.getRemarks());
		}
		if (dto.getApprovedBy() != null) {
			attendance.setApprovedBy(dto.getApprovedBy());
		}
		if (dto.getApprovalStatus() != null) {
			attendance.setApprovalStatus(dto.getApprovalStatus());
		}

		attendanceRepository.save(attendance);
		log.info("Attendance updated successfully for id: {}", id);
	}

	@Override
	@Transactional(readOnly = true)
	public AttendanceResponseDto getAttendance(Long id) {
		log.info("Fetching attendance id: {}", id);
		Attendance attendance = attendanceRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException(messageByLocaleService.getMessage("attendance.not.found", new Object[] { id })));
		return attendanceMapper.entityToDto(attendance);
	}

	@Override
	@Transactional(readOnly = true)
	public AttendanceResponseDto getTodayAttendance(Long employeeId) {
		log.info("Fetching today's attendance for employeeId: {}", employeeId);
		LocalDate today = LocalDate.now();
		return attendanceRepository.findByEmployeeIdAndAttendanceDate(employeeId, today)
				.map(attendanceMapper::entityToDto)
				.orElse(null);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<AttendanceResponseDto> getAllAttendance(AttendanceSearchRequest request, Pageable pageable) {
		log.info("Fetching attendance with filter request: {}, pageable: {}", request, pageable);
		org.springframework.data.jpa.domain.Specification<Attendance> spec = AttendanceSpecification.getSpecification(request);
		Page<Attendance> attendancePage = attendanceRepository.findAll(spec, pageable);
		return attendancePage.map(attendanceMapper::entityToDto);
	}

	private void validateAttendanceBusinessRules(AttendanceRequestDto dto) {
		if (dto.getAttendanceStatus() == null || dto.getWorkingHours() == null) {
			return; // Skip strict business rule validations if fields are not present (like in check-in phase)
		}

		if (dto.getAttendanceStatus() == AttendanceStatus.ABSENT && dto.getWorkingHours().compareTo(BigDecimal.ZERO) > 0) {
			throw new IllegalArgumentException(messageByLocaleService.getMessage("attendance.status.invalid", null));
		}

		if (dto.getAttendanceStatus() == AttendanceStatus.PRESENT && dto.getWorkingHours().compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException(messageByLocaleService.getMessage("attendance.present.hours.invalid", null));
		}

		// Also handle validation for leaves (paid / unpaid leaves cannot have working hours)
		if ((dto.getAttendanceStatus() == AttendanceStatus.PAID_LEAVE || dto.getAttendanceStatus() == AttendanceStatus.UNPAID_LEAVE)
				&& dto.getWorkingHours().compareTo(BigDecimal.ZERO) > 0) {
			throw new IllegalArgumentException(messageByLocaleService.getMessage("attendance.leave.hours.invalid", null));
		}
	}
}
