package com.epms.config;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.epms.dto.request.BulkPayrollRequest;
import com.epms.dto.response.BulkPayrollResponse;
import com.epms.service.PayrollService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Seeds payroll records for all active employees on startup when enabled.
 * Uses the same calculation logic as manual bulk generation.
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class PayrollSeeder implements CommandLineRunner {

	private final PayrollService payrollService;

	@Value("${app.payroll.seed-on-startup:true}")
	private boolean seedOnStartup;

	@Override
	@Transactional
	public void run(String... args) {
		if (!seedOnStartup) {
			log.debug("Payroll seeding disabled (app.payroll.seed-on-startup=false)");
			return;
		}

		LocalDate now = LocalDate.now();
		int month = now.getMonthValue();
		int year = now.getYear();

		log.info("Seeding payroll for all active employees for {}/{}...", month, year);

		BulkPayrollRequest request = new BulkPayrollRequest();
		request.setMonth(month);
		request.setYear(year);
		request.setBonus(BigDecimal.ZERO);
		request.setDeductions(BigDecimal.ZERO);

		BulkPayrollResponse response = payrollService.generatePayrollForAll(request);

		log.info("Payroll seeding complete for {}/{} -> generated: {}, skipped: {}, failed: {}",
				month, year, response.getGenerated(), response.getSkipped(), response.getFailed());
	}
}
