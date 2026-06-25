package com.epms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.epms.entity.SalarySlip;

/**
 * Repository for SalarySlip entities.
 * Provides standard CRUD operations plus domain-specific query methods.
 */
@Repository
public interface SalarySlipRepository extends JpaRepository<SalarySlip, Long> {

	/**
	 * Find the salary slip associated with a specific payroll record.
	 * There is a one-to-one relationship between Payroll and SalarySlip.
	 *
	 * @param payrollId the payroll's primary key
	 * @return an Optional with the salary slip, or empty if not yet generated
	 */
	Optional<SalarySlip> findByPayrollId(Long payrollId);

	/**
	 * Check whether a salary slip already exists for a given payroll.
	 * Used to prevent generating duplicate slips.
	 *
	 * @param payrollId the payroll's primary key
	 * @return true if a slip already exists
	 */
	boolean existsByPayrollId(Long payrollId);

	/**
	 * List all salary slips ever generated for a specific employee,
	 * traversing the Payroll → Employee relationship path.
	 *
	 * @param employeeId the employee's primary key
	 * @return list of salary slips ordered by most recent first
	 */
	List<SalarySlip> findByPayroll_Employee_IdOrderByGeneratedDateDesc(Long employeeId);
}
