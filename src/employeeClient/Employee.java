package employeeClient;

import common.CoordinatorInterface;
import common.NodeInterface;

import java.rmi.RemoteException;

public class Employee {
    private final CoordinatorInterface coordinator;

    public Employee(CoordinatorInterface c){
        this.coordinator=c;
    }
    void  login() throws RemoteException {
        coordinator.login();
    }


    // test method
    void addFile(String fileName) throws RemoteException {
        // request  coordinator to add a file, coordinator return a remote node reference
        NodeInterface node=coordinator.addFile(fileName);
        // call node.addFile(byte[] file,)
        node.addFile(fileName);
    }

}
