package com.epms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.epms.constant.PayrollStatus;
import com.epms.entity.Payroll;

/**
 * Repository for Payroll entities.
 * Provides standard CRUD operations (via JpaRepository) plus domain-specific
 * query methods for payroll management use-cases.
 */
@Repository
public interface PayrollRepository extends JpaRepository<Payroll, Long>, JpaSpecificationExecutor<Payroll> {

	/**
	 * Fetch all payroll records for a specific employee.
	 *
	 * @param employeeId the employee's primary key
	 * @return ordered list of payrolls (latest year/month first)
	 */
	List<Payroll> findByEmployeeIdOrderByYearDescMonthDesc(Long employeeId);

	/**
	 * Check whether a payroll already exists for a given employee, month, and year.
	 * Used to prevent duplicate payroll generation.
	 *
	 * @param employeeId the employee's primary key
	 * @param month      1-12
	 * @param year       e.g. 2024
	 * @return true if a duplicate exists
	 */
	boolean existsByEmployeeIdAndMonthAndYear(Long employeeId, Integer month, Integer year);

	/**
	 * Retrieve the specific payroll for an employee in a given month/year.
	 *
	 * @param employeeId the employee's primary key
	 * @param month      1-12
	 * @param year       e.g. 2024
	 * @return an Optional containing the payroll, or empty if none exists
	 */
	Optional<Payroll> findByEmployeeIdAndMonthAndYear(Long employeeId, Integer month, Integer year);

	/**
	 * List all payrolls for a given month and year (e.g. payroll run for March 2024).
	 *
	 * @param month 1-12
	 * @param year  e.g. 2024
	 * @return list of matching payroll records
	 */
	List<Payroll> findByMonthAndYear(Integer month, Integer year);

	/**
	 * Filter payrolls by status (GENERATED, APPROVED, PAID, CANCELLED).
	 *
	 * @param status the target payroll status
	 * @return list of matching payrolls
	 */
	List<Payroll> findByPayrollStatus(PayrollStatus status);

	/**
	 * Fetch a payroll with its employee and employee's department eagerly loaded.
	 * Used during salary slip PDF generation to avoid LazyInitializationException.
	 *
	 * @param id payroll primary key
	 * @return Optional containing the fully-loaded payroll
	 */
	@org.springframework.data.jpa.repository.Query(
		"SELECT p FROM Payroll p " +
		"JOIN FETCH p.employee e " +
		"JOIN FETCH e.department " +
		"WHERE p.id = :id")
	Optional<Payroll> findByIdWithEmployeeAndDepartment(
		@org.springframework.data.repository.query.Param("id") Long id);
}
