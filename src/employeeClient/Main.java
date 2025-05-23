package employeeClient;

import common.CoordinatorInterface;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Scanner;

public class Main {
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        try {
            CoordinatorInterface coordinator = (CoordinatorInterface) Naming.lookup("rmi://localhost:1099/mainCoordinator");
            Employee e = new Employee(coordinator);

            if (!e.login()) {
                System.out.println("Login failed!");
                return;
            }

            // واجهة تفاعلية
            while (true) {
                System.out.println("\nOptions:");
                System.out.println("1. Add File");
                System.out.println("2. Delete File");
                System.out.println("3. Get File");
                System.out.println("4. Exit");
                System.out.print("Choose an option: ");
                int choice = scanner.nextInt();
                scanner.nextLine();

                switch (choice) {
                    case 1:
                        e.addFile();
                        break;
                    case 2:
                        e.deleteFile();
                        break;
                    case 3:
                        e.getFile();
                        break;
                    case 4:
                        System.out.println("Exiting...");
                        return;
                    default:
                        System.out.println("Invalid option!");
                }
            }
        } catch (MalformedURLException | NotBoundException | RemoteException ex) {
            System.err.println("Client error: " + ex.getMessage());
        }
    }
}

