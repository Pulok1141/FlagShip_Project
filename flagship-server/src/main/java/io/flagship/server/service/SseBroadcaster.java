package io.flagship.server.service;

import io.flagship.core.model.FlagDefinition;
import io.flagship.core.model.SnapshotJsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * High-concurrency SSE Fan-out Broadcaster using Java 21 Virtual Threads (FR5, QA3).
 * Manages connected client emitters by projectId:envKey, handles heartbeats, and evicts dead emitters safely.
 */
@Service
public class SseBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(SseBroadcaster.class);

    // Map of "projectId:envKey" -> Set of active SseEmitters
    private final ConcurrentHashMap<String, Set<SseEmitter>> channels = new ConcurrentHashMap<>();

    private final long timeoutMs;

    public SseBroadcaster(
            @Value("${flagship.sse.timeout-ms:0}") long timeoutMs // 0 means default infinite/long-lived
    ) {
        this.timeoutMs = timeoutMs;
    }

    public SseEmitter subscribe(String projectId, String envKey) {
        String channelKey = buildChannelKey(projectId, envKey);
        SseEmitter emitter = new SseEmitter(timeoutMs > 0 ? timeoutMs : Long.MAX_VALUE);

        Set<SseEmitter> emitters = channels.computeIfAbsent(channelKey, k -> new CopyOnWriteArraySet<>());
        emitters.add(emitter);

        emitter.onCompletion(() -> {
            emitters.remove(emitter);
            log.debug("SSE client completed on channel {}", channelKey);
        });

        emitter.onTimeout(() -> {
            emitters.remove(emitter);
            emitter.complete();
            log.debug("SSE client timed out on channel {}", channelKey);
        });

        emitter.onError(e -> {
            emitters.remove(emitter);
            log.debug("SSE client error on channel {}: {}", channelKey, e.getMessage());
        });

        // Send initial connection ACK
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("{\"status\":\"CONNECTED\",\"channel\":\"" + channelKey + "\"}"));
        } catch (IOException e) {
            emitters.remove(emitter);
            emitter.completeWithError(e);
        }

        return emitter;
    }

    public void broadcastFlagUpdate(String projectId, String envKey, String flagKey, FlagDefinition newFlag, long version) {
        String channelKey = buildChannelKey(projectId, envKey);
        Set<SseEmitter> emitters = channels.get(channelKey);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        String payload = SnapshotJsonMapper.toJson(Map.of(
                "eventType", "FLAG_UPDATED",
                "projectId", projectId,
                "envKey", envKey,
                "flagKey", flagKey,
                "version", version,
                "flag", newFlag
        ));

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("flag_updated")
                        .id(String.valueOf(version))
                        .data(payload));
            } catch (Exception e) {
                emitters.remove(emitter);
                log.debug("Evicted disconnected SSE emitter on send failure: {}", e.getMessage());
            }
        }
    }

    public void broadcastFullSnapshotReload(String projectId, String envKey, long version) {
        String channelKey = buildChannelKey(projectId, envKey);
        Set<SseEmitter> emitters = channels.get(channelKey);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        String payload = "{\"eventType\":\"SNAPSHOT_RELOAD\",\"version\":" + version + "}";
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("snapshot-reload")
                        .id(String.valueOf(version))
                        .data(payload));
            } catch (Exception e) {
                emitters.remove(emitter);
            }
        }
    }

    @Scheduled(fixedRateString = "${flagship.sse.heartbeat-interval-ms:15000}")
    public void sendHeartbeats() {
        for (Map.Entry<String, Set<SseEmitter>> entry : channels.entrySet()) {
            Set<SseEmitter> emitters = entry.getValue();
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event().comment("heartbeat"));
                } catch (Exception e) {
                    emitters.remove(emitter);
                }
            }
        }
    }

    public int getActiveSubscriberCount(String projectId, String envKey) {
        Set<SseEmitter> emitters = channels.get(buildChannelKey(projectId, envKey));
        return emitters != null ? emitters.size() : 0;
    }

    private String buildChannelKey(String projectId, String envKey) {
        return projectId + ":" + envKey;
    }
}
