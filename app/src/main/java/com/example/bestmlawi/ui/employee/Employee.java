package com.example.bestmlewi.ui.employee;

import java.util.Date;

public class Employee {
    private String id;
    private String name;
    private String email;
    private String phoneNumber;
    private String role;
    private Date hiredDate;
    private String point_of_sale_id;
    private String location;

    public Employee() {
        // Constructeur vide nécessaire pour Firestore
    }

    public Employee(String id, String name, String email, String phoneNumber,
                    String role, Date hiredDate, String point_of_sale_id, String location) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.role = role;
        this.hiredDate = hiredDate;
        this.point_of_sale_id = point_of_sale_id;
        this.location = location;
    }

    // Getters et Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Date getHiredDate() { return hiredDate; }
    public void setHiredDate(Date hiredDate) { this.hiredDate = hiredDate; }

    public String getPointOfSaleId() { return point_of_sale_id; }
    public void setPointOfSaleId(String point_of_sale_id) { this.point_of_sale_id = point_of_sale_id; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    @Override
    public String toString() {
        return name + " - " + role + (location != null ? " (" + location + ")" : "");
    }
}