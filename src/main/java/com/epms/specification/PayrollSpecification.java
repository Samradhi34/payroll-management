package com.epms.specification;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import com.epms.dto.request.PayrollSearchRequest;
import com.epms.entity.Employee;
import com.epms.entity.Payroll;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;

public class PayrollSpecification {

	public static Specification<Payroll> getSpecification(PayrollSearchRequest request) {
		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();

			// Filter by employee ID
			if (request.getEmployeeId() != null) {
				Join<Payroll, Employee> employeeJoin = root.join("employee");
				predicates.add(cb.equal(employeeJoin.get("id"), request.getEmployeeId()));
			}

			// Global search on employee first name, last name, or email
			if (request.getSearch() != null && StringUtils.hasText(request.getSearch())) {
				String searchPattern = "%" + request.getSearch().toLowerCase().trim() + "%";
				Join<Payroll, Employee> employeeJoin = root.join("employee");
				predicates.add(cb.or(
						cb.like(cb.lower(employeeJoin.get("firstName")), searchPattern),
						cb.like(cb.lower(employeeJoin.get("lastName")), searchPattern),
						cb.like(cb.lower(employeeJoin.get("email")), searchPattern)
				));
			}

			// Filter by month
			if (request.getMonth() != null) {
				predicates.add(cb.equal(root.get("month"), request.getMonth()));
			}

			// Filter by year
			if (request.getYear() != null) {
				predicates.add(cb.equal(root.get("year"), request.getYear()));
			}

			// Filter by status
			if (request.getStatus() != null) {
				predicates.add(cb.equal(root.get("payrollStatus"), request.getStatus()));
			}

			// Filter by net salary range
			if (request.getMinNetSalary() != null) {
				predicates.add(cb.greaterThanOrEqualTo(root.get("netSalary"), request.getMinNetSalary()));
			}
			if (request.getMaxNetSalary() != null) {
				predicates.add(cb.lessThanOrEqualTo(root.get("netSalary"), request.getMaxNetSalary()));
			}

			// Filter by generated date range
			if (request.getGeneratedDateFrom() != null) {
				predicates.add(cb.greaterThanOrEqualTo(root.get("generatedDate"), request.getGeneratedDateFrom()));
			}
			if (request.getGeneratedDateTo() != null) {
				predicates.add(cb.lessThanOrEqualTo(root.get("generatedDate"), request.getGeneratedDateTo()));
			}

			return cb.and(predicates.toArray(new Predicate[0]));
		};
	}
}
