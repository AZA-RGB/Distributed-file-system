package common;

import java.io.Serializable;
import java.util.Base64;
import org.json.JSONObject;

public class FileInfo implements Serializable {
    private final String name;
    private final String department;
    private final byte[] content;

    public FileInfo(String name, String department, byte[] content) {
        this.name = name;
        this.department = department;
        this.content = content;
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

    // Convert FileInfo to JSON
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("name", name);
        json.put("department", department);
        json.put("content", Base64.getEncoder().encodeToString(content));
        return json;
    }

    // Create FileInfo from JSON
    public static FileInfo fromJson(JSONObject json) {
        String name = json.getString("name");
        String department = json.getString("department");
        byte[] content = Base64.getDecoder().decode(json.getString("content"));
        return new FileInfo(name, department, content);
    }
}