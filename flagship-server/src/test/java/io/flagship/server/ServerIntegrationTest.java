package io.flagship.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.flagship.core.model.SnapshotJsonMapper;
import io.flagship.server.domain.ExperimentEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ServerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper mapper = SnapshotJsonMapper.getMapper();

    @Test
    @DisplayName("End-to-End Control Plane: Auth, SDK Snapshot with ETag 304, and Event Ingestion")
    void testEndToEndControlPlane() throws Exception {
        System.out.println(">>> STEP 1: Logging in...");
        Map<String, String> loginReq = Map.of("email", "admin@flagship.io", "password", "admin123");
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andReturn();

        Map<?, ?> loginRes = mapper.readValue(loginResult.getResponse().getContentAsString(), Map.class);
        String jwtToken = (String) loginRes.get("token");
        System.out.println(">>> STEP 1 COMPLETE. Token: " + jwtToken.substring(0, 15) + "...");

        System.out.println(">>> STEP 2: Querying projects...");
        mockMvc.perform(get("/api/projects")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
        System.out.println(">>> STEP 2 COMPLETE.");

        System.out.println(">>> STEP 3: Querying SDK snapshot...");
        MvcResult snapshotResult = mockMvc.perform(get("/api/sdk/v1/snapshot")
                        .header("X-FlagShip-Key", "flagship_dev_key_12345"))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.ETAG))
                .andExpect(jsonPath("$.projectId").value("proj-default"))
                .andExpect(jsonPath("$.environmentKey").value("development"))
                .andReturn();

        String etag = snapshotResult.getResponse().getHeader(HttpHeaders.ETAG);
        assertThat(etag).isNotNull();
        System.out.println(">>> STEP 3 COMPLETE. ETag: " + etag);

        System.out.println(">>> STEP 4: Conditional snapshot with ETag...");
        mockMvc.perform(get("/api/sdk/v1/snapshot")
                        .header("X-FlagShip-Key", "flagship_dev_key_12345")
                        .header(HttpHeaders.IF_NONE_MATCH, etag))
                .andExpect(status().isNotModified());
        System.out.println(">>> STEP 4 COMPLETE.");

        System.out.println(">>> STEP 5: Ingesting events...");
        ExperimentEvent event = new ExperimentEvent(
                null, "proj-default", "development", "new-checkout-flow",
                "true-var", "test-user-999", ExperimentEvent.EventType.EXPOSURE, null, 1.0, Instant.now()
        );

        mockMvc.perform(post("/api/sdk/v1/events")
                        .header("X-FlagShip-Key", "flagship_dev_key_12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("events", List.of(event)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
        System.out.println(">>> STEP 5 COMPLETE. ALL INTEGRATION TESTS PASSED!");
    }
}
