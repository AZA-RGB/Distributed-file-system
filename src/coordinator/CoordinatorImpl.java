package coordinator;

import common.CoordinatorInterface;
import common.FileInfo;
import common.NodeInterface;
import common.User;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.mindrot.jbcrypt.BCrypt;


import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class CoordinatorImpl extends UnicastRemoteObject implements CoordinatorInterface {
    private final TreeMap<String, NodeInterface> nodesMap = new TreeMap<>();
    private final ConcurrentHashMap<String, Long> nodeLastHeartbeat = new ConcurrentHashMap<>();//<nodeId,heartbeatTime>

    private final ConcurrentHashMap<String, User> users = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> tokens = new ConcurrentHashMap<>();

    private final ReentrantLock fileLock = new ReentrantLock();

    private final long heartbeatTimeout=1000; // in milliseconds

    private static final String USERS_FILE = "src/coordinator/users.json";

    private final ScheduledExecutorService heartBeatScheduler = Executors.newScheduledThreadPool(1);// thread used to check heartbeats periodically


    private final AtomicInteger addFileNodeIndex = new AtomicInteger(-1);



    protected CoordinatorImpl() throws RemoteException {
        super();
        loadUsersFromJson();
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



    private void loadUsersFromJson() {
        fileLock.lock();
        try {
            if (Files.exists(Paths.get(USERS_FILE))) {
                try (FileReader fileReader = new FileReader(USERS_FILE)) {
                    JSONTokener tokener = new JSONTokener(fileReader);
                    JSONArray jsonArray = new JSONArray(tokener);
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonUser = jsonArray.getJSONObject(i);
                        String username = jsonUser.getString("username");
                        String department = jsonUser.getString("department");
                        String hashedPassword = jsonUser.getString("password");
                        JSONArray jsonPermissions = jsonUser.getJSONArray("permissions");
                        Set<String> permissions = new HashSet<>();
                        for (int j = 0; j < jsonPermissions.length(); j++) {
                            permissions.add(jsonPermissions.getString(j));
                        }
                        users.put(username, new User(username, department, permissions, hashedPassword));
                    }
                    System.out.println("Loaded " + jsonArray.length() + " users from " + USERS_FILE);
                }
            } else {
                System.out.println("No users.json file found, starting with empty users map");
            }
        } catch (IOException e) {
            System.err.println("Error loading users from JSON: " + e.getMessage());
        } finally {
            fileLock.unlock();
        }
    }

    private void saveUsersToJson() {
        fileLock.lock();
        try {
            JSONArray jsonArray = new JSONArray();
            users.forEach((username, user) -> {
                JSONObject jsonUser = new JSONObject();
                jsonUser.put("username", username);
                jsonUser.put("department", user.getDepartment());
                jsonUser.put("password", user.getPassword()); // Store hashed password
                jsonUser.put("permissions", new JSONArray(user.getPermissions()));
                jsonArray.put(jsonUser);
            });
            try (FileWriter fileWriter = new FileWriter(USERS_FILE)) {
                fileWriter.write(jsonArray.toString(2));
                System.out.println("Saved users to " + USERS_FILE);
            }
        } catch (IOException e) {
            System.err.println("Error saving users to JSON: " + e.getMessage());
        } finally {
            fileLock.unlock();
        }
    }

    @Override
    public String login(String username, String password) throws RemoteException {
        System.out.println("Login function active.....");
        User user = users.get(username);
        if (user != null && BCrypt.checkpw(password, user.getPassword())) {
            String token = UUID.randomUUID().toString();
            tokens.put(token, username);
            System.out.println("Login successful for user: " + username);
            return token;
        } else {
            System.out.println("Invalid username or password");
            if (user == null) {
                System.out.println("User not found: " + username);
            } else {
                System.out.println("Password mismatch for user: " + username);
            }
        }
        return null;
    }

    @Override
    public boolean registerUser(String username, String password, String department, Set<String> permissions) throws RemoteException {
        if (users.containsKey(username)) {
            System.out.println("User " + username + " already exists");
            return false;
        }
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        users.put(username, new User(username, department, permissions, hashedPassword));
        saveUsersToJson();
        System.out.println("Added new user successfully: " + username);
        return true;
    }

    public boolean addFile(String token, String name, String department, byte[] content) throws RemoteException {
        User user = validateToken(token);
        if (user == null || !user.hasPermission("add") || !user.getDepartment().equals(department)) {
            return false;
        }
        NodeInterface node = pickNodeToAddFile();
        return node != null && node.addFile(name, department, content);
    }

    @Override
    public boolean deleteFile(String token, String name, String department) throws RemoteException {
        User user = validateToken(token);
        if (user == null || !user.hasPermission("delete") || !user.getDepartment().equals(department)) {
            return false;
        }
        boolean success = false;
        for (NodeInterface node : getAliveNodes()) {
            if (node.deleteFile(name, department)) {
                success = true;
            }
        }
        return success;
    }

    @Override
    public FileInfo getFile(String token, String name, String department) throws RemoteException {
        User user = validateToken(token);
        if (user == null || !user.hasPermission("read")) {
            return null;
        }
        for (NodeInterface node : getAliveNodes()) {
            FileInfo file = node.getFile(name, department);
            if (file != null) {
                return file;
            }
        }
        return null;
    }

    private User validateToken(String token) {
        String username = tokens.get(token);
        return username != null ? users.get(username) : null;
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


    private List<NodeInterface> getAliveNodes() {
        List<NodeInterface> aliveNodes = new ArrayList<>();
        for (Map.Entry<String, NodeInterface> entry : nodesMap.entrySet()) {
            try {
                if (entry.getValue().isAlive()) {
                    aliveNodes.add(entry.getValue());
                }
            } catch (RemoteException e) {
                nodesMap.remove(entry.getKey());
                nodeLastHeartbeat.remove(entry.getKey());
            }
        }
        return aliveNodes;
    }
}