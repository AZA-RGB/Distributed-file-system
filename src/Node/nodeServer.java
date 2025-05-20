package Node;

import common.CoordinatorInterface;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Scanner;

public class nodeServer {
    public static void main(String[] args) {
        try {
            CoordinatorInterface c = (CoordinatorInterface) Naming.lookup("rmi://localhost:1099/mainCoordinator");
            Scanner sc = new Scanner(System.in);
            System.out.println("Enter node id");
            String id = sc.next();
            NodeImpl node = NodeImpl.getInstance(id, c);
            if (node != null) {
                Naming.rebind("rmi://localhost:1099/node" + id, node);
                c.RegisterNode(node.getNodeId(), node);
                System.out.println("Node " + id + " registered successfully");
            } else {
                System.err.println("Couldn't create node");
            }
        } catch (MalformedURLException | NotBoundException | RemoteException e) {
            System.err.println("Node server error: " + e.getMessage());
        }
    }
}