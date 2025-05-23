package managerClient;

import common.ConsoleColors;
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
                System.out.println("\n" + ConsoleColors.createBorderedMessage(
                        "Options", ConsoleColors.CYAN, "", ConsoleColors.BOLD));
                System.out.println(ConsoleColors.format(
                        "1. Register User", ConsoleColors.BLUE, "", ""));
                System.out.println(ConsoleColors.format(
                        "2. Exit", ConsoleColors.BLUE, "", ""));
                System.out.print(ConsoleColors.format(
                        "Choose an option: ", ConsoleColors.YELLOW, "", ""));
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