package io.flagship.server.command;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.flagship.core.model.SnapshotJsonMapper;
import org.springframework.stereotype.Service;

import java.util.Iterator;

/**
 * Computes structured human-readable JSON diffs between flag configurations.
 */
@Service
public class JsonDiffService {

    private final ObjectMapper mapper = SnapshotJsonMapper.getMapper();

    public String computeDiff(String oldJson, String newJson) {
        try {
            JsonNode oldNode = (oldJson != null && !oldJson.isBlank()) ? mapper.readTree(oldJson) : mapper.createObjectNode();
            JsonNode newNode = (newJson != null && !newJson.isBlank()) ? mapper.readTree(newJson) : mapper.createObjectNode();

            ObjectNode diff = mapper.createObjectNode();
            compareNodes("", oldNode, newNode, diff);
            return mapper.writeValueAsString(diff);
        } catch (Exception e) {
            return "{\"error\": \"Failed to compute diff: " + e.getMessage() + "\"}";
        }
    }

    private void compareNodes(String prefix, JsonNode oldNode, JsonNode newNode, ObjectNode diff) {
        if (oldNode.isObject() && newNode.isObject()) {
            Iterator<String> fieldNames = newNode.fieldNames();
            while (fieldNames.hasNext()) {
                String field = fieldNames.next();
                String path = prefix.isEmpty() ? field : prefix + "." + field;
                JsonNode oldVal = oldNode.get(field);
                JsonNode newVal = newNode.get(field);
                if (oldVal == null) {
                    ObjectNode change = diff.putObject(path);
                    change.putNull("old");
                    change.set("new", newVal);
                } else if (!oldVal.equals(newVal)) {
                    if (oldVal.isObject() && newVal.isObject()) {
                        compareNodes(path, oldVal, newVal, diff);
                    } else {
                        ObjectNode change = diff.putObject(path);
                        change.set("old", oldVal);
                        change.set("new", newVal);
                    }
                }
            }

            // Check deleted fields
            Iterator<String> oldFields = oldNode.fieldNames();
            while (oldFields.hasNext()) {
                String field = oldFields.next();
                String path = prefix.isEmpty() ? field : prefix + "." + field;
                if (!newNode.has(field)) {
                    ObjectNode change = diff.putObject(path);
                    change.set("old", oldNode.get(field));
                    change.putNull("new");
                }
            }
        } else if (!oldNode.equals(newNode)) {
            ObjectNode change = diff.putObject(prefix.isEmpty() ? "root" : prefix);
            change.set("old", oldNode);
            change.set("new", newNode);
        }
    }
}
