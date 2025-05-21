package common;

import java.io.Serializable;
import java.util.Set;

public class User implements Serializable {
    private final String username;
    private final String email;
    private final String department;
    private final Set<String> permissions;
    private final String password; // Stores hashed password

    public User(String username, String email, String department, Set<String> permissions, String password) {
        this.username = username;
        this.email = email;
        this.department = department;
        this.permissions = permissions;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getDepartment() {
        return department;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public String getPassword() {
        return password;
    }

    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }

    @Override
    public String toString() {
        return "User{username='" + username + "', email='" + email + "', department='" + department + "', permissions=" + permissions + "}";
    }
}