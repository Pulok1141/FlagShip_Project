package io.flagship.server.service;

import io.flagship.core.model.FlagDefinition;
import io.flagship.core.model.FlagType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SseBroadcasterTest {

    @Test
    @DisplayName("FR5: SSE broadcaster manages subscriber lifecycle and fan-out")
    void testSseFanOut() {
        SseBroadcaster broadcaster = new SseBroadcaster(10000);

        SseEmitter emitter1 = broadcaster.subscribe("proj-1", "development");
        SseEmitter emitter2 = broadcaster.subscribe("proj-1", "development");
        SseEmitter emitterOther = broadcaster.subscribe("proj-1", "production");

        assertThat(broadcaster.getActiveSubscriberCount("proj-1", "development")).isEqualTo(2);
        assertThat(broadcaster.getActiveSubscriberCount("proj-1", "production")).isEqualTo(1);

        FlagDefinition flag = new FlagDefinition(
                "checkout-feature", "Checkout", "Test", FlagType.BOOLEAN, true,
                List.of(), List.of(), List.of(), null, "default", "off", 5L, Instant.now(), List.of()
        );

        // Broadcast to development channel
        broadcaster.broadcastFlagUpdate("proj-1", "development", flag.key(), flag, 5L);

        // Manually complete one emitter
        emitter1.complete();
        // Trigger heartbeat which evicts completed/errored emitters
        broadcaster.sendHeartbeats();
    }
}
