package Node.services;

import common.CoordinatorInterface;

import java.rmi.RemoteException;
import java.util.Scanner;

public class DeleteFileService {
    private final CoordinatorInterface coordinator;
    private String currentFileName;
    private String currentDepartment;

    public DeleteFileService(CoordinatorInterface coordinator) {
        this.coordinator = coordinator;
    }

    public boolean execute(String token) throws RemoteException {
        Scanner scanner = new Scanner(System.in);
        System.out.print("File name: ");
        String name = scanner.nextLine();
        String dept =  coordinator.getDepartment(token);
        if (dept == null) {
            System.out.println("Error: Department not found for this user!");
            return false;
        }
        currentFileName = name;
        currentDepartment = dept;
        if (coordinator.deleteFile(token, name, dept)) {
            System.out.println("File deleted successfully!");
            return true;
        } else {
            System.out.println("Failed to delete file!");
            return false;
        }
    }

    public String getCurrentFileName() {
        return currentFileName;
    }

    public String getCurrentDepartment() {
        return currentDepartment;
    }
}