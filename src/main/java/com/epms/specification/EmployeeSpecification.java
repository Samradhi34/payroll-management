package com.epms.specification;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import com.epms.dto.request.EmployeeSearchRequest;
import com.epms.entity.Department;
import com.epms.entity.Employee;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;

public class EmployeeSpecification {

	public static Specification<Employee> getSpecification(EmployeeSearchRequest request) {
		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();

			/**
			 *  Global search: first name, last name, email, phone, designation
			 */
			if (request.getSearch() != null && StringUtils.hasText(request.getSearch())) {
				String searchPattern = "%" + request.getSearch().trim().toLowerCase() + "%";
				predicates.add(cb.or(
						cb.like(cb.lower(root.get("firstName")), searchPattern),
						cb.like(cb.lower(root.get("lastName")), searchPattern),
						cb.like(cb.lower(root.get("email")), searchPattern),
						cb.like(cb.lower(root.get("phone")), searchPattern),
						cb.like(cb.lower(root.get("designation")), searchPattern)
				));
			}

			/**
			 *  Filter by employee status
			 */
			if (request.getStatus() != null) {
				predicates.add(cb.equal(root.get("employeeStatus"), request.getStatus()));
			}

			/**
			 *  Filter by department id
			 */
			if (request.getDepartmentId() != null) {
				Join<Employee, Department> departmentJoin = root.join("department");
				predicates.add(cb.equal(departmentJoin.get("id"), request.getDepartmentId()));
			}

			/**
			 *  Filter by min salary
			 */
			if (request.getMinSalary() != null) {
				predicates.add(cb.greaterThanOrEqualTo(root.get("baseSalary"), request.getMinSalary()));
			}

			/**
			 *  Filter by max salary
			 */
			if (request.getMaxSalary() != null) {
				predicates.add(cb.lessThanOrEqualTo(root.get("baseSalary"), request.getMaxSalary()));
			}

			/**
			 *  Filter by joining date range
			 */
			if (request.getJoiningDateFrom() != null) {
				predicates.add(cb.greaterThanOrEqualTo(root.get("joiningDate"), request.getJoiningDateFrom()));
			}
			if (request.getJoiningDateTo() != null) {
				predicates.add(cb.lessThanOrEqualTo(root.get("joiningDate"), request.getJoiningDateTo()));
			}

			return cb.and(predicates.toArray(new Predicate[0]));
		};
	}
}
