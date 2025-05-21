package Node.services;

import common.CoordinatorInterface;
import common.User;
import coordinator.CoordinatorImpl;
import org.mindrot.jbcrypt.BCrypt;

import java.rmi.RemoteException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RegisterService {
    private final CoordinatorInterface coordinator;

    public RegisterService(CoordinatorInterface coordinator) {
        this.coordinator = coordinator;
    }

    public String execute(String username, String email, String password, String department, Set<String> permissions) throws RemoteException {
        ConcurrentHashMap<String, User> users = (coordinator).getUsers();
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
        User newUser = new User(username, email, department, permissions, hashedPassword);
        coordinator.addUser(email, newUser);
        String token = UUID.randomUUID().toString();
        (coordinator).addToken(token, email);
        (coordinator).saveUsersToJson();
        System.out.println("Added new user successfully: " + email);
        return token;
    }
}