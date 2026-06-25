package com.epms.scheduler;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.epms.constant.ApprovalStatus;
import com.epms.constant.AttendanceStatus;
import com.epms.constant.EmployeeStatus;
import com.epms.entity.Attendance;
import com.epms.entity.Employee;
import com.epms.repository.AttendanceRepository;
import com.epms.repository.EmployeeRepository;
import com.epms.repository.HolidayRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Scheduler to automate daily attendance checking.
 * Marks active employees as ABSENT, WEEK_OFF, or HOLIDAY based on log existence and day category.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AttendanceScheduler {

	private final EmployeeRepository employeeRepository;
	private final AttendanceRepository attendanceRepository;
	private final HolidayRepository holidayRepository;

	/**
	 * Runs nightly at 23:59 to check and mark status for missing logs.
	 */
	@Scheduled(cron = "${app.cron.attendance-marker}")
	public void runDailyAttendanceMarker() {
		LocalDate today = LocalDate.now();
		log.info("Starting automated daily attendance marker for date: {}", today);

		boolean isHoliday = holidayRepository.existsByHolidayDate(today);
		DayOfWeek dayOfWeek = today.getDayOfWeek();
		boolean isWeekend = (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY);

		AttendanceStatus defaultStatus;
		String remarks;

		if (isHoliday) {
			defaultStatus = AttendanceStatus.HOLIDAY;
			remarks = "Auto-marked: Official Holiday";
		} else if (isWeekend) {
			defaultStatus = AttendanceStatus.WEEK_OFF;
			remarks = "Auto-marked: Weekend Off";
		} else {
			defaultStatus = AttendanceStatus.ABSENT;
			remarks = "Auto-marked: Absent (No log found)";
		}

		List<Employee> activeEmployees = employeeRepository.findByEmployeeStatus(EmployeeStatus.ACTIVE);
		log.info("Checking attendance logs for {} active employees. Target status: {}", activeEmployees.size(), defaultStatus);

		int markedCount = 0;
		int processedCount = 0;

		for (Employee employee : activeEmployees) {
			try {
				// Don't auto-mark if they haven't joined yet
				if (today.isBefore(employee.getJoiningDate())) {
					continue;
				}

				boolean logExists = attendanceRepository.findByEmployeeIdAndAttendanceDate(employee.getId(), today).isPresent();
				if (!logExists) {
					Attendance autoAtt = new Attendance();
					autoAtt.setEmployee(employee);
					autoAtt.setAttendanceDate(today);
					autoAtt.setAttendanceStatus(defaultStatus);
					autoAtt.setWorkingHours(BigDecimal.ZERO);
					autoAtt.setRemarks(remarks);
					autoAtt.setApprovalStatus(ApprovalStatus.APPROVED);
					autoAtt.setActive(true);

					attendanceRepository.save(autoAtt);
					markedCount++;
				}
				processedCount++;
			} catch (Exception e) {
				log.error("Error processing daily attendance check for Employee ID: {} - Error: {}", 
						employee.getId(), e.getMessage());
			}
		}

		log.info("Daily attendance check complete. Total processed: {}, Marked {}: {}", 
				processedCount, defaultStatus, markedCount);
	}
}
