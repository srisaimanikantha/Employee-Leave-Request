
package com.employeeleave.main;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import com.employeeleave.Util.FileManager;
import com.employeeleave.model.Employee;
import com.employeeleave.model.LeaveRequest;
import com.employeeleave.model.Manager;
import com.employeeleave.service.LeaveService;

public class Main {
    private static final Scanner scanner = new Scanner(System.in);
    private final FileManager fileManager;
    private final LeaveService leaveService;
    private final Manager manager;

    public Main() {
        this.fileManager = new FileManager();
        this.leaveService = new LeaveService(fileManager);
        this.manager = new Manager();
    }

    public static void main(String[] args) {
        Main app = new Main();
        app.run();
    }

    public void run() {
        if (!initializeSystem()) {
            return;
        }

        boolean running = true;
        while (running) {
            displayMainMenu();
            int choice = readInt("Choose: ");
            switch (choice) {
                case 1:
                    runManagerMenu();
                    break;
                case 2:
                    runEmployeeMenu();
                    break;
                case 0:
                    System.out.println("\nExiting system. Goodbye!");
                    running = false;
                    break;
                default:
                    System.out.println("Invalid option. Please choose 1, 2, or 0.");
            }
        }
    }

    public boolean initializeSystem() {
        try {
            List<Employee> loadedEmployees = fileManager.loadEmployees();
            List<LeaveRequest> loadedRequests = fileManager.loadRequests(loadedEmployees);
            leaveService.initializeData(loadedEmployees, loadedRequests);
            return true;
        } catch (FileManager.DataLoadException e) {
            System.out.println("\n==========================================");
            System.out.println("            DATA LOAD ERROR               ");
            System.out.println("==========================================");
            System.out.println("File   : " + e.getFileName());
            if (e.getLineNumber() > 0) {
                System.out.println("Line   : " + e.getLineNumber());
            }
            System.out.println("Reason : " + e.getReason());
            System.out.println("\nCorrect the file and restart.");
            return false;
        } catch (IOException e) {
            System.out.println("\n==========================================");
            System.out.println("            FILE ACCESS ERROR             ");
            System.out.println("==========================================");
            System.out.println("Error reading storage files: " + e.getMessage());
            System.out.println("Please verify directory permissions and restart.");
            return false;
        }
    }

    private void displayMainMenu() {
        System.out.println("\n==========================================");
        System.out.println("     EMPLOYEE LEAVE REQUEST SYSTEM        ");
        System.out.println("==========================================");
        System.out.println("\nMAIN MENU\n");
        System.out.println("1. Manager menu");
        System.out.println("2. Employee menu");
        System.out.println("0. Exit\n");
    }

    private void displayManagerMenu() {
        System.out.println("\n-----------------------------");
        System.out.println("MANAGER MENU");
        System.out.println("-----------------------------");
        System.out.println("1. Register employee");
        System.out.println("2. View employees");
        System.out.println("3. View leave requests");
        System.out.println("4. Approve request");
        System.out.println("5. Reject request");
        System.out.println("6. View reports");
        System.out.println("0. Back\n");
    }

    private void displayEmployeeMenu() {
        System.out.println("\n-----------------------------");
        System.out.println("EMPLOYEE MENU");
        System.out.println("-----------------------------");
        System.out.println("1. View my profile");
        System.out.println("2. Submit leave request");
        System.out.println("3. View my requests");
        System.out.println("0. Back\n");
    }

    private void runManagerMenu() {
        boolean inManager = true;
        while (inManager) {
            displayManagerMenu();
            int choice = readInt("Choose: ");
            switch (choice) {
                case 1:
                    handleRegisterEmployee();
                    break;
                case 2:
                    handleViewEmployees();
                    break;
                case 3:
                    handleViewAllRequests();
                    break;
                case 4:
                    handleApproveRequest();
                    break;
                case 5:
                    handleRejectRequest();
                    break;
                case 6:
                    handleViewReports();
                    break;
                case 0:
                    inManager = false;
                    break;
                default:
                    System.out.println("Invalid option. Please choose between 0 and 6.");
            }
        }
    }

    private void handleRegisterEmployee() {
        System.out.println("\n--- Register Employee ---");
        String name = readRequired("Employee name: ", "Employee name");
        String dept = readRequired("Department: ", "Department");

        try {
            Employee emp = leaveService.addEmployee(name, dept);
            System.out.println("\nEmployee registered successfully.");
            System.out.println("Employee ID: " + emp.getId());
        } catch (IllegalArgumentException e) {
            System.out.println("\nRegistration failed: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("\nSave failed! Unable to persist employee to data/employees.txt: " + e.getMessage());
            System.out.println("In-memory registration has been rolled back.");
        }
    }

    private void handleViewEmployees() {
        System.out.println("\nID    NAME                   DEPARTMENT     ");
        System.out.println("--------------------------------------------");
        ArrayList<Employee> employees = leaveService.getEmployees();
        if (employees.isEmpty()) {
            System.out.println("No employees registered yet.");
        } else {
            for (Employee emp : employees) {
                System.out.println(emp);
            }
        }
        System.out.println("--------------------------------------------");
    }

    private void handleViewAllRequests() {
        ArrayList<LeaveRequest> requests = leaveService.getRequests();
        printRequestTable(requests, "ALL LEAVE REQUESTS");
    }

    private void handleApproveRequest() {
        System.out.println("\n--- Approve Leave Request ---");
        int requestId = readInt("Enter request ID: ");

        try {
            LeaveRequest req = leaveService.approveRequest(requestId);
            System.out.println("\nLeave Request #" + req.getId() + " is now APPROVED.");
            System.out.println("Saved successfully.");
        } catch (IllegalArgumentException e) {
            System.out.println("\nError: " + e.getMessage());
        } catch (IllegalStateException e) {
            System.out.println("\n" + e.getMessage());
        } catch (IOException e) {
            System.out.println("\nSave failed! Unable to persist changes: " + e.getMessage());
            System.out.println("Request status has been restored to previous state.");
        }
    }

    private void handleRejectRequest() {
        System.out.println("\n--- Reject Leave Request ---");
        int requestId = readInt("Enter request ID: ");

        try {
            LeaveRequest req = leaveService.rejectRequest(requestId);
            System.out.println("\nLeave Request #" + req.getId() + " is now REJECTED.");
            System.out.println("Saved successfully.");
        } catch (IllegalArgumentException e) {
            System.out.println("\nError: " + e.getMessage());
        } catch (IllegalStateException e) {
            System.out.println("\nError: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("\nSave failed! Unable to persist changes: " + e.getMessage());
            System.out.println("Request status has been restored to previous state.");
        }
    }

    private void handleViewReports() {
        int[] counts = leaveService.calculateStatusCounts();
        int approvedDays = leaveService.calculateApprovedCalendarDays();
        int distinctApproved = leaveService.calculateDistinctEmployeesWithApprovedLeave();

        System.out.println("\n-----------------------------");
        System.out.println("LEAVE REQUEST REPORT");
        System.out.println("-----------------------------");
        System.out.println("PENDING                     : " + counts[0]);
        System.out.println("APPROVED                    : " + counts[1]);
        System.out.println("REJECTED                    : " + counts[2]);
        System.out.println("APPROVED CALENDAR DAYS      : " + approvedDays);
        System.out.println("DISTINCT APPROVED EMPLOYEES : " + distinctApproved);
        System.out.println("-----------------------------");

        System.out.println("\n1. Save report");
        System.out.println("0. Back\n");

        int choice = readInt("Choose: ");
        if (choice == 1) {
            try {
                leaveService.saveReportToFile();
                System.out.println("\nReport saved successfully to data/report.txt");
            } catch (IOException e) {
                System.out.println("\nFailed to save report: " + e.getMessage());
            }
        }
    }

    private void runEmployeeMenu() {
        int empId = readInt("\nEnter employee ID: ");
        Employee employee = leaveService.findEmployee(empId);

        if (employee == null) {
            System.out.println("Employee ID not found.");
            return;
        }

        boolean inEmployee = true;
        while (inEmployee) {
            displayEmployeeMenu();
            int choice = readInt("Choose: ");
            switch (choice) {
                case 1:
                    System.out.println("\n--- My Profile ---");
                    employee.showProfile();
                    break;
                case 2:
                    handleSubmitLeaveRequest(employee);
                    break;
                case 3:
                    handleViewMyRequests(employee);
                    break;
                case 0:
                    inEmployee = false;
                    break;
                default:
                    System.out.println("Invalid option. Please choose between 0 and 3.");
            }
        }
    }

    private void handleSubmitLeaveRequest(Employee employee) {
        System.out.println("\n--- Submit Leave Request ---");
        LocalDate start = readDate("Start date (yyyy-MM-dd): ");
        LocalDate end = readDate("End date (yyyy-MM-dd): ");

        if (end.isBefore(start)) {
            System.out.println("\nSubmission failed: End date cannot be before start date.");
            return;
        }

        long days = java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;
        if (days < 1 || days > 10) {
            System.out.println("\nSubmission failed: Leave duration must be between 1 and 10 calendar days. Requested duration: " + days + " days.");
            return;
        }

        String type = selectLeaveType();
        String reason = readRequired("Reason: ", "Reason");

        try {
            LeaveRequest req = leaveService.submitLeaveRequest(employee.getId(), start, end, type, reason);
            System.out.println("\nLeave request submitted successfully.");
            System.out.println("\nRequest ID: " + req.getId());
            System.out.println("Days: " + req.getDays());
            System.out.println("Status: " + req.getStatus());
        } catch (IllegalArgumentException e) {
            System.out.println("\nSubmission failed: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("\nSave failed! Unable to persist request to data/requests.txt: " + e.getMessage());
            System.out.println("In-memory request has been rolled back.");
        }
    }

    private void handleViewMyRequests(Employee employee) {
        ArrayList<LeaveRequest> myRequests = leaveService.getEmployeeRequests(employee.getId());
        printRequestTable(myRequests, "MY LEAVE REQUESTS (Employee #" + employee.getId() + ")");
    }

    private void printRequestTable(ArrayList<LeaveRequest> requests, String title) {
        System.out.println("\n--- " + title + " ---");
        if (requests.isEmpty()) {
            System.out.println("No leave requests found.");
            return;
        }
        System.out.println("Request ID | Employee ID | Start      | End        | Days | Type     | Reason               | Status  ");
        System.out.println("------------------------------------------------------------------------------------------------------");
        for (LeaveRequest req : requests) {
            System.out.println(req);
        }
        System.out.println("------------------------------------------------------------------------------------------------------");
    }

    public static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            if (!scanner.hasNextLine()) {
                System.out.println("\nExiting system.");
                System.exit(0);
            }
            String input = scanner.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }

    public static String readRequired(String prompt, String fieldName) {
        while (true) {
            System.out.print(prompt);
            if (!scanner.hasNextLine()) {
                System.out.println("\nExiting system.");
                System.exit(0);
            }
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                System.out.println(fieldName + " cannot be blank. Please enter again.");
                continue;
            }
            if (input.contains("|")) {
                System.out.println(fieldName + " cannot contain the '|' character. Please enter again.");
                continue;
            }
            return input;
        }
    }

    public static LocalDate readDate(String prompt) {
        while (true) {
            System.out.print(prompt);
            if (!scanner.hasNextLine()) {
                System.out.println("\nExiting system.");
                System.exit(0);
            }
            String input = scanner.nextLine().trim();
            try {
                return LocalDate.parse(input, FileManager.DATE_FORMATTER);
            } catch (DateTimeParseException e) {
                System.out.println("Invalid date. Please use yyyy-MM-dd.");
            }
        }
    }

    private String selectLeaveType() {
        while (true) {
            System.out.println("\nSelect leave type:");
            for (int i = 0; i < FileManager.VALID_LEAVE_TYPES.length; i++) {
                System.out.println((i + 1) + ". " + FileManager.VALID_LEAVE_TYPES[i]);
            }
            int choice = readInt("Choose (1-" + FileManager.VALID_LEAVE_TYPES.length + "): ");
            if (choice >= 1 && choice <= FileManager.VALID_LEAVE_TYPES.length) {
                return FileManager.VALID_LEAVE_TYPES[choice - 1];
            }
            System.out.println("Please enter a valid choice between 1 and " + FileManager.VALID_LEAVE_TYPES.length + ".");
        }
    }
}

