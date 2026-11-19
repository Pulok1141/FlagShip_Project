package io.flagship.core.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Standard Jackson ObjectMapper configured for FlagShip snapshot serialization.
 */
public final class SnapshotJsonMapper {

    private static final ObjectMapper MAPPER = createMapper();

    private SnapshotJsonMapper() {}

    public static ObjectMapper getMapper() {
        return MAPPER;
    }

    private static ObjectMapper createMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper;
    }

    public static String toJson(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize to JSON: " + e.getMessage(), e);
        }
    }

    public static <T> T fromJson(String json, Class<T> targetClass) {
        try {
            return MAPPER.readValue(json, targetClass);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON to " + targetClass.getSimpleName() + ": " + e.getMessage(), e);
        }
    }

    public static <T> T fromJson(String json, com.fasterxml.jackson.core.type.TypeReference<T> typeRef) {
        try {
            return MAPPER.readValue(json, typeRef);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON: " + e.getMessage(), e);
        }
    }
}
