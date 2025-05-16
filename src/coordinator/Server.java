package coordinator;

import java.net.MalformedURLException;
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

public class Server {
    public static void main(String[] args) throws RemoteException, MalformedURLException, AlreadyBoundException {
        CoordinatorImpl c=new CoordinatorImpl();
        LocateRegistry.createRegistry(1099);
        Naming.bind("mainCoordinator",c);
        System.out.println("server is up and running");

    }
}
