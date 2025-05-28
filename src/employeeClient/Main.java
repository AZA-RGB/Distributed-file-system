package employeeClient;

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
            CoordinatorInterface coordinator = (CoordinatorInterface) Naming.lookup("rmi://localhost:1099/mainCoordinator");
            coordinator.doSomething();

//            Employee e = new Employee(coordinator);
//
//            if (!e.login()) {
//                System.out.println("Login failed!");
//                return;
//            }
//
//            // واجهة تفاعلية
//            while (true) {
//                System.out.println("\n" + ConsoleColors.createBorderedMessage(
//                        "Options", ConsoleColors.CYAN, "", ConsoleColors.BOLD));
//                System.out.println(ConsoleColors.format(
//                        "1. Add File", ConsoleColors.BLUE, "", ""));
//                System.out.println(ConsoleColors.format(
//                        "2. Delete File", ConsoleColors.BLUE, "", ""));
//                System.out.println(ConsoleColors.format(
//                        "3. Get File", ConsoleColors.BLUE, "", ""));
//                System.out.println(ConsoleColors.format(
//                        "4. Edit File", ConsoleColors.BLUE, "", ""));
//                System.out.println(ConsoleColors.format(
//                        "5. Exit", ConsoleColors.BLUE, "", ""));
//                System.out.print(ConsoleColors.format(
//                        "Choose an option: ", ConsoleColors.YELLOW, "", ""));
//                int choice = scanner.nextInt();
//                scanner.nextLine();
//
//                switch (choice) {
//                    case 1:
//                        e.addFile();
//                        break;
//                    case 2:
//                        e.deleteFile();
//                        break;
//                    case 3:
//                        e.getFile();
//                        break;
//                    case 4:
//                        e.editFile();
//                        break;
//                    case 5:
//                        System.out.println("Exiting...");
//                        return;
//                    default:
//                        System.out.println("Invalid option!");
//                }
//            }
//
        } catch (MalformedURLException | NotBoundException | RemoteException ex) {
            System.err.println("Client error: " + ex.getMessage());
        }
    }
}

