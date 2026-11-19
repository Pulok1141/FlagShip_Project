package io.flagship.demo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "flagship.offline-mode=true"
})
class DemoAppIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("QA5: Verify demo app starts up cleanly and executes feature flagged service")
    void testDemoAppCheckoutAndActuator() throws Exception {
        // 1. Verify feature flagged checkout endpoint
        mockMvc.perform(get("/api/checkout")
                        .param("user", "alice@example.com")
                        .param("amount", "149.99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkout.userId").value("alice@example.com"))
                .andExpect(jsonPath("$.checkout.status").value("SUCCESS"))
                .andExpect(jsonPath("$.experimentEventTracked").value("checkout_completed"));

        // 2. Verify Actuator /actuator/flagship endpoint
        mockMvc.perform(get("/actuator/flagship"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.syncMode").value("OFFLINE"));
    }
}
