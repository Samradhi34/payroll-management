package com.epms.scheduler;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.epms.constant.EmployeeStatus;
import com.epms.dto.request.PayrollGenerateRequest;
import com.epms.entity.Employee;
import com.epms.repository.EmployeeRepository;
import com.epms.repository.PayrollRepository;
import com.epms.service.PayrollService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Scheduler to automate payroll generation at the start of every month.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PayrollScheduler {

	private final EmployeeRepository employeeRepository;
	private final PayrollRepository payrollRepository;
	private final PayrollService payrollService;

	/**
	 * Automate payroll generation for all active employees for the previous month.
	 * Triggered by cron expression configured in application.properties.
	 */
	@Scheduled(cron = "${app.cron.payroll-generation}")
	public void runAutomatedPayrollGeneration() {
		LocalDate now = LocalDate.now();
		LocalDate lastMonth = now.minusMonths(1);
		int month = lastMonth.getMonthValue();
		int year = lastMonth.getYear();

		log.info("Starting automated payroll generation scheduler for month: {}, year: {}", month, year);

		List<Employee> activeEmployees = employeeRepository.findByEmployeeStatus(EmployeeStatus.ACTIVE);
		log.info("Found {} active employees to process", activeEmployees.size());

		int successCount = 0;
		int skipCount = 0;
		int failureCount = 0;

		for (Employee employee : activeEmployees) {
			try {
				/**
				 *  Prevent duplicate payroll generation
				 */
				boolean exists = payrollRepository.existsByEmployeeIdAndMonthAndYear(employee.getId(), month, year);
				if (exists) {
					log.debug("Payroll already exists for Employee ID: {} for month: {}, year: {}. Skipping.", 
							employee.getId(), month, year);
					skipCount++;
					continue;
				}

				/**
				 *  Build generate request
				 */
				PayrollGenerateRequest request = new PayrollGenerateRequest();
				request.setEmployeeId(employee.getId());
				request.setMonth(month);
				request.setYear(year);
				request.setBonus(BigDecimal.ZERO);
				request.setDeductions(BigDecimal.ZERO);

				payrollService.generatePayroll(request);
				successCount++;
			} catch (Exception e) {
				log.error("Failed to generate payroll for Employee ID: {} - Exception: {}", 
						employee.getId(), e.getMessage());
				failureCount++;
			}
		}

		log.info("Automated payroll generation complete. Results -> Success: {}, Skipped: {}, Failures: {}", 
				successCount, skipCount, failureCount);
	}
}
