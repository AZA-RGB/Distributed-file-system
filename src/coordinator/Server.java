package coordinator;

import common.CoordinatorInterface;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

public class Server {
    public static void main(String[] args) {
        try {
            CoordinatorInterface c = new CoordinatorImpl();
            LocateRegistry.createRegistry(1099);
            Naming.rebind("rmi://localhost:1099/mainCoordinator", c);
            System.out.println("Server is up and running");
        } catch (RemoteException | MalformedURLException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }
}