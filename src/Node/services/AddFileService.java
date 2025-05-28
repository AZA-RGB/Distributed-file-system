package Node.services;

import common.CoordinatorInterface;

import java.rmi.RemoteException;
import java.util.Scanner;

public class AddFileService {
    private final CoordinatorInterface coordinator;

    public AddFileService(CoordinatorInterface coordinator) {
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
        System.out.print("Content: ");
        String content = scanner.nextLine();
        if (coordinator.addFile(token, name, dept, content.getBytes())) {
            System.out.println("File added successfully!");
            return true;
        } else {
            System.out.println("Failed to add file!");
            return  false;
        }
    }
}