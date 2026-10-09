# 🏢 Employee Leave Request System

### A Production-Grade Core Java Console Application

A robust, menu-driven Employee Leave Request System built using **Java JDK 21+** and Object-Oriented Programming principles. The application manages employee registration, leave submissions, approval workflows, calendar conflict detection, and administrative reporting with **atomic file persistence and transactional rollback**.

![Java](https://img.shields.io/badge/Java-JDK%2021%2B-orange?logo=openjdk)
![Architecture](https://img.shields.io/badge/Architecture-Layered-blue)
![Dependencies](https://img.shields.io/badge/Dependencies-Zero-green)
![Testing](https://img.shields.io/badge/Tests-25%20Cases-brightgreen)
![Storage](https://img.shields.io/badge/Storage-File--Based-lightgrey)

---

## 📌 Table of Contents

- [Overview](#-overview)
- [Problem Statement](#-problem-statement)
- [Key Features](#-key-features)
- [Technology Stack](#-technology-stack)
- [System Architecture](#-system-architecture)
- [OOP Principles](#-object-oriented-programming-principles)
- [Business Rules](#-business-rules)
- [Leave Overlap Detection](#-leave-overlap-detection)
- [Data Storage and Reliability](#-data-storage-and-reliability)
- [Project Structure](#-project-structure)
- [Getting Started](#-getting-started)
- [Application Workflow](#-application-workflow)
- [Testing](#-testing)
- [Future Enhancements](#-future-enhancements)
- [Author](#-author)

---

## 📖 Overview

The Employee Leave Request System replaces manual leave management with a structured, file-based Java application.

It provides separate modules for **Managers** and **Employees**, ensuring that administrative decisions and employee self-service operations follow clearly defined business rules.

The system emphasizes clean code, data integrity, defensive input validation, reliable file handling, and maintainable object-oriented design.

### 🎯 Project Objectives

- Simplify employee leave submission and approval.
- Prevent overlapping approved leave requests for the same employee.
- Maintain a complete leave request lifecycle.
- Persist employee and leave information without an external database.
- Protect stored data using temporary-file staging and atomic file replacement.
- Validate business rules and recover safely from invalid input or storage errors.

---

## 🚨 Problem Statement

Traditional manual leave management can result in:

- **Scheduling conflicts:** Multiple overlapping leave approvals for one employee.
- **Limited visibility:** Difficulty tracking pending, approved, and rejected requests.
- **Data inconsistency:** Accidental overwrites or incomplete file updates.
- **Poor auditability:** Lack of structured historical leave records.
- **Manual reporting:** Extra effort required to calculate leave statistics.

### 💡 Proposed Solution

A layered Core Java application that provides role-based workflows, date-based validation, conflict detection, persistent file storage, and automated administrative reports.

---

## ✨ Key Features

### 👨‍💼 Manager Module

- Register new employees with automatically generated positive IDs.
- View registered employee information.
- Inspect organization-wide leave requests across all statuses.
- Approve or reject pending leave requests.
- Prevent approval when an employee has conflicting approved leave.
- Generate administrative analytics and export reports to `data/report.txt`.

### 👨‍💻 Employee Module

- Access the employee module using an employee ID.
- View personal profile information.
- Submit leave requests with start date, end date, category, and reason.
- View personal leave history and current request statuses.
- Receive validation for invalid dates, durations, categories, and empty reasons.

### 🛡️ Reliability and Validation

- Atomic file-write strategy with fallback handling.
- In-memory rollback when persistence operations fail.
- UTF-8 file storage.
- Startup validation of stored records and employee references.
- Defensive console input handling.
- Automated tests covering boundary cases and application behavior.

---

## 🛠️ Technology Stack

| Technology | Purpose |
|---|---|
| Java JDK 21+ | Core application development |
| Object-Oriented Programming | Maintainable domain design |
| Java Collections Framework | Employee and leave request management |
| Java Time API | Date calculations and validation |
| Java NIO (`java.nio.file`) | File operations and atomic moves |
| Java Enums | Type-safe leave request statuses |
| File-based storage | Persistent records without a database |
| Custom Java test suite | Automated functional and edge-case testing |

**External dependencies:** None. The project uses Java's standard library without Spring, third-party libraries, or an external database.

---

## 🏗️ System Architecture

The project follows a clean, layered architecture that separates domain models, business logic, file operations, and user interaction.

| Layer | Responsibility |
|---|---|
| `model` | Domain entities and leave status representation |
| `service` | Business rules, approval workflows, conflict checks, and rollback |
| `util` | File persistence, atomic writes, and integrity validation |
| `main` | Console interface, menu navigation, and input parsing |
| `test` | Automated test cases and end-to-end workflow verification |

### Architecture Flow

```text
             User
              |
       Console Interface
              |
        Main Application
              |
         LeaveService
          /        \
  Validation    Business Rules
          \        /
         FileManager
              |
       UTF-8 Flat Files
              |
       employees.txt
       requests.txt
        report.txt
```

---

## 🧠 Object-Oriented Programming Principles

### 1. Abstraction
An abstract `User` class defines shared identity information and the abstract `showProfile()` method.

### 2. Inheritance
`Manager` and `Employee` extend `User`, reusing shared attributes while implementing role-specific behavior.

### 3. Polymorphism
The `showProfile()` method is overridden by the subclasses, allowing dynamic method dispatch based on the actual user type.

### 4. Encapsulation
Class fields are private, and state changes are performed through controlled methods.

### 5. Type Safety
The `LeaveStatus` enum restricts request statuses to:

- `PENDING`
- `APPROVED`
- `REJECTED`

This prevents invalid status strings and improves code clarity.

---

## 📋 Business Rules

| Rule | Validation |
|---|---|
| Minimum leave duration | 1 calendar day |
| Maximum leave duration | 10 calendar days |
| Date order | End date must not precede start date |
| Duration calculation | Inclusive of both start and end dates |
| Allowed categories | Personal, Medical, Other |
| Conflict validation | Only approved requests reserve dates |
| Employee isolation | Conflicts are checked for the same employee only |
| Employee ID | Positive, automatically generated identifier |
| Empty reason | Rejected |
| Invalid date format | Handled without crashing the application |

### Inclusive Leave Duration

The application calculates the total number of calendar days using:

```java
long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
```

For example, leave from October 10 to October 12 counts as **3 calendar days**. Weekends and holidays are included.

---

## 📅 Leave Overlap Detection

One of the system's core business rules is that **only approved leave requests reserve calendar dates**.

Pending and rejected requests do not block new submissions.

### Overlap Formula

Two date intervals overlap when neither interval ends before the other begins.

```java
boolean overlaps =
    !request.getEndDate().isBefore(approved.getStartDate())
    && !request.getStartDate().isAfter(approved.getEndDate());
```

*Adjust getter names if your implementation uses different method names.*

### Expected Behavior

| Scenario | Result |
|---|---|
| Same employee, overlapping pending requests | Allowed |
| Same employee, overlapping approved requests | Approval refused |
| Different employees, identical dates | Allowed |
| New request overlaps a rejected request | Allowed |
| New request does not overlap approved leave | Approval allowed |

This design prevents double-booking without unnecessarily restricting requests that have not been approved.

---

## 💾 Data Storage and Reliability

The system stores information in UTF-8, pipe-delimited text files inside the `data/` directory.

### Storage Format

**`employees.txt`**

```text
id|name|department
101|Rahul Sharma|Engineering
102|Priya Reddy|Human Resources
```

**`requests.txt`**

```text
id|employeeId|start|end|type|reason|status
1|101|2026-10-10|2026-10-12|Personal|Family function|PENDING
```

**`report.txt`**

Contains administrative statistics such as request counts by status, approved calendar days, and distinct employees with approved leave.

*The records above are illustrative examples; actual file contents depend on application data.*

### Atomic File Persistence

The application uses a staged-write strategy to reduce the risk of partial file updates.

1. Write the updated content to a temporary file.
2. Attempt to replace the target using `Files.move()` with `StandardCopyOption.ATOMIC_MOVE`.
3. Use a fallback move strategy if atomic replacement is unsupported.
4. Restore the previous in-memory state if persistence fails.

### Startup Integrity Validation

The application validates stored records for:

- Incorrect field formatting.
- Invalid identifiers and values.
- References to nonexistent employees.
- Conflicting historical approved leave, according to the implemented integrity rules.

**Reliability note:** Atomic replacement protects individual file updates where the filesystem supports it. Full crash consistency across multiple files requires additional transaction or recovery mechanisms.

---

## 📂 Project Structure

```text
Employee-Leave-Request/
│
├── src/
│   └── com/
│       └── employeeleave/
│           ├── model/
│           │   ├── User.java
│           │   ├── Manager.java
│           │   ├── Employee.java
│           │   ├── LeaveRequest.java
│           │   └── LeaveStatus.java
│           │
│           ├── service/
│           │   └── LeaveService.java
│           │
│           ├── util/
│           │   └── FileManager.java
│           │
│           ├── main/
│           │   └── Main.java
│           │
│           └── test/
│               ├── ProjectTestSuite.java
│               └── DemoFlowVerifier.java
│
├── data/
│   ├── employees.txt
│   ├── requests.txt
│   └── report.txt
│
└── README.md
```

*The structure above represents the intended package organization. Update filenames or paths to match your actual repository.*

---

## 🚀 Getting Started

### Prerequisites

- Java Development Kit (JDK) 21 or later.
- A terminal or command prompt.
- Git (optional, for cloning the repository).

Verify Java installation:

```bash
java -version
javac -version
```

### 1. Clone the Repository

Replace the placeholder URL with your actual GitHub repository URL.

```bash
git clone https://github.com/YOUR_USERNAME/Employee-Leave-Request.git
cd Employee-Leave-Request
```

### 2. Compile the Application

If your Java source files are stored in `src/`, compile them into an output directory:

```bash
javac -d out -sourcepath src src/com/employeeleave/main/Main.java
```

This command assumes `Main.java` is the entry point and Java source files follow the package structure shown above.

### 3. Run the Application

```bash
java -cp out com.employeeleave.main.Main
```

The application should display the main menu and allow users to access the manager or employee workflows.

### 4. Run the Automated Tests

If your test classes are self-contained Java programs, compile and run them using your project's actual source paths.

For example:

```bash
javac -d out -sourcepath src src/com/employeeleave/test/ProjectTestSuite.java
java -cp out com.employeeleave.test.ProjectTestSuite
```

Repeat the process for `DemoFlowVerifier` if it has a separate `main()` method.

> **Note:** The commands assume the package names and file locations shown in this README. If your tests depend on additional source files or use a different directory structure, compile all required Java sources together.

---

## 🔄 Application Workflow

### Manager Workflow

1. Open the application.
2. Select the Manager module.
3. Register employees when necessary.
4. Review submitted leave requests.
5. Approve or reject pending requests.
6. Review organization-wide leave metrics.
7. Export the management report.

### Employee Workflow

1. Open the application.
2. Select the Employee module.
3. Enter the employee ID.
4. View personal profile information.
5. Submit a leave request with the required details.
6. View the personal leave history and updated status.

---

## 🧪 Testing and Quality Assurance

The project includes a custom automated test suite designed to cover 25 edge cases, along with a demo-flow verifier for a 13-step scenario.

### Test Coverage Areas

- **Date boundaries:** One-day leave, ten-day leave, eleven-day rejection, and inverted dates.
- **Input validation:** Invalid date formats, blank names, empty reasons, and invalid categories.
- **Request lifecycle:** Approvals, rejections, repeated operations, and status validation.
- **Overlap detection:** Pending requests, approved conflicts, and requests belonging to different employees.
- **Persistence:** Missing files, malformed records, application restarts, and stored data recovery.
- **Failure handling:** Rollback when a file-write operation fails.

### Reported Verification Results

| Metric | Result |
|---|---:|
| Automated test cases | 25 |
| Passed | 25 |
| Failed | 0 |
| Pass rate | 100% |

These are the project's reported results. Keep the 100% pass-rate claim only while the current test suite continues to pass. Test-case execution is not, by itself, proof of 100% code coverage.

---

## 📊 Administrative Analytics

The reporting module tracks useful organization-wide leave statistics, including:

- Total pending requests.
- Total approved requests.
- Total rejected requests.
- Approved calendar days.
- Number of distinct employees with approved leave.

The generated report is stored at:

```text
data/report.txt
```

This provides managers with a convenient summary of leave activity without requiring an external reporting tool or database.

---

## 🔮 Future Enhancements

Potential extensions include:

- Password-based authentication and secure password hashing using PBKDF2.
- Department-specific leave quotas and leave balance tracking.
- Holiday calendars and business-day calculations.
- Manager and employee audit logs.
- More comprehensive crash recovery and multi-file transaction handling.
- Migration from flat files to a relational database.
- REST API integration.
- Web-based user interface.
- CI automation and expanded code coverage reporting.

---

## 🎓 Learning Outcomes

This project demonstrates practical application of:

- Core Java and modern Java APIs.
- OOP principles and clean package organization.
- Business rule implementation and defensive programming.
- Date interval calculations and scheduling conflict detection.
- File I/O, atomic replacement, and error recovery.
- Custom automated testing and edge-case validation.
- Role-based application workflows and report generation.

---

## 👨‍💻 Author

**Ponnana Sri Sai Manikantha**

Computer Science and Engineering Student | Java Developer

Interested in Core Java, object-oriented software development, problem-solving, and building reliable applications.

---

## ⭐ Support

If you find this project useful for learning Core Java, OOP, file handling, or software design, consider giving the repository a ⭐ on GitHub.

**Built with Java. Designed for reliability. Focused on clean software engineering.**
