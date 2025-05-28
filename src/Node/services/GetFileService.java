package Node.services;

import common.CoordinatorInterface;
import common.FileInfo;

import java.rmi.RemoteException;
import java.util.Scanner;

public class GetFileService {
    private final CoordinatorInterface coordinator;
    private String currentFileName;
    private String currentDepartment;

    public GetFileService(CoordinatorInterface coordinator) {
        this.coordinator = coordinator;
    }

    public FileInfo execute(String token) throws RemoteException {
        Scanner scanner = new Scanner(System.in);
        System.out.print("File name: ");
        String name = scanner.nextLine();
        System.out.print("Department: ");
        String dept = scanner.nextLine();
        currentFileName = name;
        currentDepartment = dept;
        FileInfo file = coordinator.getFile(token, name, dept);
        if (file != null) {
            System.out.println("File: " + file + ", Content: " + new String(file.getContent()));
        return file;
        } else {
            System.out.println("File not found!");
            return  null;
        }
    }

    public String getCurrentFileName() {
        return currentFileName;
    }

    public String getCurrentDepartment() {
        return currentDepartment;
    }
}