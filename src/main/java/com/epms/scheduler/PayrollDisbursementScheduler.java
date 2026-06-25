package com.epms.scheduler;

import java.time.LocalDate;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.epms.constant.PayrollStatus;
import com.epms.entity.Payroll;
import com.epms.repository.PayrollRepository;
import com.epms.service.PayrollService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Scheduler to automate salary disbursement on the salary release date.
 * Automatically marks all APPROVED payrolls of the previous month as PAID.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PayrollDisbursementScheduler {

	private final PayrollRepository payrollRepository;
	private final PayrollService payrollService;

	/**
	 * Run monthly payroll disbursement.
	 */
	@Scheduled(cron = "${app.cron.payroll-payment}")
	public void runAutomatedPayrollDisbursement() {
		LocalDate now = LocalDate.now();
		LocalDate lastMonth = now.minusMonths(1);
		int month = lastMonth.getMonthValue();
		int year = lastMonth.getYear();

		log.info("Starting automated payroll disbursement scheduler for month: {}, year: {}", month, year);

		List<Payroll> targetPayrolls = payrollRepository.findByMonthAndYear(month, year);
		log.info("Found {} payroll records for {}/{}", targetPayrolls.size(), month, year);

		int paidCount = 0;
		int skippedCount = 0;
		int failureCount = 0;

		for (Payroll payroll : targetPayrolls) {
			try {
				/**
				 *  Only disburse if status is APPROVED
				 */
				if (payroll.getPayrollStatus() == PayrollStatus.APPROVED) {
					payrollService.markPayrollAsPaid(payroll.getId());
					log.info("Successfully disbursed payroll ID: {} for employee ID: {}", 
							payroll.getId(), payroll.getEmployee().getId());
					paidCount++;
				} else {
					log.debug("Skipping payroll ID: {} because status is: {}. Only APPROVED payrolls can be auto-paid.", 
							payroll.getId(), payroll.getPayrollStatus());
					skippedCount++;
				}
			} catch (Exception e) {
				log.error("Failed to auto-disburse payroll ID: {} - Exception: {}", 
						payroll.getId(), e.getMessage());
				failureCount++;
			}
		}

		log.info("Automated payroll disbursement complete. Results -> Disbursed (PAID): {}, Skipped: {}, Failures: {}", 
				paidCount, skippedCount, failureCount);
	}
}
