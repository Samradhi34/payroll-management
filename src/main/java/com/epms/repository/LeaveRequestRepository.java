package com.epms.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.epms.constant.LeaveStatus;
import com.epms.entity.LeaveRequest;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long>, JpaSpecificationExecutor<LeaveRequest> {

	List<LeaveRequest> findByEmployeeIdOrderByStartDateDesc(Long employeeId);

	@Query("SELECT lr FROM LeaveRequest lr " +
		   "WHERE lr.employee.id = :employeeId " +
		   "AND lr.leaveStatus = :status " +
		   "AND ((lr.startDate >= :start AND lr.startDate <= :end) " +
		   "OR (lr.endDate >= :start AND lr.endDate <= :end) " +
		   "OR (lr.startDate <= :start AND lr.endDate >= :end))")
	List<LeaveRequest> findApprovedLeavesInPeriod(
		@Param("employeeId") Long employeeId,
		@Param("status") LeaveStatus status,
		@Param("start") LocalDate startDate,
		@Param("end") LocalDate endDate
	);
}
