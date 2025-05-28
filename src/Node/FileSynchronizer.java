package Node;

import common.FileInfo;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.*;
//import java.net.ServerSocket;
//import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FileSynchronizer {
    private final NodeImpl node;
    private final int port;
    private final String[] otherNodes; // List of other nodes' addresses (host:port)

    public FileSynchronizer(NodeImpl node, int port, String[] otherNodes) {
        this.node = node;
        this.port = port;
        this.otherNodes = otherNodes;
    }

    // Start the synchronization server
    public void startServer() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                System.out.println("Node " + node.getNodeId() + " listening for sync requests on port " + port);
                while (true) {
                    try (Socket socket = serverSocket.accept();
                         ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                         ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
                        out.writeObject(node.getFiles());
                    } catch (IOException e) {
                        System.err.println("Error handling sync request on node " + node.getNodeId() + ": " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                System.err.println("Could not start sync server on node " + node.getNodeId() + ": " + e.getMessage());
            }
        }).start();
    }

    // Handle synchronization requests from other nodes
    private void handleClient(Socket clientSocket) {
        try (ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
             ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream())) {
            // Receive the files map from the other node
            String jsonString = (String) in.readObject();
            JSONArray departmentsArray = new JSONArray(jsonString);
            Map<String, Map<String, FileInfo>> receivedFiles = new ConcurrentHashMap<>();
            for (int i = 0; i < departmentsArray.length(); i++) {
                JSONObject deptJson = departmentsArray.getJSONObject(i);
                String department = deptJson.getString("department");
                JSONArray filesArray = deptJson.getJSONArray("files");
                Map<String, FileInfo> deptFiles = new ConcurrentHashMap<>();
                for (int j = 0; j < filesArray.length(); j++) {
                    FileInfo fileInfo = FileInfo.fromJson(filesArray.getJSONObject(j));
                    deptFiles.put(fileInfo.getName(), fileInfo);
                }
                receivedFiles.put(department, deptFiles);
            }

            // Synchronize local files with received files
            synchronizeFiles(receivedFiles);

            // Send acknowledgment
            out.writeObject("SYNC_SUCCESS");
        } catch (Exception e) {
            System.err.println("Node " + node.getNodeId() + " sync client error: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing client socket: " + e.getMessage());
            }
        }
    }

    // Synchronize local files with received files
    private void synchronizeFiles(Map<String, Map<String, FileInfo>> receivedFiles) throws IOException {
        Map<String, Map<String, FileInfo>> localFiles = node.getFiles();
        // Add or update files from receivedFiles to localFiles
        receivedFiles.forEach((department, deptFiles) -> {
            localFiles.computeIfAbsent(department, k -> new ConcurrentHashMap<>());
            deptFiles.forEach((name, fileInfo) -> {
                localFiles.get(department).put(name, fileInfo);
                System.out.println("Node " + node.getNodeId() + " synced file " + name + " in department " + department);
            });
        });

        // Remove files that are in localFiles but not in receivedFiles
        localFiles.forEach((department, deptFiles) -> {
            Map<String, FileInfo> receivedDeptFiles = receivedFiles.get(department);
            if (receivedDeptFiles != null) {
                deptFiles.keySet().removeIf(name -> !receivedDeptFiles.containsKey(name));
            } else {
                localFiles.remove(department);
            }
        });

        // Save the updated files to JSON
        node.saveFilesToJson();
    }

    // Initiate synchronization with other nodes
    public void synchronizeWithOtherNodes() {
        for (String otherNode : otherNodes) {
            String[] parts = otherNode.split(":");
            String host = parts[0];
            int port = Integer.parseInt(parts[1]);
            try (Socket socket = new Socket(host, port);
                 ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
                @SuppressWarnings("unchecked")
                Map<String, Map<String, FileInfo>> otherFiles = (Map<String, Map<String, FileInfo>>) in.readObject();
                node.compareFiles(otherFiles);
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Error syncing with node at " + otherNode + ": " + e.getMessage());
            }
        }
    }
}