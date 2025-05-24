package common;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public interface CoordinatorInterface extends Remote {
    void receiveHeartbeat(String nodeId) throws RemoteException;
    void RegisterNode(String nodeId, NodeInterface node) throws RemoteException;
    boolean addFile(String token, String name, String department, byte[] content) throws RemoteException;
    boolean deleteFile(String token, String name, String department) throws RemoteException;
    FileInfo getFile(String token, String name, String department) throws RemoteException;
    boolean editFile(String token, String name, String department, byte[] content) throws RemoteException;

    Set<String> getUserPermissions(String token) throws RemoteException;
    ConcurrentHashMap<String, User> getUsers() throws RemoteException;
    void saveUsersToJson() throws RemoteException;
    void addToken(String token,String email) throws RemoteException;
    void addUser(String email, User newUser) throws RemoteException;
    String getDepartment(String token) throws  RemoteException;
    void doSomething() throws RemoteException;
    void deLoad(String nodeId) throws RemoteException;
}