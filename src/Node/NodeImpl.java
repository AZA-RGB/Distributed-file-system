package Node;

import Node.services.DoSomethingService;
import common.CoordinatorInterface;
import common.FileInfo;
import common.NodeInterface;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class NodeImpl extends UnicastRemoteObject implements NodeInterface {
    String NodeId,StoragePath;
    CoordinatorInterface coordinator;

    private NodeImpl(String id, String storagePath,CoordinatorInterface c) throws RemoteException {
        super();
        this.NodeId=id;
        this.StoragePath=storagePath;
        this.coordinator=c;
        ScheduledExecutorService heartBeatScheduler = Executors.newScheduledThreadPool(1);
        heartBeatScheduler.scheduleAtFixedRate(() -> {
            try {
                this.coordinator.receiveHeartbeat(this.NodeId);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        },0, 500, TimeUnit.MILLISECONDS);
    }


    public static NodeImpl getInstance(String id, CoordinatorInterface c) throws RemoteException {
        //load servers id list
        //check if node already exist, if not create storage folder for the new node then create node object
        String StoragePath=findStoragePath(id);
        if(null!=StoragePath){//old node (recovery situation)
            return new NodeImpl(id,StoragePath,c);
        }
        System.out.println("This is a new Node ");
        System.out.println("creating new Storage folder....");
        StoragePath="NodeStorage-"+id;
        File directory = new File(StoragePath);
        if (directory.mkdir()) {
            System.out.println("Directory created successfully");
            //  save new node to nodeslist.json
            addToNodesList(id,StoragePath);
            return new NodeImpl(id,"NodeStorage-"+id,c);
        } else {
            System.err.println("Failed to create directory (it may already exist)");
            return null;
        }
    }


    private static boolean addToNodesList(String nodeId, String storagePath) {
        try {
            // Define the path to the NodesList.json file
            String filePath = "src/Node/NodesList.json";

            // Read the existing content of the file or create a new JSON array if the file doesn't exist
            JSONArray nodesArray;
            if (Files.exists(Paths.get(filePath))) {
                String content = new String(Files.readAllBytes(Paths.get(filePath)));
                nodesArray = new JSONArray(content);
            } else {
                nodesArray = new JSONArray();
            }

            // Create a new node object
            JSONObject newNode = new JSONObject();
            newNode.put("node_id", nodeId);
            newNode.put("storagePath", storagePath);

            // Add the new node to the array
            nodesArray.put(newNode);

            // Write the updated array back to the file
            Files.write(Paths.get(filePath), nodesArray.toString(2).getBytes());
            System.out.println("added node to json list");
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    private static String findStoragePath( String targetNodeId) {
        try (FileReader fileReader = new FileReader("src/Node/NodesList.json")) {
            JSONTokener tokener = new JSONTokener(fileReader);
            JSONArray jsonArray = new JSONArray(tokener);

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject node = jsonArray.getJSONObject(i);
            String currentId = node.getString("node_id");
            System.out.println(jsonArray);
            if (Objects.equals(currentId, targetNodeId)) {
                return node.getString("storagePath");
            }
        }
        return null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    @Override
    public boolean addFile(String name, String department, byte[] content) throws RemoteException {
        return false;
    }

    @Override
    public boolean deleteFile(String name, String department) throws RemoteException {
        return false;
    }

    @Override
    public FileInfo getFile(String name, String department) throws RemoteException {
        return null;
    }

    @Override
    public void doSomething() throws RemoteException {
        new DoSomethingService().execute();
    }

    @Override
    public boolean isAlive() throws RemoteException {
        return false;
    }



    public String getNodeId() {
        return NodeId;
    }
}
