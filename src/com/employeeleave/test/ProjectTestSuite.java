package com.employeeleave.test;
import com.employeeleave.model.Employee;
import com.employeeleave.model.LeaveRequest;
import com.employeeleave.model.LeaveStatus;
import com.employeeleave.service.LeaveService;
import com.employeeleave.Util.FileManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

public class ProjectTestSuite {
    private static int passedCount = 0;
    private static int failedCount = 0;

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("   EMPLOYEE LEAVE REQUEST SYSTEM - TEST SUITE     ");
        System.out.println("==================================================");

        Path testDir = null;
        try {
            testDir = Files.createTempDirectory("leave_system_test_");
            runAllTests(testDir);
        } catch (Exception e) {
            System.err.println("Unexpected test failure: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (testDir != null) {
                cleanupDir(testDir);
            }
        }

        System.out.println("\n==================================================");
        System.out.println("TEST SUMMARY: " + passedCount + " PASSED, " + failedCount + " FAILED");
        System.out.println("==================================================");

        if (failedCount > 0) {
            System.exit(1);
        }
    }

    private static void runAllTests(Path testDir) throws Exception {
        FileManager fm = new FileManager(testDir);
        LeaveService service = new LeaveService(fm);

        test("TEST 1: Register employee", () -> {
            Employee emp = service.addEmployee("Alice Smith", "Engineering");
            assertEqual(emp.getId(), 1, "Employee ID should be 1");
            assertEqual(emp.getName(), "Alice Smith", "Name should match");
            assertEqual(emp.getDepartment(), "Engineering", "Department should match");
            assertEqual(service.getEmployees().size(), 1, "Employee count should be 1");
        });

        test("TEST 2: Restart application", () -> {
            LeaveService restartedService = new LeaveService(fm);
            List<Employee> loadedEmps = fm.loadEmployees();
            List<LeaveRequest> loadedReqs = fm.loadRequests(loadedEmps);
            restartedService.initializeData(loadedEmps, loadedReqs);

            assertEqual(restartedService.getEmployees().size(), 1, "Employees count should be 1 after restart");
            Employee reloaded = restartedService.findEmployee(1);
            assertNotNull(reloaded, "Employee 1 must exist after restart");
            assertEqual(reloaded.getName(), "Alice Smith", "Employee name should persist");
        });

        test("TEST 3: Submit valid 3-day leave", () -> {
            LocalDate start = LocalDate.of(2026, 10, 12);
            LocalDate end = LocalDate.of(2026, 10, 14);
            LeaveRequest req = service.submitLeaveRequest(1, start, end, "Personal", "Family visit");

            assertEqual(req.getId(), 1, "Request ID should be 1");
            assertEqual(req.getDays(), 3, "Inclusive duration should be 3 days");
            assertEqual(req.getStatus(), LeaveStatus.PENDING, "Initial status must be PENDING");
        });

        test("TEST 4: Submit 1-day leave", () -> {
            LocalDate day = LocalDate.of(2026, 11, 1);
            LeaveRequest req = service.submitLeaveRequest(1, day, day, "Medical", "Dental checkup");
            assertEqual(req.getDays(), 1, "Inclusive duration should be 1 day");
            assertEqual(req.getStatus(), LeaveStatus.PENDING, "Status should be PENDING");
        });

        test("TEST 5: Submit 10-day leave", () -> {
            LocalDate start = LocalDate.of(2026, 12, 1);
            LocalDate end = LocalDate.of(2026, 12, 10);
            LeaveRequest req = service.submitLeaveRequest(1, start, end, "Other", "Conference");
            assertEqual(req.getDays(), 10, "Inclusive duration should be 10 days");
            assertEqual(req.getStatus(), LeaveStatus.PENDING, "Status should be PENDING");
        });

        test("TEST 6: Submit 11-day leave", () -> {
            LocalDate start = LocalDate.of(2026, 12, 1);
            LocalDate end = LocalDate.of(2026, 12, 11);
            assertThrows(IllegalArgumentException.class, () ->
                    service.submitLeaveRequest(1, start, end, "Personal", "Long vacation"));
        });

        test("TEST 7: End date before start date", () -> {
            LocalDate start = LocalDate.of(2026, 10, 15);
            LocalDate end = LocalDate.of(2026, 10, 10);
            assertThrows(IllegalArgumentException.class, () ->
                    service.submitLeaveRequest(1, start, end, "Personal", "Time travel"));
        });

        test("TEST 8: Invalid date format/value", () -> {
            assertThrows(Exception.class, () ->
                    LocalDate.parse("2026-15-50", FileManager.DATE_FORMATTER));
            assertThrows(Exception.class, () ->
                    LocalDate.parse("2026-02-30", FileManager.DATE_FORMATTER));
        });

        test("TEST 9: Blank employee name", () -> {
            assertThrows(IllegalArgumentException.class, () ->
                    service.addEmployee("   ", "Finance"));
        });

        test("TEST 10: Blank department", () -> {
            assertThrows(IllegalArgumentException.class, () ->
                    service.addEmployee("Bob", "   "));
        });

        test("TEST 11: Blank reason", () -> {
            LocalDate start = LocalDate.of(2026, 10, 20);
            LocalDate end = LocalDate.of(2026, 10, 22);
            assertThrows(IllegalArgumentException.class, () ->
                    service.submitLeaveRequest(1, start, end, "Personal", "  "));
        });

        test("TEST 12: Invalid leave type", () -> {
            LocalDate start = LocalDate.of(2026, 10, 20);
            LocalDate end = LocalDate.of(2026, 10, 22);
            assertThrows(IllegalArgumentException.class, () ->
                    service.submitLeaveRequest(1, start, end, "Vacation", "Holiday"));
        });

        test("TEST 13: Approve PENDING request", () -> {
            LeaveRequest approved = service.approveRequest(1);
            assertEqual(approved.getStatus(), LeaveStatus.APPROVED, "Status should be APPROVED");
        });

        test("TEST 14: Approve same request again", () -> {
            assertThrows(IllegalStateException.class, () -> service.approveRequest(1));
        });

        test("TEST 15: Reject PENDING request", () -> {
            LeaveRequest rejected = service.rejectRequest(2);
            assertEqual(rejected.getStatus(), LeaveStatus.REJECTED, "Status should be REJECTED");
        });

        test("TEST 16: Reject already APPROVED request", () -> {
            assertThrows(IllegalStateException.class, () -> service.rejectRequest(1));
        });

        test("TEST 17: Approve overlapping request", () -> {
            LocalDate start = LocalDate.of(2026, 10, 14);
            LocalDate end = LocalDate.of(2026, 10, 16);
            LeaveRequest overlappingReq = service.submitLeaveRequest(1, start, end, "Personal", "Overlap test");

            assertThrows(IllegalStateException.class, () -> service.approveRequest(overlappingReq.getId()));
            assertEqual(overlappingReq.getStatus(), LeaveStatus.PENDING, "Request remains PENDING after refused approval");
        });

        test("TEST 18: Approve non-overlapping request", () -> {
            LocalDate start = LocalDate.of(2026, 10, 15);
            LocalDate end = LocalDate.of(2026, 10, 17);
            LeaveRequest nonOverlapping = service.submitLeaveRequest(1, start, end, "Medical", "Post checkup");
            LeaveRequest approved = service.approveRequest(nonOverlapping.getId());
            assertEqual(approved.getStatus(), LeaveStatus.APPROVED, "Should approve successfully");
        });

        test("TEST 19: Two different employees have same dates", () -> {
            Employee emp2 = service.addEmployee("Bob Marley", "Finance");
            LocalDate start = LocalDate.of(2026, 10, 12);
            LocalDate end = LocalDate.of(2026, 10, 14);
            LeaveRequest emp2Req = service.submitLeaveRequest(emp2.getId(), start, end, "Personal", "Family trip");
            LeaveRequest approved = service.approveRequest(emp2Req.getId());
            assertEqual(approved.getStatus(), LeaveStatus.APPROVED, "Emp 2 can have same dates as Emp 1");
        });

        test("TEST 20: Pending request overlaps approved request", () -> {
            LocalDate start = LocalDate.of(2026, 10, 13);
            LocalDate end = LocalDate.of(2026, 10, 14);
            LeaveRequest pendingReq = service.submitLeaveRequest(1, start, end, "Other", "Pending overlap");
            assertEqual(pendingReq.getStatus(), LeaveStatus.PENDING, "Pending request can exist");
            assertThrows(IllegalStateException.class, () -> service.approveRequest(pendingReq.getId()));
        });

        test("TEST 21: Rejected request overlaps approved request", () -> {
            LocalDate day = LocalDate.of(2026, 11, 1);
            LeaveRequest newReq = service.submitLeaveRequest(1, day, day, "Personal", "Reclaim rejected date");
            LeaveRequest approved = service.approveRequest(newReq.getId());
            assertEqual(approved.getStatus(), LeaveStatus.APPROVED, "Approval succeeds over rejected dates");
        });

        test("TEST 22: Restart after approval", () -> {
            LeaveService reloaded = new LeaveService(fm);
            List<Employee> emps = fm.loadEmployees();
            List<LeaveRequest> reqs = fm.loadRequests(emps);
            reloaded.initializeData(emps, reqs);

            LeaveRequest req1 = reloaded.findLeaveRequest(1);
            assertNotNull(req1, "Request 1 must exist");
            assertEqual(req1.getStatus(), LeaveStatus.APPROVED, "Status APPROVED must persist");
        });

        test("TEST 23: Restart after rejection", () -> {
            LeaveService reloaded = new LeaveService(fm);
            List<Employee> emps = fm.loadEmployees();
            List<LeaveRequest> reqs = fm.loadRequests(emps);
            reloaded.initializeData(emps, reqs);

            LeaveRequest req2 = reloaded.findLeaveRequest(2);
            assertNotNull(req2, "Request 2 must exist");
            assertEqual(req2.getStatus(), LeaveStatus.REJECTED, "Status REJECTED must persist");
        });

        test("TEST 24: Delete/missing employees.txt", () -> {
            Path emptyDir = Files.createTempDirectory("leave_system_empty_");
            try {
                FileManager emptyFm = new FileManager(emptyDir);
                List<Employee> emps = emptyFm.loadEmployees();
                assertEqual(emps.size(), 0, "Missing employees.txt results in 0 employees without crashing");
            } finally {
                cleanupDir(emptyDir);
            }
        });

        test("TEST 25: Corrupt requests.txt", () -> {
            Path corruptDir = Files.createTempDirectory("leave_system_corrupt_");
            try {
                FileManager corruptFm = new FileManager(corruptDir);
                corruptFm.ensureDataDirectory();
                Files.writeString(corruptDir.resolve("employees.txt"), "1|John Doe|HR\n");
                Files.writeString(corruptDir.resolve("requests.txt"),
                        "1|1|2026-15-50|2026-10-14|Personal|Vacation|PENDING\n");

                List<Employee> emps = corruptFm.loadEmployees();
                assertThrows(FileManager.DataLoadException.class, () -> corruptFm.loadRequests(emps));
            } finally {
                cleanupDir(corruptDir);
            }
        });
    }

    @FunctionalInterface
    interface TestCase {
        void run() throws Exception;
    }

    private static void test(String name, TestCase tc) {
        try {
            tc.run();
            System.out.println("[PASS] " + name);
            passedCount++;
        } catch (Throwable t) {
            System.out.println("[FAIL] " + name + " -> " + t.getMessage());
            failedCount++;
        }
    }

    private static void assertEqual(Object actual, Object expected, String msg) {
        if (actual == null && expected == null) return;
        if (actual == null || !actual.equals(expected)) {
            throw new AssertionError(msg + " (Expected: " + expected + ", Actual: " + actual + ")");
        }
    }

    private static void assertNotNull(Object obj, String msg) {
        if (obj == null) {
            throw new AssertionError(msg + " (Object was null)");
        }
    }

    private static void assertThrows(Class<? extends Throwable> expectedType, TestCase tc) {
        try {
            tc.run();
            throw new AssertionError("Expected exception " + expectedType.getSimpleName() + " was NOT thrown.");
        } catch (Throwable t) {
            if (expectedType.isInstance(t)) {
                return;
            }
            throw new AssertionError("Expected " + expectedType.getSimpleName() + " but caught " + t.getClass().getSimpleName() + ": " + t.getMessage(), t);
        }
    }

    private static void cleanupDir(Path dir) {
        try {
            if (Files.exists(dir)) {
                Files.walk(dir)
                        .sorted(Comparator.reverseOrder())
                        .forEach(p -> {
                            try {
                                Files.deleteIfExists(p);
                            } catch (IOException ignored) {}
                        });
            }
        } catch (IOException ignored) {}
    }
}
