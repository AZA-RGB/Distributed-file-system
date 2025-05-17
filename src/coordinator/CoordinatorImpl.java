package coordinator;

import common.CoordinatorInterface;
import common.NodeInterface;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class CoordinatorImpl extends UnicastRemoteObject implements CoordinatorInterface {
    private final TreeMap<String, NodeInterface> nodesMap = new TreeMap<>();
    private final ConcurrentHashMap<String, Long> nodeLastHeartbeat = new ConcurrentHashMap<>();//<nodeId,heartbeatTime>
    private final long heartbeatTimeout=1000; // in milliseconds
    private final ScheduledExecutorService heartBeatScheduler = Executors.newScheduledThreadPool(1);// thread used to check heartbeats periodically


    private final AtomicInteger addFileNodeIndex = new AtomicInteger(-1);



    protected CoordinatorImpl() throws RemoteException {
        super();
        heartBeatScheduler.scheduleAtFixedRate(this::checkDeadNodes, 0, 1, TimeUnit.SECONDS);
    }
    private void checkDeadNodes() {// this method is used by heartBeateScheduler thread
        long currentTime = System.currentTimeMillis();
        for (Map.Entry<String, Long> entry : nodeLastHeartbeat.entrySet()) {
            if (currentTime - entry.getValue() > heartbeatTimeout) {
                System.out.println("Node " + entry.getKey() + " is dead");
                nodeLastHeartbeat.remove(entry.getKey());
            }
        }
    }

    public synchronized void receiveHeartbeat(String nodeId) throws RemoteException{// Called by nodes to send heartbeats
        nodeLastHeartbeat.put(nodeId, System.currentTimeMillis());
        System.out.println("received a heartbeat from nodeID :  "+ nodeId);
    }

    @Override
    public void RegisterNode(String nodeId, NodeInterface node) throws RemoteException {
        nodesMap.put(nodeId, node);
        nodeLastHeartbeat.put(nodeId, System.currentTimeMillis());
        System.out.println("Registered node with ID: " + nodeId);
//        printAllNodes();
    }











    @Override
    public void login() throws RemoteException {
        System.out.println("logging in ...");
    }


    @Override
    public NodeInterface addFile(String fileName) throws RemoteException {
        NodeInterface pickedNode=pickNodeToAddFile();
//        registerRequest(pickedNode);
        return pickedNode;
    }











    // helper methods.
    private void registerRequest(NodeInterface pickedNode) {

    }
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