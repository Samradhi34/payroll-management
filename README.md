# EMSPro — Employee Payroll Management System

EMSPro is a production-ready, full-stack Employee Payroll Management System (HRMS) built using **Spring Boot 3.x** on the backend and a lightweight **Vanilla HTML/CSS/JavaScript SPA (Single Page Application)** architecture on the frontend. It features secure JWT authentication, role-based access control (RBAC), automatic daily/monthly payroll calculations, and automated salary slip PDF generation.

---

## Key Features

- **Dynamic SPA Interface**: Smooth routing and view transitions using Vanilla JS, completely responsive for desktop, tablet, and mobile screens.
- **Secure Authentication**: JWT-based stateless login sessions with password hashing (BCrypt) and Spring Security filters.
- **Role-Based Access Control (RBAC)**:
  - `ADMIN`: Full authority over departments, employees, attendance logs, and payroll runs (approval, cancellation, disbursement).
  - `HR`: Can generate payroll draft runs, manage employees, and view departments.
  - `EMPLOYEE`: Access restricted to self-profile, self-attendance history, and downloading self-salary slips.
- **Automated Payroll Lifecycle**:
  - **GENERATED**: Payroll generated as a draft for the current period (with pro-rated joining/resignation calculations).
  - **APPROVED**: Admin verifies and approves the payroll.
  - **PAID**: Financial disbursement completed.
  - **CANCELLED**: Cancelled runs (not allowed once marked PAID).
- **Salary Slip PDFs**: Professional A4 salary slip generation utilizing Apache PDFBox (generated on-the-fly and cached for retrieval).
- **Daily Automated Attendance Seeder**: Automated cron jobs check and seed attendance records daily.

---

## Technology Stack

### Backend
- **Core**: Java 21, Spring Boot 3.x (Web, Security, JPA, Validation)
- **Database**: PostgreSQL
- **Security**: JWT (JSON Web Tokens) & Spring Security
- **PDF Generation**: Apache PDFBox
- **JSON Mapping**: MapStruct & Lombok
- **External API**: Cloudinary (for employee profile image storage)

### Frontend
- **Structure**: Semantic HTML5 & Vanilla CSS (variables, modular design system, responsive media queries)
- **Behavior**: Pure JavaScript SPA (client-side hash router, state store, async fetch API wrapper)
- **Design**: SLEEK dark navigation drawer, glassmorphic cards, transition animations, dynamic toast notifications.

---

## Folder Structure

```text
payroll-management/
├── .mvn/                  # Maven Wrapper configuration
├── src/
│   ├── main/
│   │   ├── java/com/epms/
│   │   │   ├── config/      # Database seeds, security configs, schedulers
│   │   │   ├── constant/    # Enums (PayrollStatus, EmployeeStatus, etc.)
│   │   │   ├── controller/  # REST APIs
│   │   │   ├── dto/         # Request & Response wrappers
│   │   │   ├── entity/      # JPA entities
│   │   │   ├── exception/   # Custom exceptions & global handlers
│   │   │   ├── mapper/      # MapStruct DTO converters
│   │   │   └── repository/  # JPA Spring Data repositories
│   │   │   └── response/    # Unified REST response handlers
│   │   │   └── service/     # Business logic & implementations
│   │   └── resources/
│   │       ├── application.properties  # App configurations
│   │       ├── messages.properties     # Multi-lingual locale keys
│   │       └── static/                 # Front-end Assets
│   │           ├── css/                # Variables, Base, Components styles
│   │           ├── js/                 # SPA code (pages, store, router, API)
│   │           └── index.html          # Main SPA landing page
│   └── test/              # JUnit integration/unit tests
├── pom.xml                # Maven dependencies
└── README.md              # Project documentation
```

---

## Installation & Setup

### Prerequisites
1. **Java Development Kit (JDK)**: v21 or higher.
2. **PostgreSQL**: Local running instance.
3. **Maven**: Optional (Maven Wrapper is included).

### Step 1: Database Configuration
1. Open PostgreSQL and create a database named `payroll-management`:
   ```sql
   CREATE DATABASE "payroll-management";
   ```
2. Adjust your Postgres credentials in `src/main/resources/application.properties` or set them via environment variables:
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/payroll-management
   spring.datasource.username=your_postgres_username
   spring.datasource.password=your_postgres_password
   ```

### Step 2: Cloudinary Setup (Optional)
If you wish to enable profile image uploads, supply your Cloudinary account details:
```properties
CLOUDINARY_CLOUD_NAME=your_cloud_name
CLOUDINARY_API_KEY=your_api_key
CLOUDINARY_API_SECRET=your_api_secret
```

### Step 3: Run the Application
Start the Spring Boot server using the Maven wrapper:

**Windows Command Prompt / PowerShell:**
```powershell
./mvnw spring-boot:run
```

**macOS / Linux:**
```bash
./mvnw spring-boot:run
```

Once started, the backend server and static frontend SPA will be accessible at:
- **Application URL**: [http://localhost:8080](http://localhost:8080)

---

## Running with Docker (Recommended)

You can run the entire application stack (Spring Boot app + PostgreSQL database) easily using Docker.

### Prerequisites
- [Docker Desktop](https://www.docker.com/products/docker-desktop/) installed and running.

### 1. Start the Containers
Open your terminal in the project root directory and run:
```bash
docker compose up --build -d
```
This builds the Spring Boot application image and starts both the application and the PostgreSQL database container in the background.

### 2. Restore Seed Data (First Time Only)
Since the container database starts empty, you need to import the test data. Run the following command in your terminal:

**Windows PowerShell:**
```powershell
Get-Content seed_data.sql | docker exec -i postgres-db psql -U postgres -d payroll-management
```

**macOS / Linux:**
```bash
docker exec -i postgres-db psql -U postgres -d payroll-management < seed_data.sql
```

### 3. Access the App
Once the containers are running, navigate to:
- **Application URL**: [http://localhost:8080](http://localhost:8080)

### 4. Stop the Containers
To stop and remove the active containers:
```bash
docker compose down
```
*Note: Your data is persisted inside a Docker volume named `pgdata` and will not be deleted when you stop the containers.*

---

## Default Login Credentials
Upon startup, the database is automatically seeded with default user profiles:

- **Admin Account**:
  - **Username**: `admin`
  - **Password**: `admin123`
- **HR Account**:
  - **Username**: `hr_user`
  - **Password**: `hr123`
- **Employee Account**:
  - **Username**: `employee`
  - **Password**: `emp123`

---

## Production Security Check
Before deploying to production or making your repository public:
1. **Externalize Secrets**: Never commit Cloudinary API secrets or production database credentials to version control. Pass them through system environment variables or secure cloud vaults.
2. **Ignored folders**: Ensure that `salary-slips/` (containing physical employee PDFs) and `src/main/resources/static/logs/` (containing debug log files) are listed in `.gitignore` (which is already configured).
