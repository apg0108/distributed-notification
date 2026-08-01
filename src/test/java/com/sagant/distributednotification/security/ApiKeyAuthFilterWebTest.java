package com.sagant.distributednotification.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.sagant.distributednotification.api.controller.NotificationController;
import com.sagant.distributednotification.config.SecurityConfig;
import com.sagant.distributednotification.config.security.ApiKeyAuthFilter;
import com.sagant.distributednotification.domain.model.NotificationChannel;
import com.sagant.distributednotification.domain.model.NotificationPriority;
import com.sagant.distributednotification.domain.model.NotificationResponse;
import com.sagant.distributednotification.domain.model.NotificationStatus;
import com.sagant.distributednotification.service.NotificationService;

@WebMvcTest(controllers = NotificationController.class)
@Import({ SecurityConfig.class, ApiKeyAuthFilter.class })
@TestPropertySource(properties = "notification.security.api-key=test-api-key")
class ApiKeyAuthFilterWebTest {

   private static final String VALID_REQUEST_BODY = """
         {"recipient":"user@example.com","channel":"LOG","body":"hello"}
         """;

   @Autowired
   private MockMvc mockMvc;

   @MockitoBean
   private NotificationService notificationService;

   @Test
   void rejectsRequestWithoutApiKeyHeader() throws Exception {
      mockMvc
            .perform(post("/notifications").contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST_BODY))
            .andExpect(status().isUnauthorized());
   }

   @Test
   void rejectsRequestWithWrongApiKey() throws Exception {
      mockMvc
            .perform(post("/notifications")
                  .header(ApiKeyAuthFilter.API_KEY_HEADER, "wrong-key")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(VALID_REQUEST_BODY))
            .andExpect(status().isUnauthorized());
   }

   @Test
   void acceptsRequestWithCorrectApiKey() throws Exception {
      final NotificationResponse response = new NotificationResponse(UUID.randomUUID(), "user@example.com", NotificationChannel.LOG, null, "hello",
            NotificationPriority.MEDIUM, Map.of(), NotificationStatus.PENDING);
      when(notificationService.createNotification(any())).thenReturn(response);

      mockMvc
            .perform(post("/notifications")
                  .header(ApiKeyAuthFilter.API_KEY_HEADER, "test-api-key")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(VALID_REQUEST_BODY))
            .andExpect(status().isOk());

      verify(notificationService).createNotification(any());
   }

   @Test
   void actuatorHealthBypassesTheApiKeyFilter() throws Exception {
      mockMvc.perform(get("/actuator/health")).andExpect(status().isNotFound());
   }
}
