package common;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface NodeInterface extends Remote {
    void addFile(String s) throws RemoteException;
}
