package com.employeeleave.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class LeaveRequest {
    private final int id;
    private final int employeeId;
    private final LocalDate start;
    private final LocalDate end;
    private final String type;
    private final String reason;
    private LeaveStatus status;

    public LeaveRequest(int id, int employeeId, LocalDate start, LocalDate end, String type, String reason, LeaveStatus status) {
        if (id <= 0) {
            throw new IllegalArgumentException("Request ID must be positive.");
        }
        if (employeeId <= 0) {
            throw new IllegalArgumentException("Employee ID must be positive.");
        }
        if (start == null) {
            throw new IllegalArgumentException("Start date cannot be null.");
        }
        if (end == null) {
            throw new IllegalArgumentException("End date cannot be null.");
        }
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("End date cannot be before start date.");
        }
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        if (days < 1 || days > 10) {
            throw new IllegalArgumentException("Leave duration must be between 1 and 10 calendar days. Requested: " + days + " days.");
        }
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("Leave type cannot be blank.");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Leave reason cannot be blank.");
        }
        if (status == null) {
            throw new IllegalArgumentException("Leave status cannot be null.");
        }

        this.id = id;
        this.employeeId = employeeId;
        this.start = start;
        this.end = end;
        this.type = type.trim();
        this.reason = reason.trim();
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public LocalDate getStart() {
        return start;
    }

    public LocalDate getEnd() {
        return end;
    }

    public String getType() {
        return type;
    }

    public String getReason() {
        return reason;
    }

    public LeaveStatus getStatus() {
        return status;
    }

    public int getDays() {
        return (int) (ChronoUnit.DAYS.between(start, end) + 1);
    }

    public void approve() {
        if (this.status != LeaveStatus.PENDING) {
            throw new IllegalStateException("Only PENDING requests can be approved. Current status: " + this.status);
        }
        this.status = LeaveStatus.APPROVED;
    }

    public void reject() {
        if (this.status != LeaveStatus.PENDING) {
            throw new IllegalStateException("Only PENDING requests can be rejected. Current status: " + this.status);
        }
        this.status = LeaveStatus.REJECTED;
    }

    public void setStatus(LeaveStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null.");
        }
        this.status = status;
    }

    @Override
    public String toString() {
        return String.format("%-10d | %-11d | %-10s | %-10s | %-4d | %-8s | %-20s | %-8s",
                id, employeeId, start, end, getDays(), type, reason, status);
    }
}
