package com.sagant.distributednotification.api.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sagant.distributednotification.domain.model.NotificationChannel;
import com.sagant.distributednotification.domain.model.NotificationPriority;
import com.sagant.distributednotification.domain.model.NotificationRequest;
import com.sagant.distributednotification.domain.model.NotificationResponse;
import com.sagant.distributednotification.domain.model.NotificationStatus;
import com.sagant.distributednotification.service.NotificationService;

@WebMvcTest(controllers = NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private NotificationService notificationService;

    @Test
    void createNotification_withValidRequest_returnsOkWithServiceResponse() throws Exception {
        final NotificationRequest request = new NotificationRequest(
                "user@example.com", NotificationChannel.LOG, "subject", "hello",
                NotificationPriority.HIGH, Map.of("k", "v"));
        final NotificationResponse response = new NotificationResponse(
                UUID.randomUUID(), "user@example.com", NotificationChannel.LOG, "subject", "hello",
                NotificationPriority.HIGH, Map.of("k", "v"), NotificationStatus.PENDING, 0, null,
                Instant.now(), Instant.now(), null);
        when(notificationService.createNotification(request)).thenReturn(response);

        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(response.id().toString()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.recipient").value("user@example.com"));

        verify(notificationService).createNotification(request);
    }

    @Test
    void createNotification_missingRecipient_returns400WithFieldError() throws Exception {
        final NotificationRequest request = new NotificationRequest(
                null, NotificationChannel.LOG, null, "hello", null, null);

        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("recipient"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("recipient is required"));
    }

    @Test
    void createNotification_missingChannel_returns400WithFieldError() throws Exception {
        final NotificationRequest request = new NotificationRequest(
                "user@example.com", null, null, "hello", null, null);

        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("channel"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("channel is required"));
    }

    @Test
    void createNotification_missingBody_returns400WithFieldError() throws Exception {
        final NotificationRequest request = new NotificationRequest(
                "user@example.com", NotificationChannel.LOG, null, null, null, null);

        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("body"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("body is required"));
    }

    @Test
    void createNotification_subjectTooLong_returns400WithFieldError() throws Exception {
        final String tooLongSubject = "x".repeat(256);
        final NotificationRequest request = new NotificationRequest(
                "user@example.com", NotificationChannel.LOG, tooLongSubject, "hello", null, null);

        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("subject"));
    }

    @Test
    void createNotification_emailChannelWithInvalidRecipientFormat_returns400WithFieldError() throws Exception {
        final NotificationRequest request = new NotificationRequest(
                "not-an-email", NotificationChannel.EMAIL, null, "hello", null, null);

        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("recipient"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("recipient must be a valid email address when channel is EMAIL"));
    }

    @Test
    void createNotification_nonEmailChannelWithNonEmailRecipient_returnsOk() throws Exception {
        final NotificationRequest request = new NotificationRequest(
                "not-an-email", NotificationChannel.LOG, null, "hello", null, null);
        final NotificationResponse response = new NotificationResponse(
                UUID.randomUUID(), "not-an-email", NotificationChannel.LOG, null, "hello",
                NotificationPriority.MEDIUM, null, NotificationStatus.PENDING, 0, null,
                Instant.now(), Instant.now(), null);
        when(notificationService.createNotification(request)).thenReturn(response);

        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void createNotification_withoutOptionalFields_stillSucceeds() throws Exception {
        final NotificationRequest request = new NotificationRequest(
                "user@example.com", NotificationChannel.EMAIL, null, "hello", null, null);
        final NotificationResponse response = new NotificationResponse(
                UUID.randomUUID(), "user@example.com", NotificationChannel.EMAIL, null, "hello",
                NotificationPriority.MEDIUM, null, NotificationStatus.PENDING, 0, null,
                Instant.now(), Instant.now(), null);
        when(notificationService.createNotification(request)).thenReturn(response);

        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}
