package com.example.bestmlewi.ui.employee;

public enum EmployeeRole {
    COLLABORATOR("collaborator"),
    DELIVER("deliver"),
    COORDINATOR("coordinator");

    private final String role;

    EmployeeRole(String role) {
        this.role = role;
    }

    public String getRole() {
        return role;
    }

    public static EmployeeRole fromString(String role) {
        for (EmployeeRole employeeRole : EmployeeRole.values()) {
            if (employeeRole.role.equalsIgnoreCase(role)) {
                return employeeRole;
            }
        }
        return COLLABORATOR; // Valeur par défaut
    }
}