package common;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface NodeInterface extends Remote {
    boolean addFile(String name, String department, byte[] content) throws RemoteException;
    boolean deleteFile(String name, String department) throws RemoteException;
    FileInfo getFile(String name, String department) throws RemoteException;
    boolean isAlive() throws RemoteException;

    void doSomething() throws RemoteException, InterruptedException;
}
