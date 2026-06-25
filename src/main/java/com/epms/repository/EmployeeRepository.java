package com.epms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.epms.constant.EmployeeStatus;
import com.epms.entity.Employee;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> , JpaSpecificationExecutor<Employee>{

	/**
	 * retrieves an active employee by id and also check active status (don’t fetch
	 * inactive)
	 * 
	 * @param id
	 * @return Optional containing the employee if found and active
	 */
	Optional<Employee> findByIdAndActiveTrue(Long id);
	
	/**
	 * 
	 * @param status
	 * @return
	 */
	List<Employee> findByEmployeeStatus(EmployeeStatus status);

	/**
	 * To avoid duplicate email.
	 * 
	 * @param email
	 * @return true if an employee with the email exists
	 */
	boolean existsByEmail(String email);

	/**
	 * Checks whether an employee with the given phone number already exists
	 * 
	 * @param phone
	 * @return true if an employee with the phone exists
	 */
	boolean existsByPhone(String phone);

	/**
	 * 
	 * @param email
	 * @return
	 */
	boolean existsByEmailIgnoreCase(String email);

	Optional<Employee> findByEmailIgnoreCase(String email);

}
