package com.employeeleave.model;
public enum LeaveStatus {
    PENDING,
    APPROVED,
    REJECTED;

    public static LeaveStatus fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Leave status cannot be blank.");
        }
        try {
            return LeaveStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown leave status: '" + value + "'. Expected PENDING, APPROVED, or REJECTED.");
        }
    }
}
