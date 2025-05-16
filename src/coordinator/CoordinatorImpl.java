package coordinator;

import common.CoordinatorInterface;
import common.NodeInterface;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

public class CoordinatorImpl extends UnicastRemoteObject implements CoordinatorInterface {
    private final TreeMap<String, NodeInterface> nodesMap = new TreeMap<>();
    private final AtomicInteger addFileNodeIndex = new AtomicInteger(-1);




    protected CoordinatorImpl() throws RemoteException {
        super();

    }


    @Override
    public void RegisterNode(String nodeId, NodeInterface node) throws RemoteException {
        nodesMap.put(nodeId, node);
        System.out.println("Registered node with ID: " + nodeId);
//        printAllNodes();
    }

    @Override
    public void login() throws RemoteException {
        System.out.println("logging in ...");
    }

    @Override
    public NodeInterface addFile(String fileName) throws RemoteException {
        return pickNodeToAddFile();
    }

    // helper methods
    private NodeInterface pickNodeToAddFile() throws RemoteException {
        if (nodesMap.isEmpty()) {
            throw new RemoteException("No nodes available to add file");
        }

        // Convert keys to array for round-robin selection
        String[] nodeKeys = nodesMap.keySet().toArray(new String[0]);

        // Get next node in round-robin fashion
        int nextIndex = addFileNodeIndex.incrementAndGet() % nodeKeys.length;
        if (nextIndex < 0) { // Handle potential negative values
            nextIndex += nodeKeys.length;
        }

        String selectedNodeKey = nodeKeys[nextIndex];
        return nodesMap.get(selectedNodeKey);
    }

    public void printAllNodes() {
        System.out.println("Registered nodes (sorted by key):");
        nodesMap.forEach((key, node) ->
                System.out.println("Node ID: " + key + " -> " + node));
    }
}