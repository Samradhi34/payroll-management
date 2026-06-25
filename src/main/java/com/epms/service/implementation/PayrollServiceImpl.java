package com.epms.service.implementation;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.epms.dto.request.BulkPayrollRequest;
import com.epms.dto.request.PayrollSearchRequest;
import com.epms.specification.PayrollSpecification;

import com.epms.constant.EmployeeStatus;
import com.epms.constant.PayrollStatus;
import com.epms.dto.request.PayrollGenerateRequest;
import com.epms.dto.response.BulkPayrollResponse;
import com.epms.dto.response.PayrollResponse;
import com.epms.dto.response.SalarySlipResponse;
import com.epms.entity.Employee;
import com.epms.entity.Payroll;
import com.epms.entity.SalarySlip;
import com.epms.exception.EmployeeNotFoundException;
import com.epms.exception.PayrollAlreadyExistsException;
import com.epms.exception.PayrollNotFoundException;
import com.epms.exception.PayrollOperationException;
import com.epms.exception.SalarySlipNotFoundException;
import com.epms.locale.MessageByLocaleService;
import com.epms.mapper.PayrollMapper;
import com.epms.mapper.SalarySlipMapper;
import com.epms.repository.AttendanceRepository;
import com.epms.repository.EmployeeRepository;
import com.epms.repository.PayrollRepository;
import com.epms.repository.SalarySlipRepository;
import com.epms.repository.LeaveRequestRepository;
import com.epms.repository.HolidayRepository;
import com.epms.service.PayrollService;
import com.epms.service.PdfGeneratorService;
import com.epms.entity.Attendance;
import com.epms.entity.Holiday;
import com.epms.entity.LeaveRequest;
import com.epms.constant.AttendanceStatus;
import com.epms.constant.LeaveType;
import com.epms.constant.LeaveStatus;
import java.time.DayOfWeek;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Production-ready implementation of {@link PayrollService}.
 *
 * <p>Handles the complete payroll lifecycle:
 * <ol>
 *   <li>GENERATE — creates a new payroll record for an active employee</li>
 *   <li>APPROVE  — finance/admin approves the payroll figures</li>
 *   <li>PAY      — salary is disbursed; payroll is finalised</li>
 *   <li>CANCEL   — only GENERATED or APPROVED payrolls may be cancelled</li>
 * </ol>
 *
 * <p>Also manages the {@link SalarySlip} lifecycle that is tightly coupled
 * to payroll: a slip can only be generated after the payroll is APPROVED or PAID,
 * and only one slip is allowed per payroll.
 *
 * <p><b>Logging levels used in this class (production strategy):</b>
 * <ul>
 *   <li>{@code log.info}  — important business milestones: payroll generated, approved, paid, cancelled</li>
 *   <li>{@code log.debug} — fine-grained diagnostics (salary math, record counts) — off in production</li>
 *   <li>{@code log.warn}  — recoverable business-rule violations (duplicate, wrong status, inactive employee)</li>
 *   <li>{@code log.error} — unexpected system-level failures that need immediate attention</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayrollServiceImpl implements PayrollService {

	private final PayrollRepository payrollRepository;
	private final SalarySlipRepository salarySlipRepository;
	private final EmployeeRepository employeeRepository;
	private final AttendanceRepository attendanceRepository;
	private final LeaveRequestRepository leaveRequestRepository;
	private final HolidayRepository holidayRepository;
	private final PayrollMapper payrollMapper;
	private final SalarySlipMapper salarySlipMapper;
	private final MessageByLocaleService messageByLocaleService;
	private final PdfGeneratorService pdfGeneratorService;

	@Value("${app.salary-slip.storage-dir:salary-slips}")
	private String storageDir;

	/**
	 *
	 * <p><b>Full validation chain:</b>
	 * <ol>
	 *   <li>Employee must exist</li>
	 *   <li>Employee must be ACTIVE (not terminated/resigned/inactive)</li>
	 *   <li>No duplicate payroll for the same employee/month/year</li>
	 *   <li>Net salary = baseSalary + bonus - deductions must be ≥ 0</li>
	 * </ol>
	 */
	@Override
	@Transactional
	public PayrollResponse generatePayroll(final PayrollGenerateRequest request) {
		log.info("[PayrollService] generatePayroll() ► employeeId={}, month={}, year={}",
				request.getEmployeeId(), request.getMonth(), request.getYear());

		/**
		 * Employee must exist 
		 */
		Employee employee = employeeRepository.findById(request.getEmployeeId())
				.orElseThrow(() -> {
					log.warn("[PayrollService] generatePayroll() ✗ Employee not found. employeeId={}",
							request.getEmployeeId());
					return new EmployeeNotFoundException(
							messageByLocaleService.getMessage("employee.not.found",
									new Object[]{request.getEmployeeId()}));
				});

		/**
		 * Employee must be ACTIVE
		 */
		if (employee.getEmployeeStatus() != EmployeeStatus.ACTIVE) {
			log.warn("[PayrollService] generatePayroll() ✗ Employee is not ACTIVE. employeeId={}, status={}",
					employee.getId(), employee.getEmployeeStatus());
			throw new PayrollOperationException(
					messageByLocaleService.getMessage("payroll.employee.not.active", null));
		}

		/**
		 * Prevent duplicate payroll for same employee/month/year 
		 */
		if (payrollRepository.existsByEmployeeIdAndMonthAndYear(
				request.getEmployeeId(), request.getMonth(), request.getYear())) {
			log.warn("[PayrollService] generatePayroll() ✗ Duplicate payroll. employeeId={}, month={}, year={}",
					request.getEmployeeId(), request.getMonth(), request.getYear());
			throw new PayrollAlreadyExistsException(
					messageByLocaleService.getMessage("payroll.already.exists",
							new Object[]{request.getMonth(), request.getYear()}));
		}

		/**
		 * Compute Total Days & Pro-ration 
		 */
		LocalDate periodStart = LocalDate.of(request.getYear(), request.getMonth(), 1);
		LocalDate periodEnd = periodStart.withDayOfMonth(periodStart.lengthOfMonth());
		int totalDaysInMonth = periodStart.lengthOfMonth();

		LocalDate startCompute = periodStart;
		if (employee.getJoiningDate().isAfter(periodStart)) {
			startCompute = employee.getJoiningDate();
		}

		LocalDate endCompute = periodEnd;
		if (employee.getResignationDate() != null && employee.getResignationDate().isBefore(periodEnd)) {
			endCompute = employee.getResignationDate();
		}

		long activeDays = 0;
		if (!startCompute.isAfter(endCompute)) {
			activeDays = ChronoUnit.DAYS.between(startCompute, endCompute) + 1;
		}

		BigDecimal baseSalary = employee.getBaseSalary();
		BigDecimal grossSalary = baseSalary;
		if (activeDays < totalDaysInMonth) {
			BigDecimal activeDaysBd = BigDecimal.valueOf(activeDays);
			BigDecimal totalDaysBd = BigDecimal.valueOf(totalDaysInMonth);
			grossSalary = baseSalary.multiply(activeDaysBd).divide(totalDaysBd, 2, RoundingMode.HALF_UP);
		}

		/**
		 * Fetch logs for calculation
		 */
		List<Attendance> attendances = attendanceRepository.findByEmployeeIdAndAttendanceDateBetween(
				request.getEmployeeId(), periodStart, periodEnd);
		List<LeaveRequest> approvedLeaves = leaveRequestRepository.findApprovedLeavesInPeriod(
				request.getEmployeeId(), LeaveStatus.APPROVED, periodStart, periodEnd);
		List<Holiday> holidays = holidayRepository.findByHolidayDateBetween(periodStart, periodEnd);
		List<LocalDate> holidayDates = holidays.stream().map(Holiday::getHolidayDate).toList();

		int absentDays = 0;
		int halfDays = 0;
		int unpaidLeaves = 0;
		int paidLeaves = 0;

		for (LocalDate date = periodStart; !date.isAfter(periodEnd); date = date.plusDays(1)) {
			/**
			 *  Skip days before joining or after resignation
			 */
			if (date.isBefore(employee.getJoiningDate()) 
					|| (employee.getResignationDate() != null && date.isAfter(employee.getResignationDate()))) {
				continue;
			}

			final LocalDate targetDate = date;
			Optional<Attendance> attOpt = attendances.stream()
					.filter(a -> a.getAttendanceDate().equals(targetDate)).findFirst();
			Optional<LeaveRequest> leaveOpt = approvedLeaves.stream()
					.filter(l -> !targetDate.isBefore(l.getStartDate()) && !targetDate.isAfter(l.getEndDate())).findFirst();

			boolean isWeekend = (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY);
			boolean isHoliday = holidayDates.contains(date);

			if (isWeekend || isHoliday) {
				continue; // paid, no deduction
			}

			if (attOpt.isPresent()) {
				Attendance att = attOpt.get();
				if (att.getAttendanceStatus() == AttendanceStatus.ABSENT) {
					absentDays++;
				} else if (att.getAttendanceStatus() == AttendanceStatus.HALF_DAY) {
					boolean isPaidLeave = leaveOpt.isPresent() && leaveOpt.get().getLeaveType() != LeaveType.UNPAID_LEAVE;
					if (!isPaidLeave) {
						halfDays++;
					} else {
						paidLeaves++;
					}
				} else if (att.getAttendanceStatus() == AttendanceStatus.UNPAID_LEAVE) {
					unpaidLeaves++;
				} else if (att.getAttendanceStatus() == AttendanceStatus.PAID_LEAVE) {
					paidLeaves++;
				}
			} else {
				/**
				 *  Missing attendance on working day is ABSENT
				 */
				absentDays++;
			}
		}

		/**
		 *  Compute Deductions 
		 */
		BigDecimal perDaySalary = BigDecimal.ZERO;
		if (totalDaysInMonth > 0) {
			perDaySalary = grossSalary.divide(BigDecimal.valueOf(totalDaysInMonth), 2, RoundingMode.HALF_UP);
		}

		BigDecimal absentDeductions = perDaySalary.multiply(BigDecimal.valueOf(absentDays));
		BigDecimal halfDayDeductions = perDaySalary.multiply(BigDecimal.valueOf(halfDays))
				.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
		BigDecimal unpaidLeaveDeductions = perDaySalary.multiply(BigDecimal.valueOf(unpaidLeaves));
		BigDecimal attendanceDeduction = absentDeductions.add(halfDayDeductions).add(unpaidLeaveDeductions);

		/**
		 Salary Components :
		 Basic: 50% of gross
		 HRA: 30% of gross
		 Allowances: 20% of gross
		 PF: 12% of Basic
		 Tax: Slab based (gross > 50000 -> 10%, gross > 20000 -> 5%, else 0)
		 */
		
		BigDecimal basic = grossSalary.multiply(BigDecimal.valueOf(0.50)).setScale(2, RoundingMode.HALF_UP);
		BigDecimal pf = basic.multiply(BigDecimal.valueOf(0.12)).setScale(2, RoundingMode.HALF_UP);

		BigDecimal tax = BigDecimal.ZERO;
		if (grossSalary.compareTo(BigDecimal.valueOf(50000.0)) > 0) {
			tax = grossSalary.multiply(BigDecimal.valueOf(0.10)).setScale(2, RoundingMode.HALF_UP);
		} else if (grossSalary.compareTo(BigDecimal.valueOf(20000.0)) > 0) {
			tax = grossSalary.multiply(BigDecimal.valueOf(0.05)).setScale(2, RoundingMode.HALF_UP);
		}

		BigDecimal bonus = (request.getBonus() != null) ? request.getBonus() : BigDecimal.ZERO;
		BigDecimal otherDeductions = (request.getDeductions() != null) ? request.getDeductions() : BigDecimal.ZERO;

		/**
		 *  Net Salary = Gross + Bonus - PF - Tax - Other Deductions - AttendanceDeduction
		 */
		BigDecimal netSalary = grossSalary.add(bonus)
				.subtract(pf)
				.subtract(tax)
				.subtract(otherDeductions)
				.subtract(attendanceDeduction);

		if (netSalary.compareTo(BigDecimal.ZERO) < 0) {
			netSalary = BigDecimal.ZERO;
		}

		log.debug("[PayrollService] generatePayroll() ► Salary calc: gross={}, bonus={}, pf={}, tax={}, attDeduct={}, net={}",
				grossSalary, bonus, pf, tax, attendanceDeduction, netSalary);

		Payroll payroll = new Payroll();
		payroll.setEmployee(employee);
		payroll.setMonth(request.getMonth());
		payroll.setYear(request.getYear());
		payroll.setBaseSalary(baseSalary);
		payroll.setGrossSalary(grossSalary);
		payroll.setTotalWorkingDays(totalDaysInMonth);
		payroll.setAbsentDays(absentDays);
		payroll.setHalfDays(halfDays);
		payroll.setUnpaidLeaves(unpaidLeaves);
		payroll.setPaidLeaves(paidLeaves);
		payroll.setAttendanceDeduction(attendanceDeduction);
		payroll.setBonus(bonus);
		payroll.setTax(tax);
		payroll.setPf(pf);
		payroll.setDeductions(otherDeductions); // Represents general deductions
		payroll.setNetSalary(netSalary);
		payroll.setGeneratedDate(LocalDateTime.now());
		payroll.setPayrollStatus(PayrollStatus.GENERATED);
		payroll.setActive(true);

		Payroll saved = payrollRepository.save(payroll);
		log.info("[PayrollService] generatePayroll() ✔ Payroll GENERATED. payrollId={}, employeeId={}, netSalary={}",
				saved.getId(), employee.getId(), netSalary);

		return payrollMapper.entityToDto(saved);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>State machine: GENERATED → APPROVED.
	 * Any other current status causes a {@link PayrollOperationException}.
	 */
	@Override
	@Transactional
	public PayrollResponse approvePayroll(final Long id) {
		log.info("[PayrollService] approvePayroll() ► payrollId={}", id);

		Payroll payroll = findPayrollOrThrow(id);

		if (payroll.getPayrollStatus() != PayrollStatus.GENERATED) {
			log.warn("[PayrollService] approvePayroll() ✗ Invalid transition. payrollId={}, currentStatus={}",
					id, payroll.getPayrollStatus());
			throw new PayrollOperationException(
					messageByLocaleService.getMessage("payroll.approve.invalid.status",
							new Object[]{payroll.getPayrollStatus()}));
		}

		payroll.setPayrollStatus(PayrollStatus.APPROVED);
		Payroll updated = payrollRepository.save(payroll);

		log.info("[PayrollService] approvePayroll() ✔ Payroll APPROVED. payrollId={}", id);
		return payrollMapper.entityToDto(updated);
	}

	/**
	 *
	 * <p>State machine: APPROVED → PAID.
	 * Represents the actual salary disbursement; this is a final state.
	 */
	@Override
	@Transactional
	public PayrollResponse markPayrollAsPaid(final Long id) {
		log.info("[PayrollService] markPayrollAsPaid() ► payrollId={}", id);

		Payroll payroll = findPayrollOrThrow(id);

		if (payroll.getPayrollStatus() != PayrollStatus.APPROVED) {
			log.warn("[PayrollService] markPayrollAsPaid() ✗ Invalid transition. payrollId={}, currentStatus={}",
					id, payroll.getPayrollStatus());
			throw new PayrollOperationException(
					messageByLocaleService.getMessage("payroll.pay.invalid.status",
							new Object[]{payroll.getPayrollStatus()}));
		}

		payroll.setPayrollStatus(PayrollStatus.PAID);
		Payroll updated = payrollRepository.save(payroll);

		log.info("[PayrollService] markPayrollAsPaid() ✔ Payroll marked PAID. payrollId={}", id);
		return payrollMapper.entityToDto(updated);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>Only GENERATED and APPROVED payrolls can be cancelled.
	 * PAID is a finalised financial transaction and cannot be reversed.
	 * CANCELLED is idempotent-guarded (no double cancellation).
	 */
	@Override
	@Transactional
	public PayrollResponse cancelPayroll(final Long id) {
		log.info("[PayrollService] cancelPayroll() ► payrollId={}", id);

		Payroll payroll = findPayrollOrThrow(id);
		PayrollStatus currentStatus = payroll.getPayrollStatus();

		if (currentStatus == PayrollStatus.PAID) {
			log.warn("[PayrollService] cancelPayroll() ✗ Cannot cancel PAID payroll. payrollId={}", id);
			throw new PayrollOperationException(
					messageByLocaleService.getMessage("payroll.cancel.paid.not.allowed", null));
		}

		if (currentStatus == PayrollStatus.CANCELLED) {
			log.warn("[PayrollService] cancelPayroll() ✗ Payroll is already CANCELLED. payrollId={}", id);
			throw new PayrollOperationException(
					messageByLocaleService.getMessage("payroll.already.cancelled", null));
		}

		payroll.setPayrollStatus(PayrollStatus.CANCELLED);
		Payroll updated = payrollRepository.save(payroll);

		log.info("[PayrollService] cancelPayroll() ✔ Payroll CANCELLED. payrollId={}, previousStatus={}",
				id, currentStatus);
		return payrollMapper.entityToDto(updated);
	}

	@Override
	@Transactional(readOnly = true)
	public PayrollResponse getPayrollById(final Long id) {
		log.debug("[PayrollService] getPayrollById() ► payrollId={}", id);
		return payrollMapper.entityToDto(findPayrollOrThrow(id));
	}

	@Override
	@Transactional(readOnly = true)
	public List<PayrollResponse> getPayrollsByEmployee(final Long employeeId) {
		log.debug("[PayrollService] getPayrollsByEmployee() ► employeeId={}", employeeId);

		/**
		 *  Validate employee existence before running payroll query
		 */
		if (!employeeRepository.existsById(employeeId)) {
			log.warn("[PayrollService] getPayrollsByEmployee() ✗ Employee not found. employeeId={}", employeeId);
			throw new EmployeeNotFoundException(
					messageByLocaleService.getMessage("employee.not.found", new Object[]{employeeId}));
		}

		List<Payroll> payrolls = payrollRepository.findByEmployeeIdOrderByYearDescMonthDesc(employeeId);
		log.debug("[PayrollService] getPayrollsByEmployee() ► Found {} payroll(s) for employeeId={}",
				payrolls.size(), employeeId);

		return payrollMapper.toDtos(payrolls);
	}

	@Override
	@Transactional(readOnly = true)
	public List<PayrollResponse> getPayrollsByMonthAndYear(final Integer month, final Integer year) {
		log.debug("[PayrollService] getPayrollsByMonthAndYear() ► month={}, year={}", month, year);

		List<Payroll> payrolls = payrollRepository.findByMonthAndYear(month, year);
		log.debug("[PayrollService] getPayrollsByMonthAndYear() ► Found {} payroll(s) for {}/{}",
				payrolls.size(), month, year);

		return payrollMapper.toDtos(payrolls);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<PayrollResponse> getAllPayrolls(final PayrollSearchRequest request, final Pageable pageable) {
		log.info("[PayrollService] getAllPayrolls() ► Filtering with request: {}, pageable: {}", request, pageable);

		org.springframework.data.jpa.domain.Specification<Payroll> spec = PayrollSpecification.getSpecification(request);
		Page<Payroll> payrollPage = payrollRepository.findAll(spec, pageable);

		log.debug("[PayrollService] getAllPayrolls() ► Fetched {} records on page {}", payrollPage.getNumberOfElements(), payrollPage.getNumber());

		return payrollPage.map(payrollMapper::entityToDto);
	}

	/**
	 *  SALARY SLIP OPERATIONS
	 */

	/**
	 *
	 * <p><b>Business rules enforced:</b>
	 * <ul>
	 *   <li>Payroll must be in APPROVED or PAID status</li>
	 *   <li>Only one salary slip allowed per payroll (one-to-one)</li>
	 *   <li>Slip path is auto-generated as: {@code slips/{empId}/{year}/{month}/salary-slip-{payrollId}.pdf}</li>
	 * </ul>
	 */
	@Override
	@Transactional
	public SalarySlipResponse generateSalarySlip(final Long payrollId) {
		log.info("[PayrollService] generateSalarySlip() ► payrollId={}", payrollId);

		/**
		 *  Use JOIN FETCH to eagerly load employee and department for PDF generation
		 */
		Payroll payroll = payrollRepository.findByIdWithEmployeeAndDepartment(payrollId)
				.orElseThrow(() -> {
					log.warn("[PayrollService] generateSalarySlip() ✗ Payroll not found. payrollId={}", payrollId);
					return new PayrollNotFoundException(
							messageByLocaleService.getMessage("payroll.not.found", new Object[]{payrollId}));
				});

		/**
		 * Payroll must be APPROVED or PAID 
		 */
		if (payroll.getPayrollStatus() != PayrollStatus.APPROVED
				&& payroll.getPayrollStatus() != PayrollStatus.PAID) {
			log.warn("[PayrollService] generateSalarySlip() ✗ Invalid payroll status. payrollId={}, status={}",
					payrollId, payroll.getPayrollStatus());
			throw new PayrollOperationException(
					messageByLocaleService.getMessage("salary.slip.payroll.invalid.status",
							new Object[]{payroll.getPayrollStatus()}));
		}

		/**
		 * One slip per payroll (one-to-one integrity) 
		 */
		if (salarySlipRepository.existsByPayrollId(payrollId)) {
			log.info("[PayrollService] generateSalarySlip() ► Salary slip already exists. Returning existing slip for payrollId={}", payrollId);
			SalarySlip existingSlip = salarySlipRepository.findByPayrollId(payrollId)
					.orElseThrow(() -> new SalarySlipNotFoundException("Salary slip record not found for payroll ID: " + payrollId));
			return salarySlipMapper.entityToDto(existingSlip);
		}

		/**
		 *  Build relative slip path (used as download key)
		 */
		Employee employee = payroll.getEmployee();
		String relPath = String.format("%d/%d/%02d/salary-slip-%d.pdf",
				employee.getId(), payroll.getYear(), payroll.getMonth(), payrollId);

		/**
		 *  Write actual PDF to disk
		 */
		Path outputPath = Paths.get(storageDir).resolve(relPath).toAbsolutePath();
		try {
			Files.createDirectories(outputPath.getParent());
			pdfGeneratorService.generate(payroll, outputPath);
		} catch (IOException e) {
			log.error("[PayrollService] generateSalarySlip() ✗ Failed to write PDF. payrollId={}", payrollId, e);
			throw new PayrollOperationException("Failed to generate PDF salary slip: " + e.getMessage());
		}

		/**
		 * Persist salary slip entity 
		 */
		SalarySlip salarySlip = new SalarySlip();
		salarySlip.setPayroll(payroll);
		salarySlip.setSlipPath(relPath);
		salarySlip.setGeneratedDate(LocalDateTime.now());

		SalarySlip saved = salarySlipRepository.save(salarySlip);
		log.info("[PayrollService] generateSalarySlip() ✔ PDF slip saved. slipId={}, payrollId={}, path={}",
				saved.getId(), payrollId, outputPath);

		return salarySlipMapper.entityToDto(saved);
	}

	@Override
	@Transactional(readOnly = true)
	public SalarySlipResponse getSalarySlipByPayroll(final Long payrollId) {
		log.debug("[PayrollService] getSalarySlipByPayroll() ► payrollId={}", payrollId);

		/**
		 *  Validate payroll exists to give a meaningful 404 (not a generic slip-not-found)
		 */
		if (!payrollRepository.existsById(payrollId)) {
			log.warn("[PayrollService] getSalarySlipByPayroll() ✗ Payroll not found. payrollId={}", payrollId);
			throw new PayrollNotFoundException(
					messageByLocaleService.getMessage("payroll.not.found", new Object[]{payrollId}));
		}

		SalarySlip slip = salarySlipRepository.findByPayrollId(payrollId)
				.orElseThrow(() -> {
					log.warn("[PayrollService] getSalarySlipByPayroll() ✗ Salary slip not yet generated. payrollId={}",
							payrollId);
					return new SalarySlipNotFoundException(
							messageByLocaleService.getMessage("salary.slip.not.found",
									new Object[]{payrollId}));
				});

		return salarySlipMapper.entityToDto(slip);
	}

	@Override
	@Transactional(readOnly = true)
	public List<SalarySlipResponse> getSalarySlipsByEmployee(final Long employeeId) {
		log.debug("[PayrollService] getSalarySlipsByEmployee() ► employeeId={}", employeeId);

		if (!employeeRepository.existsById(employeeId)) {
			log.warn("[PayrollService] getSalarySlipsByEmployee() ✗ Employee not found. employeeId={}", employeeId);
			throw new EmployeeNotFoundException(
					messageByLocaleService.getMessage("employee.not.found", new Object[]{employeeId}));
		}

		List<SalarySlip> slips = salarySlipRepository
				.findByPayroll_Employee_IdOrderByGeneratedDateDesc(employeeId);
		log.debug("[PayrollService] getSalarySlipsByEmployee() ► Found {} slip(s) for employeeId={}",
				slips.size(), employeeId);

		return salarySlipMapper.toDtos(slips);
	}

	/**
	 * Centralized payroll lookup with consistent warn-logging + exception creation.
	 * Avoids repeating the same orElseThrow block across every method.
	 *
	 * @param id the payroll primary key
	 * @return the found {@link Payroll} entity (never null)
	 * @throws PayrollNotFoundException if no record exists with the given id
	 */
	private Payroll findPayrollOrThrow(final Long id) {
		return payrollRepository.findById(id)
				.orElseThrow(() -> {
					log.warn("[PayrollService] findPayrollOrThrow() ✗ Payroll not found. payrollId={}", id);
					return new PayrollNotFoundException(
							messageByLocaleService.getMessage("payroll.not.found", new Object[]{id}));
				});
	}

	@Override
	public BulkPayrollResponse generatePayrollForAll(BulkPayrollRequest request) {
		log.info("[PayrollService] generatePayrollForAll() - month={}, year={}", request.getMonth(), request.getYear());

		List<Employee> activeEmployees = employeeRepository.findByEmployeeStatus(EmployeeStatus.ACTIVE);
		List<BulkPayrollResponse.BulkPayrollDetail> details = new ArrayList<>();
		int generated = 0, skipped = 0, failed = 0;

		for (Employee emp : activeEmployees) {
			try {
				PayrollGenerateRequest genReq = new PayrollGenerateRequest();
				genReq.setEmployeeId(emp.getId());
				genReq.setMonth(request.getMonth());
				genReq.setYear(request.getYear());
				genReq.setBonus(request.getBonus());
				genReq.setDeductions(request.getDeductions());

				generatePayroll(genReq);
				generated++;
				details.add(BulkPayrollResponse.BulkPayrollDetail.builder()
						.employeeId(emp.getId())
						.employeeName(emp.getFirstName() + " " + emp.getLastName())
						.status("GENERATED")
						.message("Payroll generated successfully")
						.build());
			} catch (PayrollAlreadyExistsException e) {
				skipped++;
				details.add(BulkPayrollResponse.BulkPayrollDetail.builder()
						.employeeId(emp.getId())
						.employeeName(emp.getFirstName() + " " + emp.getLastName())
						.status("SKIPPED")
						.message("Payroll already exists for this period")
						.build());
			} catch (Exception e) {
				failed++;
				log.error("Failed to generate payroll for empId={}: {}", emp.getId(), e.getMessage());
				details.add(BulkPayrollResponse.BulkPayrollDetail.builder()
						.employeeId(emp.getId())
						.employeeName(emp.getFirstName() + " " + emp.getLastName())
						.status("FAILED")
						.message(e.getMessage())
						.build());
			}
		}

		return BulkPayrollResponse.builder()
				.totalEmployees(activeEmployees.size())
				.generated(generated)
				.skipped(skipped)
				.failed(failed)
				.details(details)
				.build();
	}
}
