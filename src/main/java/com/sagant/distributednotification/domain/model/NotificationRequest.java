package com.sagant.distributednotification.domain.model;

import java.util.Map;

import com.sagant.distributednotification.api.validation.ValidRecipientForChannel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@ValidRecipientForChannel
public record NotificationRequest(@NotBlank(message = "recipient is required") String recipient,

                                  @NotNull(message = "channel is required") NotificationChannel channel,

                                  @Size(max = 255, message = "subject must be at most 255 characters") String subject,

                                  @NotBlank(message = "body is required") String body,

                                  NotificationPriority priority,

                                  Map<String, String> metadata) {

}
