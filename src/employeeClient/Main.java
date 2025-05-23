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
            Scanner sc = new Scanner(System.in);
            System.out.println("Enter email:");
            String email = sc.nextLine();
            System.out.println("Enter password:");
            String password = sc.nextLine();
            System.out.println("Attempting login for: " + email);
            if (!e.login(email, password)) {
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
                        System.out.print("File name: ");
                        String name = scanner.nextLine();
                        System.out.print("Department: ");
                        String dept = scanner.nextLine();
                        System.out.print("Content: ");
                        String content = scanner.nextLine();
                        e.addFile(name, dept, content.getBytes());
                        break;
                    case 2:
                        System.out.print("File name: ");
                        name = scanner.nextLine();
                        System.out.print("Department: ");
                        dept = scanner.nextLine();
                        e.deleteFile(name, dept);
                        break;
                    case 3:
                        System.out.print("File name: ");
                        name = scanner.nextLine();
                        System.out.print("Department: ");
                        dept = scanner.nextLine();
                        e.getFile(name, dept);
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

