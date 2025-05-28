package common;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface NodeInterface extends Remote {
    boolean addFile(String name, String department, byte[] content) throws RemoteException;
    boolean deleteFile(String name, String department) throws RemoteException;
    FileInfo getFile(String name, String department) throws RemoteException;

    boolean editFile(String name, String department, byte[] content) throws RemoteException;

    boolean isAlive() throws RemoteException;

    void doSomething() throws RemoteException, InterruptedException;

    void lockFileForRead(String name, String department) throws RemoteException;
    void unlockFileForRead(String name, String department) throws RemoteException;
    void lockFileForWrite(String name, String department) throws RemoteException;
    void unlockFileForWrite(String name, String department) throws RemoteException;
    void unlockFileForWriteManually(String name, String department) throws RemoteException;
    void unlockFileForReadManually(String name, String department) throws RemoteException;
    boolean isWriteLocked(String name, String department) throws RemoteException;

}
