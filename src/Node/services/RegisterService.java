package Node.services;

import common.ConsoleColors;
import common.CoordinatorInterface;
import common.User;
import coordinator.CoordinatorImpl;
import org.mindrot.jbcrypt.BCrypt;

import java.rmi.RemoteException;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RegisterService {
    private final CoordinatorInterface coordinator;

    public RegisterService(CoordinatorInterface coordinator) {
        this.coordinator = coordinator;
    }

    public String execute() throws RemoteException {
        ConcurrentHashMap<String, User> users = (coordinator).getUsers();
        Scanner scanner = new Scanner(System.in);

        System.out.print("New username: ");
        String username = scanner.nextLine();
        System.out.print("Email: ");
        String email = scanner.nextLine();
        System.out.print("Password: ");
        String password = scanner.nextLine();
        System.out.print("Department: ");
        String department = scanner.nextLine();

        // Prompt for permission set
        System.out.println("\n" + ConsoleColors.createBorderedMessage(
                "Select permission set", ConsoleColors.CYAN, "", ConsoleColors.BOLD));
        System.out.println(ConsoleColors.format(
                "1. Full Access (add, delete, read, edit)", ConsoleColors.BLUE, "", ""));
        System.out.println(ConsoleColors.format(
                "2. Write Only (add, delete, edit)", ConsoleColors.BLUE, "", ""));
        System.out.println(ConsoleColors.format(
                "3. Read Only (read)", ConsoleColors.BLUE, "", ""));
        System.out.println(ConsoleColors.format(
                "4. Custom (select individual permissions)", ConsoleColors.BLUE, "", ""));
        System.out.print(ConsoleColors.format(
                "Enter choice (1-4): ", ConsoleColors.YELLOW, "", ""));

        Set<String> newUserPermissions = new HashSet<>();
        String choice = scanner.nextLine();

        switch (choice) {
            case "1":
                newUserPermissions.add("add");
                newUserPermissions.add("delete");
                newUserPermissions.add("read");
                newUserPermissions.add("edit");
                System.out.println("Selected Full Access permissions");
                break;
            case "2":
                newUserPermissions.add("add");
                newUserPermissions.add("delete");
                newUserPermissions.add("edit");
                System.out.println("Selected Write Only permissions");
                break;
            case "3":
                newUserPermissions.add("read");
                System.out.println("Selected Read Only permissions");
                break;
            case "4":
                System.out.println("Select individual permissions (enter 'done' to finish):");
                while (true) {
                    System.out.print("Enter permission (add/delete/read) or 'done': ");
                    String perm = scanner.nextLine().toLowerCase();
                    if (perm.equals("done")) {
                        break;
                    }
                    if (perm.equals("add") ||perm.equals("edit")|| perm.equals("delete") || perm.equals("read")) {
                        newUserPermissions.add(perm);
                        System.out.println("Added permission: " + perm);
                    } else {
                        System.out.println("Invalid permission, try again");
                    }
                }
                if (newUserPermissions.isEmpty()) {
                    System.out.println("No permissions selected, defaulting to read-only");
                    newUserPermissions.add("read");
                }
                break;
            default:
                System.out.println("Invalid choice, defaulting to read-only");
                newUserPermissions.add("read");
                break;
        }
        if (users.containsKey(email)) {
            System.out.println("User with email " + email + " already exists");
            return null;
        }
        // Check if username is unique
        if (users.values().stream().anyMatch(u -> u.getUsername().equals(username))) {
            System.out.println("User with username " + username + " already exists");
            return null;
        }
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        User newUser = new User(username, email, department, newUserPermissions, hashedPassword);
        coordinator.addUser(email, newUser);
        String token = UUID.randomUUID().toString();
        (coordinator).addToken(token, email);
        (coordinator).saveUsersToJson();
        System.out.println("Added new user successfully: " + email);
        return token;
    }
}