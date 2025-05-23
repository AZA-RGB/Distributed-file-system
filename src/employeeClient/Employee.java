package employeeClient;

import Node.services.AddFileService;
import Node.services.DeleteFileService;
import Node.services.GetFileService;
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

    public boolean login() throws RemoteException {
        try {
            System.out.println("Login Manager");
            LoginService loginService = new LoginService(coordinator);
            token = loginService.execute();
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

    public void addFile() throws RemoteException {
        AddFileService addFileService = new AddFileService(coordinator);
        if (addFileService.execute(token)) {
            System.out.println("File added successfully!");
        } else {
            System.out.println("Failed to add file!");
        }
    }

    public void deleteFile() throws RemoteException {
        DeleteFileService deleteFileService = new DeleteFileService(coordinator);
        if (deleteFileService.execute(token)) {
            System.out.println("File deleted successfully!");
        } else {
            System.out.println("Failed to delete file!");
        }
    }

    public void getFile() throws RemoteException {
        GetFileService getFileService = new GetFileService(coordinator);
        FileInfo file = getFileService.execute(token);
        if (file != null) {
            System.out.println("File: " + file + ", Content: " + new String(file.getContent()));
        } else {
            System.out.println("File not found!");
        }
    }
}