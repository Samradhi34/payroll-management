package com.epms.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.epms.constant.AttendanceStatus;
import com.epms.entity.Attendance;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long>, JpaSpecificationExecutor<Attendance>{

	/**
	 * 
	 * @param employeeId
	 * @param attendanceDate
	 * @return
	 */
	Optional<Attendance> findByEmployeeIdAndAttendanceDate(Long employeeId, LocalDate attendanceDate);

	long countByEmployeeIdAndAttendanceDateBetweenAndAttendanceStatus(
			Long employeeId, LocalDate startDate, LocalDate endDate, AttendanceStatus attendanceStatus);

	java.util.List<Attendance> findByEmployeeIdAndAttendanceDateBetween(
			Long employeeId, LocalDate startDate, LocalDate endDate);
}
