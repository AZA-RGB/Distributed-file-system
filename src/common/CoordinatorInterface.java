package common;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Set;

public interface CoordinatorInterface extends Remote {
    void RegisterNode(String nodeId, NodeInterface node) throws RemoteException;
    String login(String username, String password) throws RemoteException;

    boolean registerUser(String username, String password, String department, Set<String> permissions) throws RemoteException;

    boolean addFile(String token, String name, String department, byte[] content) throws RemoteException;

    boolean deleteFile(String token, String name, String department) throws RemoteException;

    FileInfo getFile(String token, String name, String department) throws RemoteException;
    void receiveHeartbeat(String nodeId) throws RemoteException;
}
