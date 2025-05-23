package Node.services;

import common.CoordinatorInterface;

import java.rmi.RemoteException;
import java.util.Scanner;

public class EditFileService {
    private final CoordinatorInterface coordinator;

    public EditFileService(CoordinatorInterface coordinator) {
        this.coordinator = coordinator;
    }

    public boolean execute(String token) throws RemoteException {
        Scanner scanner = new Scanner(System.in);
        System.out.print("File name: ");
        String name = scanner.nextLine();
        String dept =  coordinator.getDepartment(token);
        System.out.print("Content: ");
        String content = scanner.nextLine();
        if (coordinator.editFile(token, name, dept, content.getBytes())) {
            System.out.println("File edited successfully!");
            return true;
        } else {
            System.out.println("Failed to edit file!");
            return  false;
        }
    }
}