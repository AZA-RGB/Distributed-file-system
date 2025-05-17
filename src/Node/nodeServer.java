package Node;

import common.CoordinatorInterface;
import common.NodeInterface;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Scanner;

public class nodeServer {



    public static void main(String[] args) throws MalformedURLException, NotBoundException, RemoteException {
        CoordinatorInterface c = (CoordinatorInterface) Naming.lookup("rmi://localhost:1099/mainCoordinator");
        Scanner sc=new Scanner(System.in);
        System.out.println("Enter node id");
        String id=sc.next();
        NodeImpl node= NodeImpl.getInstance(id,c);
        if(node!=null){
            c.RegisterNode(node.NodeId, node);
        }else {
            System.err.println("couldn't create node ");
        }
    }
}
