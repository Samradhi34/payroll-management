package com.epms.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.epms.dto.request.BulkPayrollRequest;
import com.epms.dto.request.PayrollGenerateRequest;
import com.epms.dto.request.PayrollSearchRequest;
import com.epms.dto.response.BulkPayrollResponse;
import com.epms.dto.response.PayrollResponse;
import com.epms.dto.response.SalarySlipResponse;

/**
 * Service contract for all Payroll and SalarySlip operations.
 *
 * <p>This interface defines the business operations related to payroll management:
 * generating payrolls, managing payroll status lifecycle (GENERATED → APPROVED → PAID),
 * cancellation, and generating salary slips tied to approved/paid payrolls.
 *
 * <p><b>Role-Based Access:</b>
 * <ul>
 *   <li>ADMIN: Full access — generate, approve, pay, cancel, read all, generate slips</li>
 *   <li>HR: Can generate payrolls, read all payrolls, generate slips, read slips</li>
 *   <li>EMPLOYEE: Can only read their own payrolls and salary slips</li>
 * </ul>
 */
public interface PayrollService {

	/**
	 * Generate a new payroll record for the given employee and period.
	 *
	 * <p><b>Business Rules:</b>
	 * <ul>
	 *   <li>The employee must exist and be in ACTIVE status</li>
	 *   <li>No payroll must already exist for the same employee, month, and year</li>
	 *   <li>Net salary = base salary + bonus - deductions</li>
	 *   <li>Net salary must be a positive value after calculation</li>
	 *   <li>Generated date is set to the current timestamp</li>
	 *   <li>Initial status is always GENERATED</li>
	 * </ul>
	 *
	 * @param request the payroll generation request with all required fields
	 * @return the created payroll as a response DTO
	 */
	PayrollResponse generatePayroll(PayrollGenerateRequest request);

	/**
	 * Approve a payroll record.
	 *
	 * <p><b>Business Rules:</b>
	 * <ul>
	 *   <li>Only a GENERATED payroll can be approved</li>
	 *   <li>Attempting to approve an APPROVED, PAID, or CANCELLED payroll throws PayrollOperationException</li>
	 * </ul>
	 *
	 * @param id the payroll ID to approve
	 * @return the updated payroll response
	 */
	PayrollResponse approvePayroll(Long id);

	/**
	 * Mark a payroll as PAID (salary disbursed to employee).
	 *
	 * <p><b>Business Rules:</b>
	 * <ul>
	 *   <li>Only an APPROVED payroll can be marked as PAID</li>
	 *   <li>Attempting to pay a GENERATED, PAID, or CANCELLED payroll throws PayrollOperationException</li>
	 * </ul>
	 *
	 * @param id the payroll ID to mark as paid
	 * @return the updated payroll response
	 */
	PayrollResponse markPayrollAsPaid(Long id);

	/**
	 * Cancel a payroll record.
	 *
	 * <p><b>Business Rules:</b>
	 * <ul>
	 *   <li>Only GENERATED or APPROVED payrolls can be cancelled</li>
	 *   <li>A PAID payroll cannot be cancelled — it is a finalized transaction</li>
	 *   <li>An already CANCELLED payroll cannot be cancelled again</li>
	 * </ul>
	 *
	 * @param id the payroll ID to cancel
	 * @return the updated payroll response
	 */
	PayrollResponse cancelPayroll(Long id);

	/**
	 * Retrieve a specific payroll record by its ID.
	 *
	 * @param id the payroll primary key
	 * @return the payroll response DTO
	 * @throws com.epms.exception.PayrollNotFoundException if not found
	 */
	PayrollResponse getPayrollById(Long id);

	/**
	 * List all payroll records for a specific employee, ordered by most recent first.
	 *
	 * @param employeeId the employee's primary key
	 * @return list of payroll responses (empty list if no payrolls found)
	 */
	List<PayrollResponse> getPayrollsByEmployee(Long employeeId);

	/**
	 * List all payrolls generated for a specific month and year across all employees.
	 * Useful for HR to review a monthly payroll run.
	 *
	 * @param month 1-12
	 * @param year  e.g. 2024
	 * @return list of payroll responses for that period
	 */
	List<PayrollResponse> getPayrollsByMonthAndYear(Integer month, Integer year);

	/**
	 * Fetch all payroll records in the system with specification-based filtering, pagination and sorting.
	 *
	 * @param request the filtering criteria
	 * @param pageable pagination and sorting parameters
	 * @return page of payroll records
	 */
	Page<PayrollResponse> getAllPayrolls(PayrollSearchRequest request, Pageable pageable);

	/**
	 * Generate a salary slip document path for a specific payroll.
	 *
	 * <p><b>Business Rules:</b>
	 * <ul>
	 *   <li>Payroll must be in APPROVED or PAID status</li>
	 *   <li>A salary slip must not already exist for this payroll</li>
	 *   <li>The slip path is a unique reference (formatted as employeeId/year/month)</li>
	 * </ul>
	 *
	 * @param payrollId the payroll ID for which to generate a slip
	 * @return the created salary slip response
	 */
	SalarySlipResponse generateSalarySlip(Long payrollId);

	/**
	 * Retrieve the salary slip associated with a specific payroll record.
	 *
	 * @param payrollId the payroll's primary key
	 * @return the salary slip response DTO
	 * @throws com.epms.exception.SalarySlipNotFoundException if not yet generated
	 */
	SalarySlipResponse getSalarySlipByPayroll(Long payrollId);

	/**
	 * List all salary slips generated for an employee across all payroll periods.
	 *
	 * @param employeeId the employee's primary key
	 * @return list of salary slip responses ordered by most recent first
	 */
	List<SalarySlipResponse> getSalarySlipsByEmployee(Long employeeId);

	/**
	 * Generate payroll for all active employees for a given month and year.
	 *
	 * @param request the bulk payroll generation request
	 * @return summary of generated, skipped, and failed payrolls
	 */
	BulkPayrollResponse generatePayrollForAll(BulkPayrollRequest request);
}
