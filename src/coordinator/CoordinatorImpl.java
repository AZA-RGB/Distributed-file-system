package coordinator;

import common.CoordinatorInterface;
import common.FileInfo;
import common.NodeInterface;
import common.User;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

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
    private final ConcurrentHashMap<String, NodeInterface> nodesMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> nodeLastHeartbeat = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<AtomicInteger>> pendingRequests = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, User> users = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> tokens = new ConcurrentHashMap<>();
    private final long heartbeatTimeout = 1000;
    private final ScheduledExecutorService heartBeatScheduler = Executors.newScheduledThreadPool(1);
    private final AtomicInteger addFileNodeIndex = new AtomicInteger(-1);
    private final AtomicInteger RequestId = new AtomicInteger(-1);
    private final ReentrantLock fileLock = new ReentrantLock();
    private static final String USERS_FILE = "src/coordinator/users.json";

    protected CoordinatorImpl() throws RemoteException {
        super();
        loadUsersFromJson();
        heartBeatScheduler.scheduleAtFixedRate(this::checkDeadNodes, 0, 1, TimeUnit.SECONDS);
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
                        String email = jsonUser.getString("email");
                        String department = jsonUser.getString("department");
                        String hashedPassword = jsonUser.getString("password");
                        JSONArray jsonPermissions = jsonUser.getJSONArray("permissions");
                        Set<String> permissions = new HashSet<>();
                        for (int j = 0; j < jsonPermissions.length(); j++) {
                            permissions.add(jsonPermissions.getString(j));
                        }
                        users.put(email, new User(username, email, department, permissions, hashedPassword));
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

    @Override
    public void saveUsersToJson() {
        fileLock.lock();
        try {
            JSONArray jsonArray = new JSONArray();
            users.forEach((email, user) -> {
                JSONObject jsonUser = new JSONObject();
                jsonUser.put("username", user.getUsername());
                jsonUser.put("email", email);
                jsonUser.put("department", user.getDepartment());
                jsonUser.put("password", user.getPassword());
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
    public void addUser(String email, User newUser){
        users.put(email, newUser);
    }

    @Override
    public ConcurrentHashMap<String, User> getUsers() {
        return users;
    }
    public ConcurrentHashMap<String, String> getTokens() {
        return tokens;
    }

    private void checkDeadNodes() {
        long currentTime = System.currentTimeMillis();
        nodeLastHeartbeat.forEachEntry(1, entry -> {
            if (currentTime - entry.getValue() > heartbeatTimeout) {
                System.out.println("Node " + entry.getKey() + " is dead");
                nodesMap.remove(entry.getKey());
                nodeLastHeartbeat.remove(entry.getKey());
            }
        });
    }

    @Override
    public void receiveHeartbeat(String nodeId) throws RemoteException {
        nodeLastHeartbeat.put(nodeId, System.currentTimeMillis());
//        System.out.println("Received heartbeat from nodeID: " + nodeId);
    }

    @Override
    public void RegisterNode(String nodeId, NodeInterface node) throws RemoteException {
        nodesMap.put(nodeId, node);
        nodeLastHeartbeat.put(nodeId, System.currentTimeMillis());
        System.out.println("Registered node with ID: " + nodeId);
    }



    @Override
    public boolean addFile(String token, String name, String department, byte[] content) throws RemoteException {
        User user = validateToken(token);
        if (user == null || !user.hasPermission("add") || !user.getDepartment().equals(department)) {
            return false;
        }
        String nodeID = pickNodeToAddFile();

        return nodeID != null && nodesMap.get(nodeID).addFile(name, department, content);
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
    @Override
     public void addToken(String token,String email) {
        tokens.put(token, email);
    }

    @Override
    public Set<String> getUserPermissions(String token) throws RemoteException {
        User user = validateToken(token);
        if (user == null) {
            System.out.println("Invalid token for permission check");
            return new HashSet<>();
        }

        System.out.println(user.getPermissions());
        return user.getPermissions();
    }



    private String pickNodeToAddFile() throws RemoteException {
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
        System.out.println("selected node is "+selectedNodeKey);
        return selectedNodeKey;
    }




    public void doSomething() throws  RemoteException{
        String nodeID=pickNodeToAddFile();
        nodesMap.get(nodeID).doSomething();
        //add it to a map of order,nodeId
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