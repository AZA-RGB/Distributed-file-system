package Node;

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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.Base64;


public class NodeImpl extends UnicastRemoteObject implements NodeInterface {
    private final String nodeId;
    private final String storagePath;
    private final CoordinatorInterface coordinator;
    private final Map<String, Map<String, FileInfo>> files; // department -> name -> FileInfo
    private final ReentrantLock fileLock = new ReentrantLock(); // Lock for file operations
    private final String filesJsonPath;
    private final ConcurrentHashMap<String, ReentrantReadWriteLock> fileLocks = new ConcurrentHashMap<>();
    private final FileSynchronizer synchronizer;


    private NodeImpl(String id, String storagePath, CoordinatorInterface c) throws RemoteException {
        super();
        this.nodeId = id;
        this.storagePath = storagePath;
        this.coordinator = c;
        this.files = new ConcurrentHashMap<>();
        this.filesJsonPath = storagePath + "/files.json"; // e.g., NodeStorage-1/files.json
        ensureStorageDirectory(); // Ensure directory exists
        loadFilesFromJson(); // Load files at startup

        // Initialize synchronization (port and other nodes)
        int port = 5000 + Integer.parseInt(id); // Unique port for each node
        String[] otherNodes = getOtherNodes(id);
        this.synchronizer = new FileSynchronizer(this, port, otherNodes);
        synchronizer.startServer(); // Start the sync server

        // Schedule daily synchronization at midnight
        scheduleDailySynchronization();

        // Heartbeat scheduler
        ScheduledExecutorService heartBeatScheduler = Executors.newScheduledThreadPool(1);

        heartBeatScheduler.scheduleAtFixedRate(() -> {
            try {
                this.coordinator.receiveHeartbeat(this.nodeId);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }, 0, 500, TimeUnit.MILLISECONDS);
    }

    public static NodeImpl getInstance(String id, CoordinatorInterface c) throws RemoteException {
        String storagePath = findStoragePath(id);
        if (storagePath != null) {
            return new NodeImpl(id, storagePath, c);
        }
        System.out.println("This is a new Node ");
        System.out.println("Creating new storage folder...");
        storagePath = "NodeStorage-" + id;
        File directory = new File(storagePath);
        if (directory.mkdirs()) { // Use mkdirs to create parent directories if needed
            System.out.println("Directory created successfully: " + storagePath);
            addToNodesList(id, storagePath);
            return new NodeImpl(id, storagePath, c);
        } else {
            System.err.println("Failed to create directory: " + storagePath);
            throw new RemoteException("Failed to create storage directory for node " + id);
        }
    }

    private String[] getOtherNodes(String currentNodeId) {
        try (FileReader fileReader = new FileReader("src/Node/NodesList.json")) {
            JSONTokener tokener = new JSONTokener(fileReader);
            JSONArray jsonArray = new JSONArray(tokener);
            String[] otherNodes = new String[jsonArray.length() - 1];
            int index = 0;
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject node = jsonArray.getJSONObject(i);
                String nodeId = node.getString("node_id");
                if (!nodeId.equals(currentNodeId)) {
                    int port = 5000 + Integer.parseInt(nodeId);
                    otherNodes[index++] = "localhost:" + port;
                }
            }
            return otherNodes;
        } catch (IOException e) {
            System.err.println("Error reading NodesList.json: " + e.getMessage());
            return new String[]{};
        }
    }

    private void scheduleDailySynchronization() {
        ScheduledExecutorService syncScheduler = Executors.newScheduledThreadPool(1);

        // Schedule synchronization everyday
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextMidnight = now.with(LocalTime.MIDNIGHT).plusDays(1);
        long initialDelay = ChronoUnit.MILLIS.between(now, nextMidnight);
        syncScheduler.scheduleAtFixedRate(() -> {
            System.out.println("Node " + nodeId + " starting daily synchronization...");
            synchronizer.synchronizeWithOtherNodes();
        }, initialDelay, 24 * 60 * 60, TimeUnit.SECONDS);

        // Schedule synchronization every 10 seconds for testing
//        syncScheduler.scheduleAtFixedRate(() -> {
//            System.out.println("Node " + nodeId + " starting immediate synchronization for testing...");
//            synchronizer.synchronizeWithOtherNodes();
//        }, 0, 10, TimeUnit.SECONDS); // Synchronize every 10 seconds
    }
    private void scheduleHeartbeat() {
        ScheduledExecutorService heartBeatScheduler = Executors.newScheduledThreadPool(1);
        heartBeatScheduler.scheduleAtFixedRate(() -> {
            try {
                this.coordinator.receiveHeartbeat(this.nodeId);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }, 0, 500, TimeUnit.MILLISECONDS);
    }

    private void ensureStorageDirectory() throws RemoteException {
        try {
            Files.createDirectories(Paths.get(storagePath));
        } catch (IOException e) {
            throw new RemoteException("Failed to create storage directory for node " + nodeId + ": " + e.getMessage());
        }
    }

    private void loadFilesFromJson() {
        fileLock.lock();
        try {
            Path jsonPath = Paths.get(filesJsonPath);
            if (Files.exists(jsonPath)) {
                try (FileReader fileReader = new FileReader(jsonPath.toFile())) {
                    JSONTokener tokener = new JSONTokener(fileReader);
                    JSONArray departmentsArray = new JSONArray(tokener);
                    for (int i = 0; i < departmentsArray.length(); i++) {
                        JSONObject deptJson = departmentsArray.getJSONObject(i);
                        String department = deptJson.getString("department");
                        JSONArray filesArray = deptJson.getJSONArray("files");
                        Map<String, FileInfo> deptFiles = new ConcurrentHashMap<>();
                        for (int j = 0; j < filesArray.length(); j++) {
                            JSONObject fileJson = filesArray.getJSONObject(j);
                            String name = fileJson.getString("name");
                            String dept = fileJson.getString("department");
                            byte[] content;
                            Object contentObj = fileJson.get("content");
                            if (contentObj instanceof String) {
                                try {
                                    content = Base64.getDecoder().decode((String) contentObj);
                                } catch (IllegalArgumentException e) {
                                    System.err.println("Invalid Base64 content for file " + name + ", treating as plain string: " + e.getMessage());
                                    content = ((String) contentObj).getBytes();
                                }
                            } else {
                                content = (byte[]) contentObj;
                            }
                            LocalDateTime lastModifiedTime = LocalDateTime.now();
                            if (fileJson.has("lastModifiedTime")) {
                                lastModifiedTime = LocalDateTime.parse(fileJson.getString("lastModifiedTime"));
                            } else {
                                System.out.println("Warning: lastModifiedTime not found for file " + name + ", using current time");
                            }
                            FileInfo fileInfo = new FileInfo(name, dept, content);
                            // Use reflection to set lastModifiedTime
                            try {
                                java.lang.reflect.Field field = FileInfo.class.getDeclaredField("lastModifiedTime");
                                field.setAccessible(true);
                                field.set(fileInfo, lastModifiedTime);
                            } catch (NoSuchFieldException | IllegalAccessException e) {
                                System.err.println("Error setting lastModifiedTime for file " + name + ": " + e.getMessage());
                            }
                            deptFiles.put(name, fileInfo);
                        }
                        files.put(department, deptFiles);
                    }
                    System.out.println("Loaded " + departmentsArray.length() + " departments from " + filesJsonPath);
                } catch (IOException | org.json.JSONException e) {
                    System.err.println("Error loading files from JSON for node " + nodeId + ": " + e.getMessage());
                }
            } else {
                System.out.println("No files.json found for node " + nodeId + ", starting with empty files map");
            }
        } finally {
            fileLock.unlock();
        }
    }

     void saveFilesToJson() {
        fileLock.lock();
        try {
            // Write to a temporary file first
            Path tempPath = Paths.get(filesJsonPath + ".tmp");
            JSONArray departmentsArray = new JSONArray();
            files.forEach((department, deptFiles) -> {
                JSONObject deptJson = new JSONObject();
                deptJson.put("department", department);
                JSONArray filesArray = new JSONArray();
                deptFiles.forEach((name, fileInfo) -> filesArray.put(fileInfo.toJson()));
                deptJson.put("files", filesArray);
                departmentsArray.put(deptJson);
            });
            Files.write(tempPath, departmentsArray.toString(2).getBytes());
            // Atomically rename temporary file to final file
            Files.move(tempPath, Paths.get(filesJsonPath), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Saved " + departmentsArray.length() + " departments to " + filesJsonPath);
        } catch (IOException e) {
            System.err.println("Error saving files to JSON for node " + nodeId + ": " + e.getMessage());
        } finally {
            fileLock.unlock();
        }
    }

    private static boolean addToNodesList(String nodeId, String storagePath) {
        try {
            String filePath = "src/Node/NodesList.json";
            Path jsonPath = Paths.get(filePath);
            Files.createDirectories(jsonPath.getParent()); // Ensure parent directory exists
            JSONArray nodesArray;
            if (Files.exists(jsonPath)) {
                String content = new String(Files.readAllBytes(jsonPath));
                nodesArray = new JSONArray(content);
            } else {
                nodesArray = new JSONArray();
            }
            JSONObject newNode = new JSONObject();
            newNode.put("node_id", nodeId);
            newNode.put("storagePath", storagePath);
            nodesArray.put(newNode);
            Files.write(jsonPath, nodesArray.toString(2).getBytes());
            System.out.println("Added node " + nodeId + " to JSON list");
            return true;
        } catch (IOException e) {
            System.err.println("Error adding node to NodesList.json: " + e.getMessage());
            return false;
        }
    }

    private static String findStoragePath(String targetNodeId) {
        try (FileReader fileReader = new FileReader("src/Node/NodesList.json")) {
            JSONTokener tokener = new JSONTokener(fileReader);
            JSONArray jsonArray = new JSONArray(tokener);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject node = jsonArray.getJSONObject(i);
                String currentId = node.getString("node_id");
                if (Objects.equals(currentId, targetNodeId)) {
                    return node.getString("storagePath");
                }
            }
            return null;
        } catch (IOException e) {
            System.err.println("Error reading NodesList.json: " + e.getMessage());
            return null;
        }
    }

    private ReentrantReadWriteLock getFileLock(String department, String name) {
        String key = department + ":" + name;
        return fileLocks.computeIfAbsent(key, k -> new ReentrantReadWriteLock());
    }

    public void compareFiles(Map<String, Map<String, FileInfo>> otherFiles) {
        LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
        otherFiles.forEach((department, otherDeptFiles) -> {
            files.computeIfAbsent(department, k -> new ConcurrentHashMap<>());
            Map<String, FileInfo> localDeptFiles = files.get(department);
            otherDeptFiles.forEach((name, otherFileInfo) -> {
                FileInfo localFileInfo = localDeptFiles.get(name);
                if (localFileInfo == null) {
                    // File doesn't exist locally, add it
                    localDeptFiles.put(name, otherFileInfo);
                    System.out.println("Node " + nodeId + " added new file " + name + " in department " + department + " during sync");
                } else {
                    // File exists, check if it was modified today and update if necessary
                    LocalDateTime otherModifiedTime = otherFileInfo.getLastModifiedTime();
                    LocalDateTime localModifiedTime = localFileInfo.getLastModifiedTime();
                    if (otherModifiedTime.isAfter(todayStart) && otherModifiedTime.isAfter(localModifiedTime)) {
                        localDeptFiles.put(name, otherFileInfo);
                        System.out.println("Node " + nodeId + " updated file " + name + " in department " + department + " during sync");
                    }
                }
            });
        });
        saveFilesToJson();
    }


    @Override
    public boolean addFile(String name, String department, byte[] content) throws RemoteException {
        ReentrantReadWriteLock lock = getFileLock(department, name);
        lock.writeLock().lock();
        try {
            files.computeIfAbsent(department, k -> new ConcurrentHashMap<>());
            FileInfo fileInfo = new FileInfo(name, department, content);
            files.get(department).put(name, fileInfo);
            saveFilesToJson(); // Save to JSON after adding
            System.out.println("Added file " + name + " to department " + department + " on node " + nodeId);
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean deleteFile(String name, String department) throws RemoteException {
        ReentrantReadWriteLock lock = getFileLock(department, name);
        lock.writeLock().lock();
        try{
            Map<String, FileInfo> deptFiles = files.get(department);
            if (deptFiles != null) {
                FileInfo removed = deptFiles.remove(name);
                if (removed != null) {
                    saveFilesToJson(); // Save to JSON after deleting
                    System.out.println("Deleted file " + name + " from department " + department + " on node " + nodeId);
                    return true;
                }
            }
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public FileInfo getFile(String name, String department) throws RemoteException {
        ReentrantReadWriteLock lock = getFileLock(department, name);
        lock.readLock().lock();
        try{
            Map<String, FileInfo> deptFiles = files.get(department);
            FileInfo fileInfo = deptFiles != null ? deptFiles.get(name) : null;
            if (fileInfo != null) {
                System.out.println("Retrieved file " + name + " from department " + department + " on node " + nodeId);
            }
            return fileInfo;
        } finally {
            lock.readLock().unlock();
        }
    }
    @Override
    public boolean editFile(String name, String department, byte[] content) throws RemoteException {
        ReentrantReadWriteLock lock = getFileLock(department, name);
        lock.writeLock().lock();
        try{
            Map<String, FileInfo> deptFiles = files.get(department);
            if (deptFiles != null) {
                FileInfo existingFile = deptFiles.get(name);
                if (existingFile != null) {
                    FileInfo updatedFile = new FileInfo(name, department, content);
                    deptFiles.put(name, updatedFile);
                    saveFilesToJson(); // Save to JSON after editing
                    System.out.println("Edited file " + name + " in department " + department + " on node " + nodeId);
                    return true;
                }
            }
            System.out.println("Failed to edit file " + name + " in department " + department + " on node " + nodeId + ": File or department not found");
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }
    @Override
    public boolean isAlive() throws RemoteException {
        return true;
    }

    public void doSomething() throws RemoteException, InterruptedException {
        Thread.sleep(3000);
        System.out.println("node: "+nodeId+" is doing something.....");
//        coordinator.deLoad(nodeId);
    }

    public String getNodeId() {
        return nodeId;
    }

    // New: Getter for files map (needed for synchronization)
    public Map<String, Map<String, FileInfo>> getFiles() {
        return files;
    }
    // New: Implement lock/unlock methods for read/write
    @Override
    public void lockFileForRead(String name, String department) throws RemoteException {
        ReentrantReadWriteLock lock = getFileLock(department, name);
        lock.readLock().lock();
        System.out.println("Locked file " + name + " in department " + department + " for reading on node " + nodeId);
    }

    @Override
    public void unlockFileForRead(String name, String department) throws RemoteException {
        ReentrantReadWriteLock lock = getFileLock(department, name);
        if (lock.getReadLockCount() > 0 || lock.isWriteLockedByCurrentThread()) {
            lock.readLock().unlock();
            System.out.println("Unlocked file " + name + " in department " + department + " for reading on node " + nodeId);
        } else {
            System.out.println("No lock to unlock for file " + name + " in department " + department + " on node " + nodeId);
        }
    }

    @Override
    public void lockFileForWrite(String name, String department) throws RemoteException {
        ReentrantReadWriteLock lock = getFileLock(department, name);
        lock.writeLock().lock();
        System.out.println("Locked file " + name + " in department " + department + " for writing on node " + nodeId);
    }

    @Override
    public void unlockFileForWrite(String name, String department) throws RemoteException {
        ReentrantReadWriteLock lock = getFileLock(department, name);
        if (lock.isWriteLockedByCurrentThread()) {
            lock.writeLock().unlock();
            System.out.println("Unlocked file " + name + " in department " + department + " for writing on node " + nodeId);
        } else {
            System.out.println("No write lock to unlock for file " + name + " in department " + department + " on node " + nodeId);
        }
    }

    // New: Manual unlock method
    @Override
    public void unlockFileForWriteManually(String name, String department) throws RemoteException {
        ReentrantReadWriteLock lock = getFileLock(department, name);
        if (lock.isWriteLockedByCurrentThread()) {
            lock.writeLock().unlock();
            System.out.println("Manually unlocked file " + name + " in department " + department + " for writing on node " + nodeId);
        } else {
            System.out.println("No write lock to manually unlock for file " + name + " in department " + department + " on node " + nodeId);
        }
    }

    @Override
    public void unlockFileForReadManually(String name, String department) throws RemoteException {
        ReentrantReadWriteLock lock = getFileLock(department, name);
        if (lock.getReadLockCount() > 0) {
            lock.readLock().unlock();
            System.out.println("Manually unlocked file " + name + " in department " + department + " for reading on node " + nodeId);
        } else {
            System.out.println("No read lock to manually unlock for file " + name + " in department " + department + " on node " + nodeId);
        }
    }

    @Override
    public boolean isWriteLocked(String name, String department) throws RemoteException {
        ReentrantReadWriteLock lock = getFileLock(department, name);
        return lock.isWriteLocked();
    }

}