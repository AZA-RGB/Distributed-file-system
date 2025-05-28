package common;

import java.io.Serializable;
import java.util.Base64;
import org.json.JSONObject;
import java.time.LocalDateTime;

public class FileInfo implements Serializable {
    private final String name;
    private final String department;
    private final byte[] content;
    private final LocalDateTime lastModifiedTime;


    public FileInfo(String name, String department, byte[] content) {
        this.name = name;
        this.department = department;
        this.content = content;
        this.lastModifiedTime = LocalDateTime.now();

    }

    public String getName() {
        return name;
    }

    public String getDepartment() {
        return department;
    }

    public byte[] getContent() {
        return content;
    }

    // MODIFIED: Added getter for lastModifiedTime
    public LocalDateTime getLastModifiedTime() {
        return lastModifiedTime;
    }

    // Convert FileInfo to JSON
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("name", name);
        json.put("department", department);
        json.put("content", Base64.getEncoder().encodeToString(content));
        // MODIFIED: Added lastModifiedTime to JSON
        json.put("lastModifiedTime", lastModifiedTime.toString());
        return json;
    }

    // Create FileInfo from JSON
    public static FileInfo fromJson(JSONObject json) {
        String name = json.getString("name");
        String department = json.getString("department");
        byte[] content = Base64.getDecoder().decode(json.getString("content"));
        // Parse lastModifiedTime from JSON
        LocalDateTime lastModifiedTime = LocalDateTime.parse(json.getString("lastModifiedTime"));
        FileInfo fileInfo = new FileInfo(name, department, content);
        // Use reflection to set lastModifiedTime since constructor already sets it
        try {
            java.lang.reflect.Field field = FileInfo.class.getDeclaredField("lastModifiedTime");
            field.setAccessible(true);
            field.set(fileInfo, lastModifiedTime);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            System.err.println("Error setting lastModifiedTime: " + e.getMessage());
        }
        return fileInfo;

    }

    @Override
    public String toString() {
        return "FileInfo{name='" + name + "', department='" + department + "'}";
    }
}