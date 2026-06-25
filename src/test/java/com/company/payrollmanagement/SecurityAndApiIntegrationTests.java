package com.company.payrollmanagement;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.epms.constant.AttendanceStatus;
import com.epms.dto.request.AttendanceRequestDto;
import com.epms.dto.request.DepartmentRequest;
import com.epms.dto.request.LoginRequest;
import com.epms.dto.request.RegisterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.epms.repository.UserRepository;
import com.epms.repository.EmployeeRepository;
import com.epms.repository.DepartmentRepository;
import com.jayway.jsonpath.JsonPath;

/**
 * SecurityAndApiIntegrationTests is a comprehensive MockMvc test suite.
 * It simulates real HTTP REST client requests to verify:
 * 1. Authentication & Token generation.
 * 2. Input validations (blank fields, invalid emails, numeric bounds).
 * 3. Role-Based Access Control (RBAC) (preventing EMPLOYEE from modifying departments).
 * 4. Custom JSON error payloads on 401 and 403 status codes.
 * 5. Business logic validations (such as invalid attendance hours or future dates).
 */
@SpringBootTest(classes = com.epms.PayrollManagementSystemApplication.class, properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureMockMvc
@Transactional // Automatically rolls back database mutations after every test case
class SecurityAndApiIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private EmployeeRepository employeeRepository;

	@Autowired
	private DepartmentRepository departmentRepository;

	private String adminToken;
	private String employeeToken;
	private Long testEmployeeId;

	@BeforeEach
	void setUp() throws Exception {
		// Clean up pre-existing test_employee to avoid database pollution
		userRepository.findByUsername("test_employee").ifPresent(user -> {
			userRepository.delete(user);
			userRepository.flush();
		});

		// Seed a test department and employee
		com.epms.entity.Department dept = new com.epms.entity.Department();
		dept.setName("IntegrationTestDept");
		dept.setLocation("Floor 1");
		dept.setActive(true);
		dept = departmentRepository.save(dept);

		com.epms.entity.Employee emp = new com.epms.entity.Employee();
		emp.setFirstName("Test");
		emp.setLastName("User");
		emp.setEmail("employee@test.com");
		emp.setPhone("1234567890");
		emp.setDesignation("Tester");
		emp.setBaseSalary(BigDecimal.valueOf(50000.0));
		emp.setJoiningDate(LocalDate.now().minusDays(10));
		emp.setEmployeeStatus(com.epms.constant.EmployeeStatus.ACTIVE);
		emp.setDepartment(dept);
		emp.setActive(true);
		emp = employeeRepository.save(emp);
		testEmployeeId = emp.getId();

		// Log in as seeded Admin (created by DatabaseSeeder on start) to get token
		LoginRequest adminLogin = new LoginRequest();
		adminLogin.setUsername("admin");
		adminLogin.setPassword("admin123");

		MvcResult adminResult = mockMvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(adminLogin)))
				.andExpect(status().isOk())
				.andReturn();

		String adminResponseStr = adminResult.getResponse().getContentAsString();
		adminToken = JsonPath.read(adminResponseStr, "$.data.token");

		// Register and Login a standard employee user to get employee token
		RegisterRequest registerEmployee = new RegisterRequest();
		registerEmployee.setUsername("test_employee");
		registerEmployee.setEmail("employee@test.com");
		registerEmployee.setPassword("testpwd123");

		mockMvc.perform(post("/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(registerEmployee)))
				.andExpect(status().isCreated());

		LoginRequest empLogin = new LoginRequest();
		empLogin.setUsername("test_employee");
		empLogin.setPassword("testpwd123");

		MvcResult empResult = mockMvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(empLogin)))
				.andExpect(status().isOk())
				.andReturn();

		String empResponseStr = empResult.getResponse().getContentAsString();
		employeeToken = JsonPath.read(empResponseStr, "$.data.token");
	}

	@Test
	void testUnauthenticatedAccessLockout_Expect401() throws Exception {
		// Test access to protected endpoint without auth header
		mockMvc.perform(get("/employees"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.error").value("Unauthorized"))
				.andExpect(jsonPath("$.message").exists());
	}

	@Test
	void testInvalidLoginCredentials_Expect401() throws Exception {
		LoginRequest badLogin = new LoginRequest();
		badLogin.setUsername("admin");
		badLogin.setPassword("wrongpassword");

		mockMvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(badLogin)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.message").exists());
	}

	@Test
	void testDuplicateUserRegistration_ExpectValidationException() throws Exception {
		// Register a user that already exists (admin)
		RegisterRequest duplicateReg = new RegisterRequest();
		duplicateReg.setUsername("admin"); // Duplicate
		duplicateReg.setEmail("unique@company.com");
		duplicateReg.setPassword("admin123");

		mockMvc.perform(post("/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(duplicateReg)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400));
	}

	@Test
	void testAdminAuthorizations_CanManageDepartments_ExpectSuccess() throws Exception {
		DepartmentRequest dept = new DepartmentRequest();
		dept.setName("Quality Assurance");
		dept.setLocation("Floor 5");

		// Admin has full access to create departments
		mockMvc.perform(post("/departments")
				.header("Authorization", "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(dept)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.name").value("Quality Assurance"));
	}

	@Test
	void testEmployeeAuthorizations_CannotManageDepartments_Expect403Forbidden() throws Exception {
		DepartmentRequest dept = new DepartmentRequest();
		dept.setName("Unauthorized Dept");
		dept.setLocation("Floor 1");

		// Standard employee is prohibited from creating departments
		mockMvc.perform(post("/departments")
				.header("Authorization", "Bearer " + employeeToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(dept)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.status").value(403))
				.andExpect(jsonPath("$.message").value("Access denied: You do not have permission to access this resource"));
	}

	@Test
	void testAttendanceBusinessValidation_FutureDate_ExpectBadRequest() throws Exception {
		AttendanceRequestDto requestDto = new AttendanceRequestDto();
		requestDto.setEmployeeId(testEmployeeId);
		requestDto.setAttendanceDate(LocalDate.now().plusDays(2)); // Future date
		requestDto.setAttendanceStatus(AttendanceStatus.PRESENT);
		requestDto.setWorkingHours(BigDecimal.valueOf(8));

		mockMvc.perform(post("/attendances/mark")
				.header("Authorization", "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestDto)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400));
	}

	@Test
	void testAttendanceBusinessValidation_InvalidHoursForPresent_ExpectBadRequest() throws Exception {
		AttendanceRequestDto requestDto = new AttendanceRequestDto();
		requestDto.setEmployeeId(testEmployeeId);
		requestDto.setAttendanceDate(LocalDate.now());
		requestDto.setAttendanceStatus(AttendanceStatus.PRESENT);
		requestDto.setWorkingHours(BigDecimal.valueOf(0)); // Zero hours for Present status

		mockMvc.perform(post("/attendances/mark")
				.header("Authorization", "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestDto)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400));
	}
}
