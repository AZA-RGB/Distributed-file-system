package managerClient;

import Node.services.LoginService;
import Node.services.RegisterService;
import common.CoordinatorInterface;

import java.rmi.RemoteException;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

public class Manager {
    private final CoordinatorInterface coordinator;
    private String token; // Store token for permission checks


    public Manager(CoordinatorInterface c) {
        this.coordinator = c;
    }

    public boolean login() throws RemoteException {
        System.out.println("Login Manager");
        LoginService loginService = new LoginService(coordinator);
        token = loginService.execute();
        Set<String> permissions = coordinator.getUserPermissions(token);
        System.out.println(permissions);
        if (!permissions.contains("manage_users")) {
            System.out.println("Error: You do not have permission to manage users (manage_users required)");
            return false;
        }
        if (token != null) {
            System.out.println("Manager: Login successful, token: " + token);
            return true;
        } else {
            System.out.println("Manager: Login failed");
            return false;
        }
    }

    public void registerUser() throws RemoteException {
        // Check if manager has manage_users permission
        Set<String> permissions = coordinator.getUserPermissions(token);
        if (!permissions.contains("manage_users")) {
            System.out.println("Error: You do not have permission to manage users (manage_users required)");
            return;
        }
        RegisterService registerService = new RegisterService(coordinator);
        String newUserToken = registerService.execute();
        if (newUserToken != null) {
            System.out.println("User registered successfully! Token: " + newUserToken);
        } else {
            System.out.println("Registration failed! User may already exist.");
        }
    }
}