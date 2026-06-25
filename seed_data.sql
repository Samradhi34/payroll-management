-- 1. Truncate previous data in correct dependency order
TRUNCATE TABLE salary_slip, payroll, attendance, leave_request, leave_balance, user_roles, users, roles, employee, department, holiday CASCADE;

-- Reset sequences to ensure clean starting IDs
ALTER SEQUENCE department_id_seq RESTART WITH 1;
ALTER SEQUENCE employee_id_seq RESTART WITH 1;
ALTER SEQUENCE leave_balance_id_seq RESTART WITH 1;
ALTER SEQUENCE leave_request_id_seq RESTART WITH 1;
ALTER SEQUENCE attendance_id_seq RESTART WITH 1;
ALTER SEQUENCE payroll_id_seq RESTART WITH 1;
ALTER SEQUENCE salary_slip_id_seq RESTART WITH 1;
ALTER SEQUENCE holiday_id_seq RESTART WITH 1;
ALTER SEQUENCE users_id_seq RESTART WITH 1;
ALTER SEQUENCE roles_id_seq RESTART WITH 1;

-- 2. Seed Roles
INSERT INTO roles (id, role_type, active, created_at, updated_at) VALUES
(1, 'ADMIN', true, NOW(), NOW()),
(2, 'HR', true, NOW(), NOW()),
(3, 'EMPLOYEE', true, NOW(), NOW());

SELECT setval('roles_id_seq', (SELECT MAX(id) FROM roles));

-- 3. Seed Users (Matching Employees 1 to 55)
-- Admin Password: admin123  -> $2a$10$SVjJXZElW8nFLnDiZtkFNOKboeSuOHdbmM4owDlmLXE5QRZLK8MKW
-- HR Password: hrpwd123     -> $2a$10$OHc/BfFC1M79795LYIbVr.eJyWoFIDdnknBYw/uV8VrCWODWUGHxC
-- Employee Password: emppwd123 -> $2a$10$JotM4Z6RcMRWzm2olsJdDOgQOcwfAJ82Lxub8zgD1qRhJsp/pZsYq
INSERT INTO users (id, username, email, password_hash, enabled, active, created_at, updated_at) VALUES
(1, 'admin', 'admin@company.com', '$2a$10$SVjJXZElW8nFLnDiZtkFNOKboeSuOHdbmM4owDlmLXE5QRZLK8MKW', true, true, NOW(), NOW()),
(2, 'hr1', 'hr1@company.com', '$2a$10$OHc/BfFC1M79795LYIbVr.eJyWoFIDdnknBYw/uV8VrCWODWUGHxC', true, true, NOW(), NOW()),
(3, 'hr2', 'hr2@company.com', '$2a$10$OHc/BfFC1M79795LYIbVr.eJyWoFIDdnknBYw/uV8VrCWODWUGHxC', true, true, NOW(), NOW()),
(4, 'emp4', 'emp4@company.com', '$2a$10$JotM4Z6RcMRWzm2olsJdDOgQOcwfAJ82Lxub8zgD1qRhJsp/pZsYq', true, true, NOW(), NOW()),
(5, 'emp5', 'emp5@company.com', '$2a$10$JotM4Z6RcMRWzm2olsJdDOgQOcwfAJ82Lxub8zgD1qRhJsp/pZsYq', true, true, NOW(), NOW()),
(6, 'emp6', 'emp6@company.com', '$2a$10$JotM4Z6RcMRWzm2olsJdDOgQOcwfAJ82Lxub8zgD1qRhJsp/pZsYq', true, true, NOW(), NOW()),
(7, 'emp7', 'emp7@company.com', '$2a$10$JotM4Z6RcMRWzm2olsJdDOgQOcwfAJ82Lxub8zgD1qRhJsp/pZsYq', true, true, NOW(), NOW()),
(8, 'emp8', 'emp8@company.com', '$2a$10$JotM4Z6RcMRWzm2olsJdDOgQOcwfAJ82Lxub8zgD1qRhJsp/pZsYq', true, true, NOW(), NOW()),
(9, 'emp9', 'emp9@company.com', '$2a$10$JotM4Z6RcMRWzm2olsJdDOgQOcwfAJ82Lxub8zgD1qRhJsp/pZsYq', true, true, NOW(), NOW()),
(10, 'emp10', 'emp10@company.com', '$2a$10$JotM4Z6RcMRWzm2olsJdDOgQOcwfAJ82Lxub8zgD1qRhJsp/pZsYq', true, true, NOW(), NOW()),
(11, 'emp11', 'emp11@company.com', '$2a$10$JotM4Z6RcMRWzm2olsJdDOgQOcwfAJ82Lxub8zgD1qRhJsp/pZsYq', true, true, NOW(), NOW()),
(12, 'emp12', 'emp12@company.com', '$2a$10$JotM4Z6RcMRWzm2olsJdDOgQOcwfAJ82Lxub8zgD1qRhJsp/pZsYq', true, true, NOW(), NOW()),
(13, 'emp13', 'emp13@company.com', '$2a$10$JotM4Z6RcMRWzm2olsJdDOgQOcwfAJ82Lxub8zgD1qRhJsp/pZsYq', true, true, NOW(), NOW()),
(14, 'emp14', 'emp14@company.com', '$2a$10$JotM4Z6RcMRWzm2olsJdDOgQOcwfAJ82Lxub8zgD1qRhJsp/pZsYq', true, true, NOW(), NOW()),
(15, 'emp15', 'emp15@company.com', '$2a$10$JotM4Z6RcMRWzm2olsJdDOgQOcwfAJ82Lxub8zgD1qRhJsp/pZsYq', true, true, NOW(), NOW()),
(16, 'emp16', 'emp16@company.com', '$2a$10$JotM4Z6RcMRWzm2olsJdDOgQOcwfAJ82Lxub8zgD1qRhJsp/pZsYq', true, true, NOW(), NOW()),
(17, 'emp17', 'emp17@company.com', '$2a$10$JotM4Z6RcMRWzm2olsJdDOgQOcwfAJ82Lxub8zgD1qRhJsp/pZsYq', true, true, NOW(), NOW()),
(18, 'emp18', 'emp18@company.com', '$2a$10$JotM4Z6RcMRWzm2olsJdDOgQOcwfAJ82Lxub8zgD1qRhJsp/pZsYq', true, true, NOW(), NOW()),
(19, 'emp19', 'emp19@company.com', '$2a$10$JotM4Z6RcMRWzm2olsJdDOgQOcwfAJ82Lxub8zgD1qRhJsp/pZsYq', true, true, NOW(), NOW()),
(20, 'emp20', 'emp20@company.com', '$2a$10$JotM4Z6RcMRWzm2olsJdDOgQOcwfAJ82Lxub8zgD1qRhJsp/pZsYq', true, true, NOW(), NOW()),
(21, 'emp21', 'emp21@company.com', '$2a$10$JotM4Z6RcMRWzm2olsJdDOgQOcwfAJ82Lxub8zgD1qRhJsp/pZsYq', true, true, NOW(), NOW()),
(22, 'emp22', 'emp22@company.com', '$2a$10$JotM4Z6RcMRWzm2olsJdDOgQOcwfAJ82Lxub8zgD1qRhJsp/pZsYq', true, true, NOW(), NOW()),
(23, 'emp23', 'emp23@company.com', '$2a$10$JotM4Z6RcMRWzm2olsJdDOgQOcwfAJ82Lxub8zgD1qRhJsp/pZsYq', true, true, NOW(), NOW()),
(24, 'emp24', 'emp24@company.com', '$2a$10$JotM4Z6RcMRWzm2olsJdDOgQOcwfAJ82Lxub8zgD1qRhJsp/pZsYq', true, true, NOW(), NOW()),
(25, 'emp25', 'emp25@company.com', '$2a$10$JotM4Z6RcMRWzm2olsJdDOgQOcwfAJ82Lxub8zgD1qRhJsp/pZsYq', true, true, NOW(), NOW()),
(26, 'emp26', 'emp26@company.com', '$2a$10$JotM4Z6RcMRWzm2olsJdDOgQOcwfAJ82Lxub8zgD1qRhJsp/pZsYq', true, true, NOW(), NOW()),
(27, 'emp27', 'emp27@company.com', '$2a$10$JotM4Z6RcMRWzm2olsJdDOgQOcwfAJ82Lxub8zgD1qRhJsp/pZsYq', true, true, NOW(), NOW()),
(28, 'emp28', 'emp28@company.com', '$2a$10$JotM4Z6RcMRWzm2olsJdDOgQOcwfAJ82Lxub8zgD1qRhJsp/pZsYq', true, true, NOW(), NOW()),
(29, 'emp29', 'emp29@company.com', '$2a$10$JotM4Z6RcMRWzm2olsJdDOgQOcwfAJ82Lxub8zgD1qRhJsp/pZsYq', true, true, NOW(), NOW()),
(30, 'emp30', 'emp30@company.com', '$2a$10$JotM4Z6RcMRWzm2olsJdDOgQOcwfAJ82Lxub8zgD1qRhJsp/pZsYq', true, true, NOW(), NOW()),
(31, 'emp31', 'emp31@company.com', '$2a$10$JotM4Z6RcMRWzm2olsJdDOgQOcwfAJ82Lxub8zgD1qRhJsp/pZsYq', true, true, NOW(), NOW()),
(32, 'emp32', 'emp32@company.com', '$2a$10$JotM4Z6RcMRWzm2olsJdDOgQOcwfAJ82Lxub8zgD1qRhJsp/pZsYq', true, true, NOW(), NOW()),
(33, 'emp33', 'emp33@company.com', '$2a$10$JotM4Z6RcMRWzm2olsJdDOgQOcwfAJ82Lxub8zgD1qRhJsp/pZsYq', true, true, NOW(), NOW()),
(34, 'emp34', 'emp34@company.com', '$2a$10$JotM4Z6RcMRWzm2olsJdDOgQOcwfAJ82Lxub8zgD1qRhJsp/pZsYq', true, true, NOW(), NOW()),
(35, 'emp35', 'emp35@company.com', '$2a$10$JotM4Z6RcMRWzm2olsJdDOgQOcwfAJ82Lxub8zgD1qRhJsp/pZsYq', true, true, NOW(), NOW()),
(36, 'emp36', 'emp36@company.com', '$2a$10$JotM4Z6RcMRWzm2olsJdDOgQOcwfAJ82Lxub8zgD1qRhJsp/pZsYq', true, true, NOW(), NOW()),
(37, 'emp37', 'emp37@company.com', '$2a$10$JotM4Z6RcMRWzm2olsJdDOgQOcwfAJ82Lxub8zgD1qRhJsp/pZsYq', true, true, NOW(), NOW()),
(38, 'emp38', 'emp38@company.com', '$2a$10$JotM4Z6RcMRWzm2olsJdDOgQOcwfAJ82Lxub8zgD1qRhJsp/pZsYq', true, true, NOW(), NOW()),
(39, 'emp39', 'emp39@company.com', '$2a$10$JotM4Z6RcMRWzm2olsJdDOgQOcwfAJ82Lxub8zgD1qRhJsp/pZsYq', true, true, NOW(), NOW()),
(40, 'emp40', 'emp40@company.com', '$2a$10$JotM4Z6RcMRWzm2olsJdDOgQOcwfAJ82Lxub8zgD1qRhJsp/pZsYq', true, true, NOW(), NOW()),
(41, 'emp41', 'emp41@company.com', '$2a$10$JotM4Z6RcMRWzm2olsJdDOgQOcwfAJ82Lxub8zgD1qRhJsp/pZsYq', true, true, NOW(), NOW()),
(42, 'emp42', 'emp42@company.com', '$2a$10$JotM4Z6RcMRWzm2olsJdDOgQOcwfAJ82Lxub8zgD1qRhJsp/pZsYq', true, true, NOW(), NOW()),
(43, 'emp43', 'emp43@company.com', '$2a$10$JotM4Z6RcMRWzm2olsJdDOgQOcwfAJ82Lxub8zgD1qRhJsp/pZsYq', true, true, NOW(), NOW()),
(44, 'emp44', 'emp44@company.com', '$2a$10$JotM4Z6RcMRWzm2olsJdDOgQOcwfAJ82Lxub8zgD1qRhJsp/pZsYq', true, true, NOW(), NOW()),
(45, 'emp45', 'emp45@company.com', '$2a$10$JotM4Z6RcMRWzm2olsJdDOgQOcwfAJ82Lxub8zgD1qRhJsp/pZsYq', true, true, NOW(), NOW()),
(46, 'emp46', 'emp46@company.com', '$2a$10$JotM4Z6RcMRWzm2olsJdDOgQOcwfAJ82Lxub8zgD1qRhJsp/pZsYq', true, true, NOW(), NOW()),
(47, 'emp47', 'emp47@company.com', '$2a$10$JotM4Z6RcMRWzm2olsJdDOgQOcwfAJ82Lxub8zgD1qRhJsp/pZsYq', true, true, NOW(), NOW()),
(48, 'emp48', 'emp48@company.com', '$2a$10$JotM4Z6RcMRWzm2olsJdDOgQOcwfAJ82Lxub8zgD1qRhJsp/pZsYq', true, true, NOW(), NOW()),
(49, 'emp49', 'emp49@company.com', '$2a$10$JotM4Z6RcMRWzm2olsJdDOgQOcwfAJ82Lxub8zgD1qRhJsp/pZsYq', true, true, NOW(), NOW()),
(50, 'emp50', 'emp50@company.com', '$2a$10$JotM4Z6RcMRWzm2olsJdDOgQOcwfAJ82Lxub8zgD1qRhJsp/pZsYq', true, true, NOW(), NOW()),
(51, 'emp51', 'emp51@company.com', '$2a$10$JotM4Z6RcMRWzm2olsJdDOgQOcwfAJ82Lxub8zgD1qRhJsp/pZsYq', true, true, NOW(), NOW()),
(52, 'emp52', 'emp52@company.com', '$2a$10$JotM4Z6RcMRWzm2olsJdDOgQOcwfAJ82Lxub8zgD1qRhJsp/pZsYq', true, true, NOW(), NOW()),
(53, 'emp53', 'emp53@company.com', '$2a$10$JotM4Z6RcMRWzm2olsJdDOgQOcwfAJ82Lxub8zgD1qRhJsp/pZsYq', true, true, NOW(), NOW()),
(54, 'emp54', 'emp54@company.com', '$2a$10$JotM4Z6RcMRWzm2olsJdDOgQOcwfAJ82Lxub8zgD1qRhJsp/pZsYq', true, true, NOW(), NOW()),
(55, 'emp55', 'emp55@company.com', '$2a$10$JotM4Z6RcMRWzm2olsJdDOgQOcwfAJ82Lxub8zgD1qRhJsp/pZsYq', true, true, NOW(), NOW());

SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));

-- 4. Seed User Roles Mappings
INSERT INTO user_roles (user_id, role_id) VALUES
(1, 1), -- admin  -> ADMIN
(2, 2), -- hr1    -> HR
(3, 2), -- hr2    -> HR
(4, 3), (5, 3), (6, 3), (7, 3), (8, 3), (9, 3), (10, 3),
(11, 3), (12, 3), (13, 3), (14, 3), (15, 3), (16, 3), (17, 3),
(18, 3), (19, 3), (20, 3), (21, 3), (22, 3), (23, 3), (24, 3),
(25, 3), (26, 3), (27, 3), (28, 3), (29, 3), (30, 3), (31, 3),
(32, 3), (33, 3), (34, 3), (35, 3), (36, 3), (37, 3), (38, 3),
(39, 3), (40, 3), (41, 3), (42, 3), (43, 3), (44, 3), (45, 3),
(46, 3), (47, 3), (48, 3), (49, 3), (50, 3), (51, 3), (52, 3),
(53, 3), (54, 3), (55, 3);

-- 5. Seed Departments
INSERT INTO department (id, active, dept_name, location, dept_desc, created_at, updated_at) VALUES
(1, true, 'Engineering', 'Floor 4', 'Software Engineering & IT', NOW(), NOW()),
(2, true, 'Human Resources', 'Floor 2', 'People Operations & Recruiting', NOW(), NOW()),
(3, true, 'Finance', 'Floor 3', 'Accounting & Financial Analyst Team', NOW(), NOW()),
(4, true, 'Marketing', 'Floor 1', 'Growth & Digital Marketing Team', NOW(), NOW()),
(5, true, 'Operations', 'Floor 1', 'Operations & Office Logistics', NOW(), NOW());

SELECT setval('department_id_seq', (SELECT MAX(id) FROM department));

-- 6. Seed Employees (55 Employees)
-- Fields: id, active, base_salary, department_id, designation, email, employee_status, first_name, last_name, phone, joining_date, resignation_date, manager_id, created_at, updated_at
INSERT INTO employee (id, active, base_salary, department_id, designation, email, employee_status, first_name, last_name, phone, joining_date, resignation_date, manager_id, created_at, updated_at) VALUES
-- Admin Employee (reports to none)
(1, true, 120000.00, 1, 'Chief Technical Officer', 'admin@company.com', 'ACTIVE', 'CTO', 'Admin', '9990001111', '2020-01-01', NULL, NULL, NOW(), NOW()),

-- HR Employees (2) - Report to CTO
(2, true, 80000.00, 2, 'HR Manager', 'hr1@company.com', 'ACTIVE', 'Sarah', 'Connor', '9990001112', '2021-06-01', NULL, 1, NOW(), NOW()),
(3, true, 75000.00, 2, 'HR Recruiter', 'hr2@company.com', 'ACTIVE', 'David', 'Miller', '9990001113', '2022-03-15', NULL, 1, NOW(), NOW()),

-- Tech Leads / Managers
(4, true, 95000.00, 1, 'Tech Lead', 'emp4@company.com', 'ACTIVE', 'James', 'Kowalski', '9990001114', '2021-10-10', NULL, 1, NOW(), NOW()),
(9, true, 90000.00, 3, 'Finance Manager', 'emp9@company.com', 'ACTIVE', 'William', 'Jones', '9990001119', '2021-01-10', NULL, 1, NOW(), NOW()),
(12, true, 72000.00, 4, 'Marketing Director', 'emp12@company.com', 'ACTIVE', 'Isabella', 'Davis', '9990001122', '2021-07-20', NULL, 1, NOW(), NOW()),
(15, true, 65000.00, 5, 'Operations Lead', 'emp15@company.com', 'ACTIVE', 'Henry', 'Lopez', '9990001125', '2022-05-18', NULL, 1, NOW(), NOW()),

-- Engineering Team (Report to Tech Lead 4)
(5, true, 70000.00, 1, 'Senior Developer', 'emp5@company.com', 'ACTIVE', 'Robert', 'Martin', '9990001115', '2022-02-20', NULL, 4, NOW(), NOW()),
(6, true, 55000.00, 1, 'Junior Developer', 'emp6@company.com', 'ACTIVE', 'Emily', 'Watson', '9990001116', '2023-01-15', NULL, 4, NOW(), NOW()),
(7, true, 62000.00, 1, 'QA Engineer', 'emp7@company.com', 'ACTIVE', 'Michael', 'Johnson', '9990001117', '2023-08-01', NULL, 4, NOW(), NOW()),
(8, true, 48000.00, 1, 'UI/UX Designer', 'emp8@company.com', 'ACTIVE', 'Sophia', 'Brown', '9990001118', '2024-02-10', NULL, 4, NOW(), NOW()),
(17, true, 85000.00, 1, 'DevOps Engineer', 'emp17@company.com', 'ACTIVE', 'Joseph', 'Wilson', '9990001127', '2022-09-01', NULL, 1, NOW(), NOW()),
(18, true, 50000.00, 1, 'System Administrator', 'emp18@company.com', 'ACTIVE', 'Amelia', 'Anderson', '9990001128', '2023-05-15', NULL, 1, NOW(), NOW()),
(19, true, 68000.00, 1, 'Scrum Master', 'emp19@company.com', 'ACTIVE', 'Benjamin', 'Thomas', '9990001129', '2022-08-10', NULL, 1, NOW(), NOW()),
(20, true, 46000.00, 1, 'Support Specialist', 'emp20@company.com', 'ACTIVE', 'Evelyn', 'Taylor', '9990001130', '2024-01-20', NULL, 4, NOW(), NOW()),
(21, true, 58000.00, 1, 'Database Administrator', 'emp21@company.com', 'ACTIVE', 'Samuel', 'Moore', '9990001131', '2023-07-01', NULL, 4, NOW(), NOW()),
(28, true, 71000.00, 1, 'Backend Specialist', 'emp28@company.com', 'ACTIVE', 'Lily', 'Harris', '9990001138', '2023-03-01', NULL, 4, NOW(), NOW()),
(29, true, 73000.00, 1, 'Frontend Specialist', 'emp29@company.com', 'ACTIVE', 'Ryan', 'Sanchez', '9990001139', '2023-03-10', NULL, 4, NOW(), NOW()),
(30, true, 56000.00, 1, 'QA Automation', 'emp30@company.com', 'ACTIVE', 'Chloe', 'Clark', '9990001140', '2023-12-15', NULL, 4, NOW(), NOW()),

-- Finance Team (Report to Finance Manager 9)
(10, true, 60000.00, 3, 'Senior Analyst', 'emp10@company.com', 'ACTIVE', 'Olivia', 'Garcia', '9990001120', '2022-11-01', NULL, 9, NOW(), NOW()),
(11, true, 45000.00, 3, 'Accountant', 'emp11@company.com', 'ACTIVE', 'Alexander', 'Martinez', '9990001121', '2023-04-12', NULL, 9, NOW(), NOW()),
(24, true, 78000.00, 3, 'Risk Manager', 'emp24@company.com', 'ACTIVE', 'Ella', 'Lee', '9990001134', '2022-04-05', NULL, 9, NOW(), NOW()),
(25, true, 48000.00, 3, 'Billing Clerk', 'emp25@company.com', 'ACTIVE', 'Logan', 'Perez', '9990001135', '2023-12-01', NULL, 9, NOW(), NOW()),

-- Marketing Team (Report to Marketing Director 12)
(13, true, 50000.00, 4, 'SEO Specialist', 'emp13@company.com', 'ACTIVE', 'Daniel', 'Rodriguez', '9990001123', '2023-09-05', NULL, 12, NOW(), NOW()),
(14, true, 40000.00, 4, 'Content Writer', 'emp14@company.com', 'ACTIVE', 'Mia', 'Hernandez', '9990001124', '2024-03-01', NULL, 12, NOW(), NOW()),
(22, true, 52000.00, 4, 'Social Media Lead', 'emp22@company.com', 'ACTIVE', 'Harper', 'Jackson', '9990001132', '2023-02-14', NULL, 12, NOW(), NOW()),
(23, true, 44000.00, 4, 'Graphic Designer', 'emp23@company.com', 'ACTIVE', 'Lucas', 'Martin', '9990001133', '2024-04-01', NULL, 12, NOW(), NOW()),
(33, true, 60000.00, 4, 'PR Executive', 'emp33@company.com', 'ACTIVE', 'Danielle', 'Hill', '9990001143', '2023-06-18', NULL, 12, NOW(), NOW()),

-- Operations Team (Report to Operations Lead 15)
(16, true, 42000.00, 5, 'Coordinator', 'emp16@company.com', 'ACTIVE', 'Charlotte', 'Gonzalez', '9990001126', '2023-11-22', NULL, 15, NOW(), NOW()),
(26, true, 64000.00, 5, 'Inventory Controller', 'emp26@company.com', 'ACTIVE', 'Avery', 'Thompson', '9990001136', '2022-07-22', NULL, 15, NOW(), NOW()),
(27, true, 43000.00, 5, 'Logistics Officer', 'emp27@company.com', 'ACTIVE', 'Jackson', 'White', '9990001137', '2023-10-15', NULL, 15, NOW(), NOW()),

-- HR Department Staff (Report to HR Manager 2)
(31, true, 50000.00, 2, 'HR Recruiter', 'emp31@company.com', 'ACTIVE', 'Luke', 'Ramirez', '9990001141', '2024-01-10', NULL, 2, NOW(), NOW()),
(32, true, 53000.00, 2, 'HR Coordinator', 'emp32@company.com', 'ACTIVE', 'Zoey', 'Lewis', '9990001142', '2024-02-15', NULL, 2, NOW(), NOW()),

-- Additional General Active Employees (To reach headcount requirement)
(44, true, 54000.00, 1, 'Support Tier 2', 'emp44@company.com', 'ACTIVE', 'Liam', 'Parker', '9990001154', '2024-05-12', NULL, 4, NOW(), NOW()),
(45, true, 56000.00, 1, 'Security Analyst', 'emp45@company.com', 'ACTIVE', 'Oliver', 'Evans', '9990001155', '2024-06-01', NULL, 4, NOW(), NOW()),

-- Edge Cases: Mid-Month Joiners in June 2026 (5 Employees)
(34, true, 60000.00, 1, 'Developer', 'emp34@company.com', 'ACTIVE', 'Matthew', 'Allen', '9990001144', '2026-06-05', NULL, 4, NOW(), NOW()),
(35, true, 90000.00, 1, 'Tech Architect', 'emp35@company.com', 'ACTIVE', 'Natalie', 'Young', '9990001145', '2026-06-10', NULL, 1, NOW(), NOW()),
(36, true, 60000.00, 1, 'Developer', 'emp36@company.com', 'ACTIVE', 'Joshua', 'King', '9990001146', '2026-06-15', NULL, 4, NOW(), NOW()),
(37, true, 80000.00, 3, 'Auditor', 'emp37@company.com', 'ACTIVE', 'Grace', 'Wright', '9990001147', '2026-06-20', NULL, 9, NOW(), NOW()),
(38, true, 45000.00, 4, 'Copywriter', 'emp38@company.com', 'ACTIVE', 'Andrew', 'Scott', '9990001148', '2026-06-25', NULL, 12, NOW(), NOW()),

-- Edge Cases: Mid-Month Resignations in June 2026 (5 Employees)
(39, true, 60000.00, 1, 'QA Analyst', 'emp39@company.com', 'RESIGNED', 'Hannah', 'Green', '9990001149', '2023-01-01', '2026-06-05', 4, NOW(), NOW()),
(40, true, 90000.00, 1, 'Lead Engineer', 'emp40@company.com', 'RESIGNED', 'Christian', 'Baker', '9990001150', '2022-05-01', '2026-06-10', 1, NOW(), NOW()),
(41, true, 60000.00, 1, 'QA Analyst', 'emp41@company.com', 'RESIGNED', 'Elizabeth', 'Adams', '9990001151', '2023-06-15', '2026-06-15', 4, NOW(), NOW()),
(42, true, 80000.00, 3, 'Controller', 'emp42@company.com', 'RESIGNED', 'Tyler', 'Nelson', '9990001152', '2021-08-10', '2026-06-20', 9, NOW(), NOW()),
(43, true, 45000.00, 4, 'Designer', 'emp43@company.com', 'RESIGNED', 'Victoria', 'Carter', '9990001153', '2024-01-01', '2026-06-25', 12, NOW(), NOW()),

-- Edge Cases: Inactive / Terminated Employees (5 Employees)
(46, false, 55000.00, 1, 'Junior Developer', 'emp46@company.com', 'TERMINATED', 'Leo', 'Mitchell', '9990001156', '2023-11-01', '2026-05-20', 4, NOW(), NOW()),
(47, false, 72000.00, 3, 'Senior Accountant', 'emp47@company.com', 'TERMINATED', 'Sophia', 'Collins', '9990001157', '2022-10-15', '2026-05-30', 9, NOW(), NOW()),
(48, false, 48000.00, 4, 'Media Specialist', 'emp48@company.com', 'TERMINATED', 'Julian', 'Stewart', '9990001158', '2024-02-01', '2026-06-01', 12, NOW(), NOW()),
(49, false, 67000.00, 1, 'Cloud Engineer', 'emp49@company.com', 'TERMINATED', 'Zoe', 'Morris', '9990001159', '2023-04-01', '2026-06-08', 4, NOW(), NOW()),
(50, false, 43000.00, 5, 'Facilities Clerk', 'emp50@company.com', 'TERMINATED', 'Owen', 'Rogers', '9990001160', '2024-03-01', '2026-06-12', 15, NOW(), NOW()),

-- Edge Cases: Low Gross Income Slabs (Base Salary <= 20000 for Tax Exempt Verification) (5 Employees)
(51, true, 18000.00, 5, 'Office Assistant', 'emp51@company.com', 'ACTIVE', 'Nora', 'Reed', '9990001161', '2024-09-01', NULL, 15, NOW(), NOW()),
(52, true, 19000.00, 5, 'Support staff', 'emp52@company.com', 'ACTIVE', 'Eli', 'Cook', '9990001162', '2024-10-01', NULL, 15, NOW(), NOW()),
(53, true, 20000.00, 5, 'Mail Coordinator', 'emp53@company.com', 'ACTIVE', 'Stella', 'Bell', '9990001163', '2024-11-01', NULL, 15, NOW(), NOW()),
(54, true, 15000.00, 5, 'Intern Dev', 'emp54@company.com', 'ACTIVE', 'Aaron', 'Ward', '9990001164', '2026-05-01', NULL, 4, NOW(), NOW()),
(55, true, 17500.00, 4, 'Intern Marketing', 'emp55@company.com', 'ACTIVE', 'Hazel', 'Watson', '9990001165', '2026-05-15', NULL, 12, NOW(), NOW());

SELECT setval('employee_id_seq', (SELECT MAX(id) FROM employee));

-- 7. Seed Leave Balances for all 55 employees for the year 2026
INSERT INTO leave_balance (employee_id, year, casual_leave_balance, sick_leave_balance, earned_leave_balance, active, created_at, updated_at)
SELECT id, 2026, 12.0, 10.0, 15.0, true, NOW(), NOW() FROM employee;

SELECT setval('leave_balance_id_seq', (SELECT MAX(id) FROM leave_balance));

-- 8. Seed some initial Holidays for 2026 (Exclusions during attendance & leave calculations)
INSERT INTO holiday (id, name, holiday_date, active, created_at, updated_at) VALUES
(1, 'New Year Day', '2026-01-01', true, NOW(), NOW()),
(2, 'Republic Day', '2026-01-26', true, NOW(), NOW()),
(3, 'Good Friday', '2026-04-03', true, NOW(), NOW()),
(4, 'Independence Day', '2026-08-15', true, NOW(), NOW()),
(5, 'Christmas', '2026-12-25', true, NOW(), NOW());

SELECT setval('holiday_id_seq', (SELECT MAX(id) FROM holiday));
