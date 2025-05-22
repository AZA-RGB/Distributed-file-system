package managerClient;

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
            CoordinatorInterface c = (CoordinatorInterface) Naming.lookup("rmi://localhost:1099/mainCoordinator");
            Manager m = new Manager(c);

            if (!m.login()) {
                System.out.println("Login failed!");
                return;
            }

            // واجهة تفاعلية
            while (true) {
                System.out.println("\nOptions:");
                System.out.println("1. Register User");
                System.out.println("2. Exit");
                System.out.print("Choose an option: ");
                int choice = scanner.nextInt();
                scanner.nextLine();

                switch (choice) {
                    case 1:
                        m.registerUser();
                        break;
                    case 2:
                        System.out.println("Exiting...");
                        return;
                    default:
                        System.out.println("Invalid option!");
                }
            }
        } catch (MalformedURLException | NotBoundException | RemoteException e) {
            System.err.println("Manager client error: " + e.getMessage());
        }
    }
}