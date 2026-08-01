package com.sagant.distributednotification.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sagant.distributednotification.config.security.ApiKeyAuthFilter;
import com.sagant.distributednotification.domain.entity.Notification;
import com.sagant.distributednotification.domain.model.NotificationChannel;
import com.sagant.distributednotification.domain.model.NotificationRequest;
import com.sagant.distributednotification.domain.model.NotificationStatus;
import com.sagant.distributednotification.repository.NotificationRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class NotificationApiIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16.13");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NotificationRepository notificationRepository;

    @Value("${notification.security.api-key}")
    private String apiKey;

    @BeforeEach
    void cleanUp() {
        notificationRepository.deleteAll();
    }

    @Test
    void createNotification_withValidPayloadAndApiKey_persistsAndEventuallyMarksAsSent() throws Exception {
        final NotificationRequest request = new NotificationRequest("integration@example.com", NotificationChannel.LOG, null, "hola integracion",
                null, null);

        final String responseBody = mockMvc
                .perform(post("/notifications")
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        final UUID id = UUID.fromString(objectMapper.readTree(responseBody).get("id").asText());
        assertThat(notificationRepository.findById(id)).isPresent();

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            final Notification persisted = notificationRepository.findById(id).orElseThrow();
            assertThat(persisted.getStatus()).isEqualTo(NotificationStatus.SENT);
            assertThat(persisted.getSentAt()).isNotNull();
        });
    }

    @Test
    void createNotification_withoutApiKey_returnsUnauthorizedAndDoesNotPersist() throws Exception {
        final NotificationRequest request = new NotificationRequest("integration@example.com", NotificationChannel.LOG, null, "hola", null, null);

        mockMvc
                .perform(post("/notifications").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        assertThat(notificationRepository.count()).isZero();
    }

    @Test
    void createNotification_withMissingRecipient_returnsBadRequestWithFieldErrorAndDoesNotPersist() throws Exception {
        final NotificationRequest request = new NotificationRequest(null, NotificationChannel.LOG, null, "hola", null, null);

        mockMvc
                .perform(post("/notifications")
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("recipient"));

        assertThat(notificationRepository.count()).isZero();
    }
}
