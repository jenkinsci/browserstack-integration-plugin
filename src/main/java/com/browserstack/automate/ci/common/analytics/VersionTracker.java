package com.browserstack.automate.ci.common.analytics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * @author Shirish Kamath
 * @author Anirudha Khanna
 */
public class VersionTracker {

    private static final String ID_FILENAME = "browserstack-id.txt";

    private static final ObjectMapper mapper = new ObjectMapper();

    private final File rootDir;

    private ObjectNode metadata;

    public VersionTracker(File rootDir) {
        this.rootDir = rootDir;
    }

    public boolean init(String pluginVersion) throws IOException {
        try {
            loadMetadata();
        } catch (IOException e) {
            // first install (?)
            saveMetadata(pluginVersion);
            return true;
        }

        return false;
    }

    public String getClientId() throws IOException {
        return getProperty("id");
    }

    public String getPluginVersion() {
        return getProperty("version");
    }

    public String getProperty(String name) {
        return (metadata != null && metadata.has(name)) ? metadata.get(name).asText() : null;
    }

    public boolean updateVersion(String version) throws IOException {
        String pluginVersion = getPluginVersion();
        boolean needsUpdate = (pluginVersion == null || !pluginVersion.equals(version));
        if (needsUpdate) {
            saveMetadata(version);
            return true;
        }

        return false;
    }

    public void loadMetadata() throws IOException {
        metadata = mapper.readValue(new File(rootDir, ID_FILENAME), ObjectNode.class);
    }

    private void saveMetadata(String pluginVersion) throws IOException {
        String instanceId = UUID.randomUUID().toString().replace("\\-", "");

        ObjectNode metadata = mapper.createObjectNode();
        metadata.put("id", instanceId);
        metadata.put("version", pluginVersion);
        metadata.put("timestamp", System.currentTimeMillis());

        File f = new File(rootDir, ID_FILENAME);
        try {
            mapper.writeValue(f, metadata);
        } catch (IOException ex) {
            throw new IOException("Failed to store plugin metadata", ex);
        }
    }
}
