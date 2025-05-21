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
    private static final Scanner scanner = new Scanner(System.in);

    public Manager(CoordinatorInterface c) {
        this.coordinator = c;
    }

    public boolean login(String email, String password) throws RemoteException {
        System.out.println("Manager: Attempting login for email: " + email);
        LoginService loginService = new LoginService(coordinator);
        token = loginService.execute(email, password);
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

        System.out.print("New username: ");
        String username = scanner.nextLine();
        System.out.print("Email: ");
        String email = scanner.nextLine();
        System.out.print("Password: ");
        String password = scanner.nextLine();
        System.out.print("Department: ");
        String department = scanner.nextLine();

        // Prompt for permission set
        System.out.println("Select permission set:");
        System.out.println("1. Full Access (add, delete, read)");
        System.out.println("2. Write Only (add, delete)");
        System.out.println("3. Read Only (read)");
        System.out.println("4. Custom (select individual permissions)");
        System.out.print("Enter choice (1-4): ");

        Set<String> newUserPermissions = new HashSet<>();
        String choice = scanner.nextLine();

        switch (choice) {
            case "1":
                newUserPermissions.add("add");
                newUserPermissions.add("delete");
                newUserPermissions.add("read");
                System.out.println("Selected Full Access permissions");
                break;
            case "2":
                newUserPermissions.add("add");
                newUserPermissions.add("delete");
                System.out.println("Selected Write Only permissions");
                break;
            case "3":
                newUserPermissions.add("read");
                System.out.println("Selected Read Only permissions");
                break;
            case "4":
                System.out.println("Select individual permissions (enter 'done' to finish):");
                while (true) {
                    System.out.print("Enter permission (add/delete/read) or 'done': ");
                    String perm = scanner.nextLine().toLowerCase();
                    if (perm.equals("done")) {
                        break;
                    }
                    if (perm.equals("add") || perm.equals("delete") || perm.equals("read")) {
                        newUserPermissions.add(perm);
                        System.out.println("Added permission: " + perm);
                    } else {
                        System.out.println("Invalid permission, try again");
                    }
                }
                if (newUserPermissions.isEmpty()) {
                    System.out.println("No permissions selected, defaulting to read-only");
                    newUserPermissions.add("read");
                }
                break;
            default:
                System.out.println("Invalid choice, defaulting to read-only");
                newUserPermissions.add("read");
                break;
        }

        RegisterService registerService = new RegisterService(coordinator);
        String newUserToken = registerService.execute(username, email, password, department, newUserPermissions);
        if (newUserToken != null) {
            System.out.println("User registered successfully! Token: " + newUserToken);
        } else {
            System.out.println("Registration failed! User may already exist.");
        }
    }
}