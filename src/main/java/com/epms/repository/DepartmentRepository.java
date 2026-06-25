package com.epms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.epms.entity.Department;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long>, JpaSpecificationExecutor<Department>{

	boolean existsByName(String name);

	Optional<Department> findByIdAndActiveTrue(Long id);

	boolean existsByNameIgnoreCase(String name);

	List<Department> findAllByActiveTrue();


}
