package com.employeeleave.model;

public class Manager extends User {

    public Manager() {
        super(0, "Office Manager");
    }

    public Manager(int id, String name) {
        super(id, name);
    }

    @Override
    public void showProfile() {
        System.out.println("Manager ID   : " + getId());
        System.out.println("Manager Name : " + getName());
        System.out.println("Role         : Office Manager / System Administrator");
    }
}
