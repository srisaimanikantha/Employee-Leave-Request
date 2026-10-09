package com.employeeleave.Util;

import com.employeeleave.model.Employee;
import com.employeeleave.model.LeaveRequest;
import com.employeeleave.model.LeaveStatus;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FileManager {
    private static final Path DEFAULT_DATA_DIR = Paths.get("data");
    private static final String EMPLOYEES_FILE_NAME = "employees.txt";
    private static final String REQUESTS_FILE_NAME = "requests.txt";
    private static final String REPORT_FILE_NAME = "report.txt";

    public static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);

    public static final String[] VALID_LEAVE_TYPES = {"Personal", "Medical", "Other"};

    private final Path dataDir;
    private final Path employeesFile;
    private final Path requestsFile;
    private final Path reportFile;

    public FileManager() {
        this(DEFAULT_DATA_DIR);
    }

    public FileManager(Path dataDir) {
        this.dataDir = dataDir;
        this.employeesFile = dataDir.resolve(EMPLOYEES_FILE_NAME);
        this.requestsFile = dataDir.resolve(REQUESTS_FILE_NAME);
        this.reportFile = dataDir.resolve(REPORT_FILE_NAME);
    }

    public Path getDataDir() {
        return dataDir;
    }

    public static class DataLoadException extends Exception {
        private final String fileName;
        private final int lineNumber;
        private final String reason;

        public DataLoadException(String fileName, int lineNumber, String reason) {
            super("DATA LOAD ERROR in " + fileName + (lineNumber > 0 ? " (line " + lineNumber + ")" : "") + ": " + reason);
            this.fileName = fileName;
            this.lineNumber = lineNumber;
            this.reason = reason;
        }

        public String getFileName() {
            return fileName;
        }

        public int getLineNumber() {
            return lineNumber;
        }

        public String getReason() {
            return reason;
        }
    }

    public void ensureDataDirectory() throws IOException {
        if (!Files.exists(dataDir)) {
            Files.createDirectories(dataDir);
        }
    }

    public List<Employee> loadEmployees() throws DataLoadException, IOException {
        ensureDataDirectory();
        List<Employee> list = new ArrayList<>();
        if (!Files.exists(employeesFile)) {
            return list;
        }

        Set<Integer> seenIds = new HashSet<>();
        try (BufferedReader reader = Files.newBufferedReader(employeesFile, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }

                String[] parts = line.split("\\|", -1);
                if (parts.length != 3) {
                    throw new DataLoadException(EMPLOYEES_FILE_NAME, lineNumber,
                            "Expected 3 pipe-separated fields (id|name|department), found " + parts.length + ".");
                }

                String idStr = parts[0].trim();
                String name = parts[1].trim();
                String department = parts[2].trim();

                int id;
                try {
                    id = Integer.parseInt(idStr);
                } catch (NumberFormatException e) {
                    throw new DataLoadException(EMPLOYEES_FILE_NAME, lineNumber,
                            "Invalid numeric employee ID: '" + idStr + "'.");
                }

                if (id <= 0) {
                    throw new DataLoadException(EMPLOYEES_FILE_NAME, lineNumber,
                            "Employee ID must be a positive integer (> 0). Found: " + id + ".");
                }

                if (seenIds.contains(id)) {
                    throw new DataLoadException(EMPLOYEES_FILE_NAME, lineNumber,
                            "Duplicate employee ID detected: " + id + ".");
                }

                if (name.isEmpty()) {
                    throw new DataLoadException(EMPLOYEES_FILE_NAME, lineNumber,
                            "Employee name cannot be blank.");
                }

                if (department.isEmpty()) {
                    throw new DataLoadException(EMPLOYEES_FILE_NAME, lineNumber,
                            "Employee department cannot be blank.");
                }

                seenIds.add(id);
                list.add(new Employee(id, name, department));
            }
        }

        return list;
    }

    public List<LeaveRequest> loadRequests(List<Employee> employees) throws DataLoadException, IOException {
        ensureDataDirectory();
        List<LeaveRequest> list = new ArrayList<>();
        if (!Files.exists(requestsFile)) {
            return list;
        }

        Set<Integer> validEmployeeIds = new HashSet<>();
        for (Employee emp : employees) {
            validEmployeeIds.add(emp.getId());
        }

        Set<Integer> seenRequestIds = new HashSet<>();
        try (BufferedReader reader = Files.newBufferedReader(requestsFile, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }

                String[] parts = line.split("\\|", -1);
                if (parts.length != 7) {
                    throw new DataLoadException(REQUESTS_FILE_NAME, lineNumber,
                            "Expected 7 pipe-separated fields (id|employeeId|start|end|type|reason|status), found " + parts.length + ".");
                }

                String idStr = parts[0].trim();
                String empIdStr = parts[1].trim();
                String startStr = parts[2].trim();
                String endStr = parts[3].trim();
                String type = parts[4].trim();
                String reason = parts[5].trim();
                String statusStr = parts[6].trim();

                int id;
                try {
                    id = Integer.parseInt(idStr);
                } catch (NumberFormatException e) {
                    throw new DataLoadException(REQUESTS_FILE_NAME, lineNumber,
                            "Invalid numeric request ID: '" + idStr + "'.");
                }

                if (id <= 0) {
                    throw new DataLoadException(REQUESTS_FILE_NAME, lineNumber,
                            "Request ID must be positive (> 0). Found: " + id + ".");
                }

                if (seenRequestIds.contains(id)) {
                    throw new DataLoadException(REQUESTS_FILE_NAME, lineNumber,
                            "Duplicate request ID detected: " + id + ".");
                }

                int empId;
                try {
                    empId = Integer.parseInt(empIdStr);
                } catch (NumberFormatException e) {
                    throw new DataLoadException(REQUESTS_FILE_NAME, lineNumber,
                            "Invalid numeric employee ID: '" + empIdStr + "'.");
                }

                if (!validEmployeeIds.contains(empId)) {
                    throw new DataLoadException(REQUESTS_FILE_NAME, lineNumber,
                            "Employee ID " + empId + " does not exist in employee records.");
                }

                LocalDate start;
                try {
                    start = LocalDate.parse(startStr, DATE_FORMATTER);
                } catch (DateTimeParseException e) {
                    throw new DataLoadException(REQUESTS_FILE_NAME, lineNumber,
                            "Invalid date format or value for start date: '" + startStr + "'. Expected yyyy-MM-dd.");
                }

                LocalDate end;
                try {
                    end = LocalDate.parse(endStr, DATE_FORMATTER);
                } catch (DateTimeParseException e) {
                    throw new DataLoadException(REQUESTS_FILE_NAME, lineNumber,
                            "Invalid date format or value for end date: '" + endStr + "'. Expected yyyy-MM-dd.");
                }

                if (end.isBefore(start)) {
                    throw new DataLoadException(REQUESTS_FILE_NAME, lineNumber,
                            "End date (" + end + ") cannot be before start date (" + start + ").");
                }

                long days = java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;
                if (days < 1 || days > 10) {
                    throw new DataLoadException(REQUESTS_FILE_NAME, lineNumber,
                            "Leave duration must be between 1 and 10 calendar days. Found: " + days + " days.");
                }

                if (!isValidLeaveType(type)) {
                    throw new DataLoadException(REQUESTS_FILE_NAME, lineNumber,
                            "Invalid leave type: '" + type + "'. Allowed: Personal, Medical, Other.");
                }

                if (reason.isEmpty()) {
                    throw new DataLoadException(REQUESTS_FILE_NAME, lineNumber,
                            "Leave reason cannot be blank.");
                }

                LeaveStatus status;
                try {
                    status = LeaveStatus.fromString(statusStr);
                } catch (IllegalArgumentException e) {
                    throw new DataLoadException(REQUESTS_FILE_NAME, lineNumber,
                            "Invalid leave status: '" + statusStr + "'. Allowed: PENDING, APPROVED, REJECTED.");
                }

                LeaveRequest request = new LeaveRequest(id, empId, start, end, normalizeLeaveType(type), reason, status);

                if (status == LeaveStatus.APPROVED) {
                    for (LeaveRequest existing : list) {
                        if (existing.getStatus() == LeaveStatus.APPROVED && existing.getEmployeeId() == empId) {
                            if (!request.getEnd().isBefore(existing.getStart()) && !request.getStart().isAfter(existing.getEnd())) {
                                throw new DataLoadException(REQUESTS_FILE_NAME, lineNumber,
                                        "Overlapping approved leave requests found for Employee ID " + empId +
                                                ": Request #" + request.getId() + " (" + request.getStart() + " to " + request.getEnd() +
                                                ") overlaps with Request #" + existing.getId() + " (" + existing.getStart() + " to " + existing.getEnd() + ").");
                            }
                        }
                    }
                }

                seenRequestIds.add(id);
                list.add(request);
            }
        }

        return list;
    }

    public static boolean isValidLeaveType(String type) {
        if (type == null) return false;
        String t = type.trim();
        for (String valid : VALID_LEAVE_TYPES) {
            if (valid.equalsIgnoreCase(t)) {
                return true;
            }
        }
        return false;
    }

    public static String normalizeLeaveType(String type) {
        if (type == null) return "";
        String t = type.trim();
        for (String valid : VALID_LEAVE_TYPES) {
            if (valid.equalsIgnoreCase(t)) {
                return valid;
            }
        }
        return t;
    }

    public void saveEmployees(List<Employee> employees) throws IOException {
        ensureDataDirectory();
        Path tempFile = Files.createTempFile(dataDir, "employees_", ".tmp");

        try {
            try (BufferedWriter writer = Files.newBufferedWriter(tempFile, StandardCharsets.UTF_8)) {
                for (Employee emp : employees) {
                    writer.write(emp.getId() + "|" + emp.getName() + "|" + emp.getDepartment());
                    writer.newLine();
                }
            }
            safeReplaceFile(tempFile, employeesFile);
        } catch (IOException e) {
            Files.deleteIfExists(tempFile);
            throw e;
        }
    }

    public void saveRequests(List<LeaveRequest> requests) throws IOException {
        ensureDataDirectory();
        Path tempFile = Files.createTempFile(dataDir, "requests_", ".tmp");

        try {
            try (BufferedWriter writer = Files.newBufferedWriter(tempFile, StandardCharsets.UTF_8)) {
                for (LeaveRequest req : requests) {
                    writer.write(req.getId() + "|" +
                            req.getEmployeeId() + "|" +
                            req.getStart() + "|" +
                            req.getEnd() + "|" +
                            req.getType() + "|" +
                            req.getReason() + "|" +
                            req.getStatus().name());
                    writer.newLine();
                }
            }
            safeReplaceFile(tempFile, requestsFile);
        } catch (IOException e) {
            Files.deleteIfExists(tempFile);
            throw e;
        }
    }

    public void saveReport(String reportContent) throws IOException {
        ensureDataDirectory();
        Path tempFile = Files.createTempFile(dataDir, "report_", ".tmp");

        try {
            try (BufferedWriter writer = Files.newBufferedWriter(tempFile, StandardCharsets.UTF_8)) {
                writer.write(reportContent);
                writer.newLine();
            }
            safeReplaceFile(tempFile, reportFile);
        } catch (IOException e) {
            Files.deleteIfExists(tempFile);
            throw e;
        }
    }

    private void safeReplaceFile(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException | UnsupportedOperationException e) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
