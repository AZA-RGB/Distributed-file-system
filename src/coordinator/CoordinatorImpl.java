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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class CoordinatorImpl extends UnicastRemoteObject implements CoordinatorInterface {
    private final ConcurrentHashMap<String, NodeInterface> nodesMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> nodeLastHeartbeat = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, User> users = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> tokens = new ConcurrentHashMap<>();
    private final long heartbeatTimeout = 1000;
    private final ScheduledExecutorService heartBeatScheduler = Executors.newScheduledThreadPool(1);
    private final AtomicInteger addFileNodeIndex = new AtomicInteger(-1);
    private final ReentrantLock fileLock = new ReentrantLock();
    private static final String USERS_FILE = "src/coordinator/users.json";
    private final ConcurrentHashMap<String, ReentrantReadWriteLock> fileLocks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> lockedFiles = new ConcurrentHashMap<>();    // Map to track lock status (department:name -> token)

    // MODIFIED: Added lockTimeouts to handle automatic unlocking after timeout
    private final ConcurrentHashMap<String, Long> lockTimeouts = new ConcurrentHashMap<>();
    private final ScheduledExecutorService timeoutScheduler = Executors.newScheduledThreadPool(1);
    private static final long LOCK_TIMEOUT_SECONDS = 300; // 5 minutes

    protected CoordinatorImpl() throws RemoteException {
        super();
        loadUsersFromJson();
        heartBeatScheduler.scheduleAtFixedRate(this::checkDeadNodes, 0, 100, TimeUnit.MILLISECONDS);
        // Added timeout scheduler to check for expired locks
        timeoutScheduler.scheduleAtFixedRate(this::checkLockTimeouts, 0, 60, TimeUnit.SECONDS);
    }

    // Added method to check and unlock expired locks
    private void checkLockTimeouts() {
        long currentTime = System.currentTimeMillis();
        lockTimeouts.forEachEntry(1, entry -> {
            if (currentTime - entry.getValue() > LOCK_TIMEOUT_SECONDS * 1000) {
                String lockKey = entry.getKey();
                String token = lockedFiles.get(lockKey);
                if (token != null) {
                    String[] parts = lockKey.split(":");
                    String department = parts[0];
                    String name = parts[1];
                    try {
                        System.out.println("Timeout expired for file " + name + " in department " + department + ", unlocking automatically...");
                        if (lockKey.startsWith(department + ":" + name)) {
                            unlockFileForWriteManually(token, name, department);
                        } else {
                            unlockFileForReadManually(token, name, department);
                        }
                    } catch (RemoteException e) {
                        System.err.println("Error unlocking file " + name + " in department " + department + ": " + e.getMessage());
                    }
                }
            }
        });
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
//            System.out.println(pendingRequests.get( entry.getKey()));

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
        String lockKey = department + ":" + name;
        if (lockedFiles.containsKey(lockKey)) {
            System.out.println("File " + name + " in department " + department + " is locked by another user!");
            return false;
        }
        lockedFiles.put(lockKey, token);
        lockTimeouts.put(lockKey, System.currentTimeMillis());


        String nodeID = pickNodeToAddFile();
        if (nodeID != null) {
            ReentrantReadWriteLock lock = getFileLock(department, name);
            lock.writeLock().lock();
            try {
                boolean success = nodesMap.get(nodeID).addFile(name, department, content);
                if (success) {
                    for (NodeInterface node : getAliveNodes()) {
                        FileInfo file = node.getFile(name, department);
                        if (file != null && node.isWriteLocked(name, department)) {
                            node.unlockFileForWrite(name, department);
                        }
                    }
                    lockedFiles.remove(lockKey);
                    lockTimeouts.remove(lockKey);

                }
                return success;
            } finally {
                lock.writeLock().unlock();
            }
        }
        return false;

    }

    @Override
    public boolean deleteFile(String token, String name, String department) throws RemoteException {
        User user = validateToken(token);
        if (user == null || !user.hasPermission("delete") || !user.getDepartment().equals(department)) {
            return false;
        }
        String lockKey = department + ":" + name;
        if (lockedFiles.containsKey(lockKey)) {
            System.out.println("DEBUG: File " + name + " in department " + department + " is locked by token " + lockedFiles.get(lockKey));
            return false;
        }
        lockedFiles.put(lockKey, token);
        lockTimeouts.put(lockKey, System.currentTimeMillis());


        ReentrantReadWriteLock lock = getFileLock(department, name);
        lock.writeLock().lock();
        try {
            for (NodeInterface node : getAliveNodes()) {
                FileInfo file = node.getFile(name, department);
                if (file != null) {
                    node.lockFileForWrite(name, department);
                }
            }
            // Delete on all nodes
            boolean success = false;
            for (NodeInterface node : getAliveNodes()) {
                if (node.deleteFile(name, department)) {
                    success = true;
                }
            }
            return success;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public FileInfo getFile(String token, String name, String department) throws RemoteException {
        User user = validateToken(token);
        if (user == null || !user.hasPermission("read")) {
            return null;
        }
        String lockKey = department + ":" + name;
        if (lockedFiles.containsKey(lockKey)) {
            System.out.println("DEBUG: File " + name + " in department " + department + " is locked by token " + lockedFiles.get(lockKey));
            return null;
        }
        lockedFiles.put(lockKey, token);
        lockTimeouts.put(lockKey, System.currentTimeMillis());


        ReentrantReadWriteLock lock = getFileLock(department, name);
        lock.readLock().lock();
        try {
            for (NodeInterface node : getAliveNodes()) {
                node.lockFileForRead(name, department);
            }
            FileInfo file = null;
            for (NodeInterface node : getAliveNodes()) {
                file = node.getFile(name, department);
                if (file != null) {
                    break;
                }
            }
            return file;
        } finally {
            lock.readLock().unlock();
        }
    }


    @Override
    public boolean editFile(String token, String name, String department, byte[] content) throws RemoteException {
        User user = validateToken(token);
        if (user == null || !user.hasPermission("edit") || !user.getDepartment().equals(department)) {
            return false;
        }
        String lockKey = department + ":" + name;
        if (lockedFiles.containsKey(lockKey)) {
            System.out.println("DEBUG: File " + name + " in department " + department + " is locked by token " + lockedFiles.get(lockKey));
            return false;
        }
        lockedFiles.put(lockKey, token);
        lockTimeouts.put(lockKey, System.currentTimeMillis());

        ReentrantReadWriteLock lock = getFileLock(department, name);
        lock.writeLock().lock();
        try {
            for (NodeInterface node : getAliveNodes()) {
                FileInfo file = node.getFile(name, department);
                if (file != null) {
                    node.lockFileForWrite(name, department);
                }
            }
            boolean success = false;
            for (NodeInterface node : getAliveNodes()) {
                FileInfo file = node.getFile(name, department);
                if (file != null) {
                    success = node.editFile(name, department, content);
                    break;
                }
            }
            return success;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void unlockFileForWriteManually(String token, String name, String department) throws RemoteException {
        User user = validateToken(token);
        if (user == null || !user.hasPermission("edit") || !user.getDepartment().equals(department)) {
            throw new RemoteException("Permission denied or invalid token");
        }
        String lockKey = department + ":" + name;
        if (lockedFiles.get(lockKey) != null && lockedFiles.get(lockKey).equals(token)) {
            ReentrantReadWriteLock lock = getFileLock(department, name);
            lock.writeLock().lock();
            try {
                for (NodeInterface node : getAliveNodes()) {
                    FileInfo file = node.getFile(name, department);
                    if (file != null) {
                        node.unlockFileForWriteManually(name, department);
                    }
                }
                lockedFiles.remove(lockKey);
                lockTimeouts.remove(lockKey);

                System.out.println("File " + name + " in department " + department + " unlocked successfully!");
            } finally {
                lock.writeLock().unlock();
            }
        } else {
            System.out.println("You do not have permission to unlock this file!");
        }
    }

    @Override
    public void unlockFileForReadManually(String token, String name, String department) throws RemoteException {
        User user = validateToken(token);
        if (user == null || !user.hasPermission("read") || !user.getDepartment().equals(department)) {
            throw new RemoteException("Permission denied or invalid token");
        }
        String lockKey = department + ":" + name;
        if (lockedFiles.get(lockKey) != null && lockedFiles.get(lockKey).equals(token)) {
            ReentrantReadWriteLock lock = getFileLock(department, name);
            lock.readLock().lock();
            try {
                for (NodeInterface node : getAliveNodes()) {
                    FileInfo file = node.getFile(name, department);
                    if (file != null) {
                        node.unlockFileForReadManually(name, department);
                    }
                }
                lockedFiles.remove(lockKey);
                lockTimeouts.remove(lockKey);

                System.out.println("File " + name + " in department " + department + " unlocked successfully!");
            } finally {
                lock.readLock().unlock();
            }
        } else {
            System.out.println("You do not have permission to unlock this file!");
        }
    }


    private User validateToken(String token) {
        String username = tokens.get(token);
        return username != null ? users.get(username) : null;
    }

@Override
    public String getDepartment(String token) {
        User user = validateToken(token);
        return user.getDepartment();
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




    public void doSomething() throws RemoteException {
        RetryUtil.retryWithSleep(() -> {
            String nodeID = pickNodeToAddFile();
            nodesMap.get(nodeID).doSomething();
        });
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

    private ReentrantReadWriteLock getFileLock(String department, String name) {
        String key = department + ":" + name;
        return fileLocks.computeIfAbsent(key, k -> new ReentrantReadWriteLock());
    }


}