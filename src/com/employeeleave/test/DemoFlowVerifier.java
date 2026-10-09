package com.employeeleave.test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import com.employeeleave.Util.FileManager;
import com.employeeleave.model.Employee;
import com.employeeleave.model.LeaveRequest;
import com.employeeleave.model.LeaveStatus;
import com.employeeleave.service.LeaveService;

public class DemoFlowVerifier {
    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("   VERIFYING SECTION 29 FINAL DEMONSTRATION FLOW  ");
        System.out.println("==================================================");

        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("demo_flow_test_");
            FileManager fm = new FileManager(tempDir);
            LeaveService service = new LeaveService(fm);

            System.out.println("STEP 1: Application started.");

            System.out.println("STEP 2 & 3: Manager registers Ponnana Sai (IT)...");
            Employee emp = service.addEmployee("Ponnana Sai", "IT");
            System.out.println("Registered Employee ID: " + emp.getId() + " (" + emp.getName() + ", " + emp.getDepartment() + ")");
            if (emp.getId() != 1) throw new AssertionError("Expected ID 1 for first employee");

            System.out.println("STEP 4 & 5: Employee submits leave 2026-10-20 to 2026-10-22 (Personal)...");
            LocalDate s1 = LocalDate.of(2026, 10, 20);
            LocalDate e1 = LocalDate.of(2026, 10, 22);
            LeaveRequest req1 = service.submitLeaveRequest(emp.getId(), s1, e1, "Personal", "Family function");
            System.out.println("Submitted: Request #" + req1.getId() + ", Days: " + req1.getDays() + ", Status: " + req1.getStatus());
            if (req1.getDays() != 3) throw new AssertionError("Expected 3 days");
            if (req1.getStatus() != LeaveStatus.PENDING) throw new AssertionError("Expected PENDING");

            System.out.println("STEP 6 & 7: Manager approves request #" + req1.getId() + "...");
            LeaveRequest approved1 = service.approveRequest(req1.getId());
            System.out.println("Request #" + approved1.getId() + " status is now: " + approved1.getStatus());
            if (approved1.getStatus() != LeaveStatus.APPROVED) throw new AssertionError("Expected APPROVED");

            System.out.println("STEP 8: Employee submits 2026-10-22 to 2026-10-24...");
            LocalDate s2 = LocalDate.of(2026, 10, 22);
            LocalDate e2 = LocalDate.of(2026, 10, 24);
            LeaveRequest req2 = service.submitLeaveRequest(emp.getId(), s2, e2, "Personal", "Extended function");
            System.out.println("Submitted: Request #" + req2.getId() + ", Days: " + req2.getDays() + ", Status: " + req2.getStatus());
            if (req2.getStatus() != LeaveStatus.PENDING) throw new AssertionError("Expected PENDING");

            System.out.println("STEP 9: Manager attempts approval of request #" + req2.getId() + "...");
            try {
                service.approveRequest(req2.getId());
                throw new AssertionError("Approval should have been refused due to overlap!");
            } catch (IllegalStateException e) {
                System.out.println("Approval successfully refused: " + e.getMessage());
            }

            System.out.println("STEP 10: Submit 2026-10-25 to 2026-10-27 and approve it...");
            LocalDate s3 = LocalDate.of(2026, 10, 25);
            LocalDate e3 = LocalDate.of(2026, 10, 27);
            LeaveRequest req3 = service.submitLeaveRequest(emp.getId(), s3, e3, "Personal", "Another function");
            LeaveRequest approved3 = service.approveRequest(req3.getId());
            System.out.println("Request #" + approved3.getId() + " approved: " + approved3.getStatus());
            if (approved3.getStatus() != LeaveStatus.APPROVED) throw new AssertionError("Expected APPROVED");

            System.out.println("STEP 11: View reports...");
            int[] counts = service.calculateStatusCounts();
            int approvedDays = service.calculateApprovedCalendarDays();
            int distinctEmployees = service.calculateDistinctEmployeesWithApprovedLeave();

            System.out.println("PENDING                     : " + counts[0]);
            System.out.println("APPROVED                    : " + counts[1]);
            System.out.println("REJECTED                    : " + counts[2]);
            System.out.println("APPROVED CALENDAR DAYS      : " + approvedDays);
            System.out.println("DISTINCT APPROVED EMPLOYEES : " + distinctEmployees);

            if (counts[0] != 1) throw new AssertionError("Expected PENDING count 1");
            if (counts[1] != 2) throw new AssertionError("Expected APPROVED count 2");
            if (counts[2] != 0) throw new AssertionError("Expected REJECTED count 0");
            if (approvedDays != 6) throw new AssertionError("Expected approved days 6");
            if (distinctEmployees != 1) throw new AssertionError("Expected distinct employees 1");

            System.out.println("STEP 12 & 13: Restart application and verify persistence...");
            LeaveService reloadedService = new LeaveService(fm);
            List<Employee> emps = fm.loadEmployees();
            List<LeaveRequest> reqs = fm.loadRequests(emps);
            reloadedService.initializeData(emps, reqs);

            if (reloadedService.getEmployees().size() != 1) throw new AssertionError("Employee count mismatch");
            if (reloadedService.getRequests().size() != 3) throw new AssertionError("Request count mismatch");
            if (reloadedService.findLeaveRequest(1).getStatus() != LeaveStatus.APPROVED) throw new AssertionError("Req 1 not approved");
            if (reloadedService.findLeaveRequest(2).getStatus() != LeaveStatus.PENDING) throw new AssertionError("Req 2 not pending");
            if (reloadedService.findLeaveRequest(3).getStatus() != LeaveStatus.APPROVED) throw new AssertionError("Req 3 not approved");

            System.out.println("\nSUCCESS: All steps of demonstration flow passed with 100% precision!");
        } catch (Exception e) {
            System.err.println("Demonstration flow failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } finally {
            if (tempDir != null) {
                try {
                    Files.walk(tempDir)
                            .sorted(Comparator.reverseOrder())
                            .forEach(p -> {
                                try { Files.deleteIfExists(p); } catch (Exception ignored) {}
                            });
                } catch (Exception ignored) {}
            }
        }
    }
}

