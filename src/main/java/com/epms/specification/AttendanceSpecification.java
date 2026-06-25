package com.epms.specification;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import com.epms.dto.request.AttendanceSearchRequest;
import com.epms.entity.Attendance;
import com.epms.entity.Employee;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;

public class AttendanceSpecification {

	public static Specification<Attendance> getSpecification(AttendanceSearchRequest request) {
		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();

			// Filter by employee ID
			if (request.getEmployeeId() != null) {
				Join<Attendance, Employee> employeeJoin = root.join("employee");
				predicates.add(cb.equal(employeeJoin.get("id"), request.getEmployeeId()));
			}

			// Global search on employee name/email
			if (request.getSearch() != null && StringUtils.hasText(request.getSearch())) {
				String searchPattern = "%" + request.getSearch().toLowerCase().trim() + "%";
				Join<Attendance, Employee> employeeJoin = root.join("employee");
				predicates.add(cb.or(
						cb.like(cb.lower(employeeJoin.get("firstName")), searchPattern),
						cb.like(cb.lower(employeeJoin.get("lastName")), searchPattern),
						cb.like(cb.lower(employeeJoin.get("email")), searchPattern)
				));
			}

			// Filter by status
			if (request.getStatus() != null) {
				predicates.add(cb.equal(root.get("attendanceStatus"), request.getStatus()));
			}

			// Filter by date range
			if (request.getDateFrom() != null) {
				predicates.add(cb.greaterThanOrEqualTo(root.get("attendanceDate"), request.getDateFrom()));
			}
			if (request.getDateTo() != null) {
				predicates.add(cb.lessThanOrEqualTo(root.get("attendanceDate"), request.getDateTo()));
			}

			// Filter by working hours range
			if (request.getMinWorkingHours() != null) {
				predicates.add(cb.greaterThanOrEqualTo(root.get("workingHours"), request.getMinWorkingHours()));
			}
			if (request.getMaxWorkingHours() != null) {
				predicates.add(cb.lessThanOrEqualTo(root.get("workingHours"), request.getMaxWorkingHours()));
			}

			return cb.and(predicates.toArray(new Predicate[0]));
		};
	}
}
