package employeeClient;

import common.CoordinatorInterface;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public class Main {
    public static void main(String[] args) throws MalformedURLException, NotBoundException, RemoteException {
        CoordinatorInterface c=(CoordinatorInterface) Naming.lookup("rmi://localhost:1099/mainCoordinator");
        Employee e=new Employee(c);
        System.out.println("client started");

        e.addFile("hello file");

    }
}
