package employeeClient;

import Node.services.LoginService;
import common.CoordinatorInterface;
import common.FileInfo;

import java.rmi.RemoteException;
import java.util.Scanner;

public class Employee {
    private final CoordinatorInterface coordinator;
    private String token;
    private static final Scanner scanner = new Scanner(System.in);

    public Employee(CoordinatorInterface c) throws RemoteException {
        this.coordinator = c;
    }

    public boolean login(String email, String password) throws RemoteException {
        System.out.println("Employee: Calling login for user: " + email + ", password: " + password);
        try {
            LoginService loginService = new LoginService(coordinator);
            token = loginService.execute(email, password);
            System.out.println("Employee: Login returned token: " + (token != null ? token : "null"));
        } catch (RemoteException e) {
            System.err.println("Employee: RMI error during login: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
        if (token == null) {
            System.out.println("Employee: Login failed (returned null)");
        } else {
            System.out.println("Employee: Login successful");
        }
        return token != null;
    }

    public void addFile(String name, String department, byte[] content) throws RemoteException {
        if (coordinator.addFile(token, name, department, content)) {
            System.out.println("File added successfully!");
        } else {
            System.out.println("Failed to add file!");
        }
    }

    public void deleteFile(String name, String department) throws RemoteException {
        if (coordinator.deleteFile(token, name, department)) {
            System.out.println("File deleted successfully!");
        } else {
            System.out.println("Failed to delete file!");
        }
    }

    public void getFile(String name, String department) throws RemoteException {
        FileInfo file = coordinator.getFile(token, name, department);
        if (file != null) {
            System.out.println("File: " + file + ", Content: " + new String(file.getContent()));
        } else {
            System.out.println("File not found!");
        }
    }
}