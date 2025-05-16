package common;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface CoordinatorInterface extends Remote {
    void RegisterNode(String nodeId, NodeInterface node) throws RemoteException;
    void login() throws RemoteException;
    NodeInterface addFile(String fileName) throws RemoteException;


}
