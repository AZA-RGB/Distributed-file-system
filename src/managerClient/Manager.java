package managerClient;

import common.CoordinatorInterface;

import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Scanner;

public class Manager {
    private final CoordinatorInterface coordinator;
    private static final Scanner scanner = new Scanner(System.in);

    public Manager(CoordinatorInterface c) {
        this.coordinator = c;
    }

    public boolean login(String username, String password) throws RemoteException {
        return coordinator.login(username, password) != null;
    }

    public void registerUser() throws RemoteException {
        System.out.print("New username: ");
        String username = scanner.nextLine();
        System.out.print("Password: ");
        String password = scanner.nextLine();
        System.out.print("Department: ");
        String department = scanner.nextLine();
        if (coordinator.registerUser(username, password, department,
                new HashSet<>(Arrays.asList("add", "delete", "read")))) {
            System.out.println("User registered successfully!");
        } else {
            System.out.println("User already exists!");
        }
    }
}