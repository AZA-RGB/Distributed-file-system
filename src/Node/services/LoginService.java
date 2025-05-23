package Node.services;

import common.CoordinatorInterface;
import common.User;
import coordinator.CoordinatorImpl;
import org.mindrot.jbcrypt.BCrypt;

import java.rmi.RemoteException;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LoginService {
    private final CoordinatorInterface coordinator;

    public LoginService(CoordinatorInterface coordinator) {
        this.coordinator = coordinator;
    }

    public String execute() throws RemoteException {
        ConcurrentHashMap<String, User> users = (coordinator).getUsers();
        Scanner sc = new Scanner(System.in);
        System.out.println("Enter email:");
        String email = sc.nextLine();
        System.out.println("Enter password:");
        String password = sc.nextLine();
        System.out.println("Attempting login for: " + email);
        User user = users.get(email);
        if (user != null && BCrypt.checkpw(password, user.getPassword())) {
            String token = UUID.randomUUID().toString();
            (coordinator).addToken(token, email);
            System.out.println("Login successful for user: " + email);
            return token;
        } else {
            System.out.println("Invalid email or password");
            if (user == null) {
                System.out.println("User not found: " + email);
            } else {
                System.out.println("Password mismatch for user: " + email);
            }
            return null;
        }
    }
}