package com.assessment.fraud.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rulesRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/rules")).andExpect(status().isUnauthorized());
    }

    @Test
    void analystCanReadRules() throws Exception {
        mockMvc.perform(get("/api/v1/rules").with(httpBasic("analyst", "analyst")))
                .andExpect(status().isOk());
    }

    @Test
    void ingestCannotReadRules() throws Exception {
        mockMvc.perform(get("/api/v1/rules").with(httpBasic("ingest", "ingest")))
                .andExpect(status().isForbidden());
    }

    @Test
    void ingestCanPostEvents() throws Exception {
        String body =
                """
                {
                  "externalId": "sec-tx-1",
                  "customerId": "cust-sec",
                  "accountId": "acc-1",
                  "amount": 12.50,
                  "currency": "USD",
                  "category": "GROCERIES",
                  "merchantName": "Shop",
                  "merchantCountry": "ZA",
                  "occurredAt": "2026-06-01T10:00:00Z"
                }
                """;
        mockMvc.perform(post("/api/v1/transactions/events")
                        .with(httpBasic("ingest", "ingest"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }
}
