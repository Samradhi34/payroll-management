package com.epms.specification;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import com.epms.dto.request.DepartmentSearchRequest;
import com.epms.entity.Department;

import jakarta.persistence.criteria.Predicate;

public class DepartmentSpecification {

	public static Specification<Department> getSpecification(DepartmentSearchRequest request) {
		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();

			/**
			 *  Global search on name, description, location
			 */
			if (request.getSearch() != null && StringUtils.hasText(request.getSearch())) {
				String searchPattern = "%" + request.getSearch().toLowerCase().trim() + "%";
				predicates.add(cb.or(
						cb.like(cb.lower(root.get("name")), searchPattern),
						cb.like(cb.lower(root.get("description")), searchPattern),
						cb.like(cb.lower(root.get("location")), searchPattern)
				));
			}

			/**
			 *  Filter by active status
			 */
			if (request.getActive() != null) {
				predicates.add(cb.equal(root.get("active"), request.getActive()));
			}

			return cb.and(predicates.toArray(new Predicate[0]));
		};
	}
}
