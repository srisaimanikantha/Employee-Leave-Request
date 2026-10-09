package com.employeeleave.service;
import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.employeeleave.Util.FileManager;
import com.employeeleave.model.Employee;
import com.employeeleave.model.LeaveRequest;
import com.employeeleave.model.LeaveStatus;

public class LeaveService {
    private final ArrayList<Employee> employees;
    private final ArrayList<LeaveRequest> requests;
    private final FileManager fileManager;

    public LeaveService(FileManager fileManager) {
        this.fileManager = fileManager;
        this.employees = new ArrayList<>();
        this.requests = new ArrayList<>();
    }

    public void initializeData(List<Employee> loadedEmployees, List<LeaveRequest> loadedRequests) {
        this.employees.clear();
        this.employees.addAll(loadedEmployees);
        this.requests.clear();
        this.requests.addAll(loadedRequests);
    }

    public ArrayList<Employee> getEmployees() {
        return employees;
    }

    public ArrayList<LeaveRequest> getRequests() {
        return requests;
    }

    public int getNextEmployeeId() {
        int maxId = 0;
        for (Employee emp : employees) {
            if (emp.getId() > maxId) {
                maxId = emp.getId();
            }
        }
        return maxId + 1;
    }

    public int getNextRequestId() {
        int maxId = 0;
        for (LeaveRequest req : requests) {
            if (req.getId() > maxId) {
                maxId = req.getId();
            }
        }
        return maxId + 1;
    }

    public void validateEmployeeInput(String name, String department) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Employee name cannot be blank.");
        }
        if (department == null || department.trim().isEmpty()) {
            throw new IllegalArgumentException("Department cannot be blank.");
        }
        if (name.contains("|")) {
            throw new IllegalArgumentException("Employee name cannot contain the delimiter '|'.");
        }
        if (department.contains("|")) {
            throw new IllegalArgumentException("Department cannot contain the delimiter '|'.");
        }
        if (name.contains("\n") || name.contains("\r") || department.contains("\n") || department.contains("\r")) {
            throw new IllegalArgumentException("Line breaks are not allowed in employee fields.");
        }
    }

    public Employee addEmployee(String name, String department) throws IOException {
        validateEmployeeInput(name, department);
        int newId = getNextEmployeeId();
        Employee employee = new Employee(newId, name, department);

        employees.add(employee);

        try {
            fileManager.saveEmployees(employees);
        } catch (IOException e) {
            employees.remove(employees.size() - 1);
            throw e;
        }

        return employee;
    }

    public Employee findEmployee(int id) {
        for (Employee emp : employees) {
            if (emp.getId() == id) {
                return emp;
            }
        }
        return null;
    }

    public LeaveRequest findLeaveRequest(int id) {
        for (LeaveRequest req : requests) {
            if (req.getId() == id) {
                return req;
            }
        }
        return null;
    }

    public void validateLeave(LocalDate start, LocalDate end, String type, String reason) {
        if (start == null) {
            throw new IllegalArgumentException("Start date is required.");
        }
        if (end == null) {
            throw new IllegalArgumentException("End date is required.");
        }
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("End date cannot be before start date.");
        }
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        if (days < 1 || days > 10) {
            throw new IllegalArgumentException("Leave duration must be between 1 and 10 calendar days. Requested duration: " + days + " days.");
        }
        if (!FileManager.isValidLeaveType(type)) {
            throw new IllegalArgumentException("Invalid leave type: '" + type + "'. Allowed types: Personal, Medical, Other.");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Reason cannot be blank.");
        }
        if (reason.contains("|")) {
            throw new IllegalArgumentException("Reason cannot contain the delimiter '|'.");
        }
        if (reason.contains("\n") || reason.contains("\r")) {
            throw new IllegalArgumentException("Line breaks are not allowed in leave reason.");
        }
    }

    public LeaveRequest submitLeaveRequest(int employeeId, LocalDate start, LocalDate end, String type, String reason) throws IOException {
        Employee employee = findEmployee(employeeId);
        if (employee == null) {
            throw new IllegalArgumentException("Employee ID " + employeeId + " not found.");
        }

        validateLeave(start, end, type, reason);

        int newId = getNextRequestId();
        String normalizedType = FileManager.normalizeLeaveType(type);
        LeaveRequest newRequest = new LeaveRequest(newId, employeeId, start, end, normalizedType, reason, LeaveStatus.PENDING);

        requests.add(newRequest);

        try {
            fileManager.saveRequests(requests);
        } catch (IOException e) {
            requests.remove(requests.size() - 1);
            throw e;
        }

        return newRequest;
    }

    public LeaveRequest checkOverlappingApprovedLeave(int employeeId, LocalDate start, LocalDate end, int excludeRequestId) {
        for (LeaveRequest req : requests) {
            if (req.getEmployeeId() == employeeId && req.getStatus() == LeaveStatus.APPROVED && req.getId() != excludeRequestId) {
                if (!end.isBefore(req.getStart()) && !start.isAfter(req.getEnd())) {
                    return req;
                }
            }
        }
        return null;
    }

    public LeaveRequest approveRequest(int requestId) throws IOException {
        LeaveRequest req = findLeaveRequest(requestId);
        if (req == null) {
            throw new IllegalArgumentException("Leave request ID " + requestId + " not found.");
        }

        if (req.getStatus() != LeaveStatus.PENDING) {
            throw new IllegalStateException("Only PENDING requests can be approved. Current status: " + req.getStatus());
        }

        LeaveRequest conflict = checkOverlappingApprovedLeave(req.getEmployeeId(), req.getStart(), req.getEnd(), req.getId());
        if (conflict != null) {
            throw new IllegalStateException("Approval refused: Requested dates (" + req.getStart() + " to " + req.getEnd() +
                    ") overlap with existing APPROVED request #" + conflict.getId() +
                    " (" + conflict.getStart() + " to " + conflict.getEnd() + ").");
        }

        LeaveStatus previousStatus = req.getStatus();
        req.approve();

        try {
            fileManager.saveRequests(requests);
        } catch (IOException e) {
            req.setStatus(previousStatus);
            throw e;
        }

        return req;
    }

    public LeaveRequest rejectRequest(int requestId) throws IOException {
        LeaveRequest req = findLeaveRequest(requestId);
        if (req == null) {
            throw new IllegalArgumentException("Leave request ID " + requestId + " not found.");
        }

        if (req.getStatus() != LeaveStatus.PENDING) {
            throw new IllegalStateException("Only PENDING requests can be rejected. Current status: " + req.getStatus());
        }

        LeaveStatus previousStatus = req.getStatus();
        req.reject();

        try {
            fileManager.saveRequests(requests);
        } catch (IOException e) {
            req.setStatus(previousStatus);
            throw e;
        }

        return req;
    }

    public ArrayList<LeaveRequest> getEmployeeRequests(int employeeId) {
        ArrayList<LeaveRequest> empRequests = new ArrayList<>();
        for (LeaveRequest req : requests) {
            if (req.getEmployeeId() == employeeId) {
                empRequests.add(req);
            }
        }
        return empRequests;
    }

    public int[] calculateStatusCounts() {
        int[] counts = new int[3];
        for (LeaveRequest req : requests) {
            if (req.getStatus() == LeaveStatus.PENDING) {
                counts[0]++;
            } else if (req.getStatus() == LeaveStatus.APPROVED) {
                counts[1]++;
            } else if (req.getStatus() == LeaveStatus.REJECTED) {
                counts[2]++;
            }
        }
        return counts;
    }

    public int calculateApprovedCalendarDays() {
        int totalDays = 0;
        for (LeaveRequest req : requests) {
            if (req.getStatus() == LeaveStatus.APPROVED) {
                totalDays += req.getDays();
            }
        }
        return totalDays;
    }

    public int calculateDistinctEmployeesWithApprovedLeave() {
        HashSet<Integer> distinctEmployees = new HashSet<>();
        for (LeaveRequest req : requests) {
            if (req.getStatus() == LeaveStatus.APPROVED) {
                distinctEmployees.add(req.getEmployeeId());
            }
        }
        return distinctEmployees.size();
    }

    public String generateLeaveReport() {
        int[] counts = calculateStatusCounts();
        int approvedDays = calculateApprovedCalendarDays();
        int distinctApproved = calculateDistinctEmployeesWithApprovedLeave();

        return "Employee Leave Request Report\n\n" +
                "PENDING=" + counts[0] + "\n" +
                "APPROVED=" + counts[1] + "\n" +
                "REJECTED=" + counts[2] + "\n" +
                "APPROVED_DAYS=" + approvedDays + "\n" +
                "DISTINCT_APPROVED_EMPLOYEES=" + distinctApproved;
    }

    public void saveReportToFile() throws IOException {
        String report = generateLeaveReport();
        fileManager.saveReport(report);
    }
}

