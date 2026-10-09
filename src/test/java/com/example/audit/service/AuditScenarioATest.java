
package com.example.audit.service;

import com.example.audit.entity.AuditChainState;
import com.example.audit.entity.AuditEvent;
import com.example.audit.repository.AuditChainStateRepository;
import com.example.audit.repository.AuditEventRepository;
import com.example.audit.service.HashChainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuditScenarioATest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuditEventRepository eventRepository;

    @Autowired
    private AuditChainStateRepository stateRepository;

    @Autowired
    private HashChainService hashChainService;

    @BeforeEach
    void cleanDatabase() {
        eventRepository.deleteAll();
        stateRepository.deleteAll();

        stateRepository.saveAndFlush(new AuditChainState(
                1L,
                0L,
                hashChainService.getGenesisHash()));
    }

    @Test
    void shouldCreateAuditEventAndVerifyChain() throws Exception {
        createEvent("USER_CREATED", "user-123", "user-001");

        mockMvc.perform(get("/audit/events")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].sequenceNumber").value(1))
                .andExpect(jsonPath("$.content[0].eventType")
                        .value("USER_CREATED"));

        mockMvc.perform(get("/audit/verify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    void shouldFilterEventsAndPaginate() throws Exception {
        createEvent("USER_CREATED", "user-123", "user-001");
        createEvent("PAYMENT_CREATED", "user-456", "payment-001");
        createEvent("USER_UPDATED", "user-123", "user-001");

        mockMvc.perform(get("/audit/events")
                .param("actorId", "user-123")
                .param("page", "0")
                .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].actorId")
                        .value("user-123"))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));

        mockMvc.perform(get("/audit/events")
                .param("eventType", "PAYMENT_CREATED")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].eventType")
                        .value("PAYMENT_CREATED"));

        mockMvc.perform(get("/audit/verify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    @Transactional
    void shouldDetectTamperedEventContent() throws Exception {
        createEvent("USER_CREATED", "user-123", "user-001");

        AuditEvent event = eventRepository
                .findAllByOrderBySequenceNumberAsc()
                .get(0);

        // Simulate unauthorized modification of stored content.
        ReflectionTestUtils.setField(
                event, "eventType", "USER_DELETED");

        eventRepository.saveAndFlush(event);

        mockMvc.perform(get("/audit/verify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.firstInvalidSequence").value(1))
                .andExpect(jsonPath("$.violationType")
                        .value("EVENT_HASH_MISMATCH"));
    }

    private void createEvent(
            String eventType,
            String actorId,
            String resourceId) throws Exception {

        String request = """
                {
                  "eventType": "%s",
                  "actorId": "%s",
                  "resourceType": "TEST_RESOURCE",
                  "resourceId": "%s",
                  "payload": {
                    "status": "ACTIVE",
                    "testData": true
                  }
                }
                """.formatted(eventType, actorId, resourceId);

        mockMvc.perform(post("/audit/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().is2xxSuccessful());
    }
}
