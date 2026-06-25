package com.company.payrollmanagement;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.epms.constant.*;
import com.epms.dto.request.*;
import com.epms.dto.response.*;
import com.epms.entity.*;
import com.epms.repository.*;
import com.epms.service.*;

@SpringBootTest(classes = com.epms.PayrollManagementSystemApplication.class, properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Transactional
class PayrollAndAttendanceModuleTests {

	@Autowired
	private LeaveService leaveService;

	@Autowired
	private AttendanceService attendanceService;

	@Autowired
	private PayrollService payrollService;

	@Autowired
	private EmployeeRepository employeeRepository;

	@Autowired
	private DepartmentRepository departmentRepository;

	@Autowired
	private HolidayRepository holidayRepository;

	@Autowired
	private LeaveBalanceRepository leaveBalanceRepository;

	@Autowired
	private LeaveRequestRepository leaveRequestRepository;

	@Autowired
	private AttendanceRepository attendanceRepository;

	@Autowired
	private PayrollRepository payrollRepository;

	@Test
	void testLeaveApplicationAndApproval() {
		// 1. Setup Employee
		Department dept = new Department();
		dept.setName("EngineeringTest");
		dept.setLocation("Building A");
		dept.setActive(true);
		dept = departmentRepository.save(dept);

		Employee emp = new Employee();
		emp.setFirstName("John");
		emp.setLastName("Doe");
		emp.setEmail("john.doe@example.com");
		emp.setPhone("1234567890");
		emp.setDesignation("Developer");
		emp.setBaseSalary(BigDecimal.valueOf(60000.0));
		emp.setJoiningDate(LocalDate.of(2026, 6, 1));
		emp.setEmployeeStatus(EmployeeStatus.ACTIVE);
		emp.setDepartment(dept);
		emp.setActive(true);
		emp = employeeRepository.save(emp);

		// 2. Check initial balances
		LeaveBalanceResponseDto initialBalance = leaveService.getLeaveBalance(emp.getId(), 2026);
		assertEquals(0, BigDecimal.valueOf(12.0).compareTo(initialBalance.getCasualLeaveBalance()));

		// 3. Apply Leave
		LeaveRequestDto applyDto = new LeaveRequestDto();
		applyDto.setEmployeeId(emp.getId());
		applyDto.setLeaveType(LeaveType.CASUAL_LEAVE);
		applyDto.setStartDate(LocalDate.of(2026, 6, 15));
		applyDto.setEndDate(LocalDate.of(2026, 6, 16)); // 2 days
		applyDto.setReason("Personal");

		LeaveResponseDto applyRes = leaveService.applyLeave(applyDto);
		assertEquals(LeaveStatus.PENDING, applyRes.getLeaveStatus());

		// 4. Approve Leave
		LeaveResponseDto approvedRes = leaveService.approveLeave(applyRes.getId(), "admin");
		assertEquals(LeaveStatus.APPROVED, approvedRes.getLeaveStatus());

		// 5. Check balance is deducted
		LeaveBalanceResponseDto updatedBalance = leaveService.getLeaveBalance(emp.getId(), 2026);
		assertEquals(0, BigDecimal.valueOf(10.0).compareTo(updatedBalance.getCasualLeaveBalance()));

		// 6. Check attendance records are automatically created
		List<Attendance> attendances = attendanceRepository.findByEmployeeIdAndAttendanceDateBetween(
				emp.getId(), LocalDate.of(2026, 6, 15), LocalDate.of(2026, 6, 16));
		assertEquals(2, attendances.size());
		for (Attendance att : attendances) {
			assertEquals(AttendanceStatus.PAID_LEAVE, att.getAttendanceStatus());
			assertEquals(0, BigDecimal.ZERO.compareTo(att.getWorkingHours()));
		}
	}

	@Test
	void testAttendanceCheckInAndCheckOut() {
		// 1. Setup Employee
		Department dept = new Department();
		dept.setName("HRTest");
		dept.setLocation("Building B");
		dept.setActive(true);
		dept = departmentRepository.save(dept);

		Employee emp = new Employee();
		emp.setFirstName("Jane");
		emp.setLastName("Doe");
		emp.setEmail("jane.doe@example.com");
		emp.setPhone("0987654321");
		emp.setDesignation("HR Specialist");
		emp.setBaseSalary(BigDecimal.valueOf(50000.0));
		emp.setJoiningDate(LocalDate.of(2026, 6, 1));
		emp.setEmployeeStatus(EmployeeStatus.ACTIVE);
		emp.setDepartment(dept);
		emp.setActive(true);
		emp = employeeRepository.save(emp);

		// 2. Check-in
		attendanceService.checkIn(emp.getId());
		LocalDate today = LocalDate.now();
		Attendance att = attendanceRepository.findByEmployeeIdAndAttendanceDate(emp.getId(), today).orElse(null);
		assertNotNull(att);
		assertNotNull(att.getCheckInTime());
		assertNull(att.getCheckOutTime());

		// 3. Check-out (mock/modify checkout time to represent 9 hours later)
		att.setCheckInTime(LocalTime.of(9, 0));
		attendanceRepository.save(att);

		// Now check-out using manual simulation with 8.5 hours
		AttendanceRequestDto updateDto = new AttendanceRequestDto();
		updateDto.setEmployeeId(emp.getId());
		updateDto.setAttendanceDate(today);
		updateDto.setCheckInTime(LocalTime.of(9, 0));
		updateDto.setCheckOutTime(LocalTime.of(17, 30));
		updateDto.setWorkingHours(BigDecimal.valueOf(8.5));
		updateDto.setAttendanceStatus(AttendanceStatus.PRESENT);

		attendanceService.updateAttendance(att.getId(), updateDto);

		Attendance updatedAtt = attendanceRepository.findById(att.getId()).orElse(null);
		assertNotNull(updatedAtt);
		assertEquals(0, BigDecimal.valueOf(8.5).compareTo(updatedAtt.getWorkingHours()));
		assertEquals(AttendanceStatus.PRESENT, updatedAtt.getAttendanceStatus());
	}

	@Test
	void testPayrollCalculationWithProRationAndDeductions() {
		// 1. Setup Department & Employee (joined on 16th June -> 15 active days out of 30)
		Department dept = new Department();
		dept.setName("FinanceTest");
		dept.setLocation("Building C");
		dept.setActive(true);
		dept = departmentRepository.save(dept);

		Employee emp = new Employee();
		emp.setFirstName("Bob");
		emp.setLastName("Smith");
		emp.setEmail("bob.smith@example.com");
		emp.setPhone("5551234567");
		emp.setDesignation("Accountant");
		emp.setBaseSalary(BigDecimal.valueOf(60000.0));
		emp.setJoiningDate(LocalDate.of(2026, 6, 16));
		emp.setEmployeeStatus(EmployeeStatus.ACTIVE);
		emp.setDepartment(dept);
		emp.setActive(true);
		emp = employeeRepository.save(emp);

		// 2. Set some attendance/absent records:
		// Active range: 16th June to 30th June (15 days)
		// June 2026:
		// We mock attendance:
		// - 16th: PRESENT
		// - 17th: ABSENT
		// - 18th: HALF_DAY (unpaid)
		// - 19th: PRESENT
		createAttendance(emp, LocalDate.of(2026, 6, 16), AttendanceStatus.PRESENT, BigDecimal.valueOf(8.0));
		createAttendance(emp, LocalDate.of(2026, 6, 17), AttendanceStatus.ABSENT, BigDecimal.valueOf(0.0));
		createAttendance(emp, LocalDate.of(2026, 6, 18), AttendanceStatus.HALF_DAY, BigDecimal.valueOf(4.0));
		createAttendance(emp, LocalDate.of(2026, 6, 19), AttendanceStatus.PRESENT, BigDecimal.valueOf(8.0));

		// Rest of active working days in June 2026:
		// 22nd, 23rd, 24th, 25th, 26th, 29th, 30th.
		// Let's leave them missing, they will be auto-calculated as ABSENT!
		// Weekend dates: 20th, 21st, 27th, 28th are weekend and ignored.

		// Let's calculate:
		// Active Days: 16th to 30th = 15 days.
		// Gross Salary = 60000 * 15/30 = 30000.0
		// Per Day Salary = 30000 / 30 = 1000.0
		// Working days in active period (Mon-Fri): 16(Tue), 17(Wed), 18(Thu), 19(Fri), 22(Mon), 23(Tue), 24(Wed), 25(Thu), 26(Fri), 29(Mon), 30(Tue) = 11 working days.
		// Attendance logged:
		// - 16th: PRESENT (0 deduction)
		// - 17th: ABSENT (1.0 day deduction = 1000)
		// - 18th: HALF_DAY (0.5 day deduction = 500)
		// - 19th: PRESENT (0 deduction)
		// Missing working days (treated as ABSENT):
		// - 22nd, 23rd, 24th, 25th, 26th, 29th, 30th = 7 days absent (7.0 day deduction = 7000)
		// Total Absent Days = 1 (logged) + 7 (missing) = 8 days -> Deduction = 8000
		// Total Half Days = 1 -> Deduction = 500
		// Total Attendance Deductions = 8500
		// Salary components:
		// Basic = 30000 * 0.5 = 15000
		// PF = 15000 * 0.12 = 1800
		// Tax = 30000 * 0.05 = 1500 (since gross is between 20000 and 50000)
		// Net Salary = Gross (30000) + Bonus (0) - PF (1800) - Tax (1500) - Other Deductions (0) - AttendanceDeduction (8500) = 18200

		PayrollGenerateRequest request = new PayrollGenerateRequest();
		request.setEmployeeId(emp.getId());
		request.setMonth(6);
		request.setYear(2026);
		request.setBonus(BigDecimal.ZERO);
		request.setDeductions(BigDecimal.ZERO);

		PayrollResponse payroll = payrollService.generatePayroll(request);
		assertEquals(0, BigDecimal.valueOf(30000.0).compareTo(payroll.getGrossSalary()));
		assertEquals(8, payroll.getAbsentDays());
		assertEquals(1, payroll.getHalfDays());
		assertEquals(0, BigDecimal.valueOf(8500.0).compareTo(payroll.getAttendanceDeduction()));
		assertEquals(0, BigDecimal.valueOf(1800.0).compareTo(payroll.getPf()));
		assertEquals(0, BigDecimal.valueOf(1500.0).compareTo(payroll.getTax()));
		assertEquals(0, BigDecimal.valueOf(18200.0).compareTo(payroll.getNetSalary()));
	}

	private void createAttendance(Employee emp, LocalDate date, AttendanceStatus status, BigDecimal hours) {
		Attendance att = new Attendance();
		att.setEmployee(emp);
		att.setAttendanceDate(date);
		att.setAttendanceStatus(status);
		att.setWorkingHours(hours);
		att.setApprovalStatus(ApprovalStatus.APPROVED);
		att.setActive(true);
		attendanceRepository.save(att);
	}
}
