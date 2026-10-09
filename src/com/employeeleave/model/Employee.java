package com.employeeleave.model;

public class Employee extends User {
    private final String department;

    public Employee(int id, String name, String department) {
        super(id, name);
        if (department == null || department.trim().isEmpty()) {
            throw new IllegalArgumentException("Department cannot be blank.");
        }
        this.department = department.trim();
    }

    public String getDepartment() {
        return department;
    }

    @Override
    public void showProfile() {
        System.out.println("Employee ID : " + getId());
        System.out.println("Name        : " + getName());
        System.out.println("Department  : " + department);
    }

    @Override
    public String toString() {
        return String.format("%-5d %-22s %-15s", getId(), getName(), department);
    }
}
