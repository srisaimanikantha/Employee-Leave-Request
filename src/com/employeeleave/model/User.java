package com.employeeleave.model;
public abstract class User {
    private final int id;
    private final String name;
    public User(int id, String name) {
        if (id < 0) {
            throw new IllegalArgumentException("User ID cannot be negative.");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("User name cannot be blank.");
        }
        this.id = id;
        this.name = name.trim();
    }
    public int getId() {
        return id;
    }
    public String getName() {
        return name;
    }
    public abstract void showProfile();
}
