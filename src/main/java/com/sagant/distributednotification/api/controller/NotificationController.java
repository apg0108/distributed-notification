package com.sagant.distributednotification.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sagant.distributednotification.config.OpenApiConfig;
import com.sagant.distributednotification.domain.model.ErrorResponse;
import com.sagant.distributednotification.domain.model.NotificationRequest;
import com.sagant.distributednotification.domain.model.NotificationResponse;
import com.sagant.distributednotification.service.NotificationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.API_KEY_SECURITY_SCHEME)
public class NotificationController {

   private final NotificationService notificationService;

   @PostMapping
   @Operation(summary = "Crear una notificación", description = "Persiste la notificación con status PENDING y dispara su procesamiento asíncrono.")
   @ApiResponses({
         @ApiResponse(responseCode = "200", description = "Notificación creada", content = @Content(schema = @Schema(implementation = NotificationResponse.class))),
         @ApiResponse(responseCode = "400", description = "Validación fallida", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
         @ApiResponse(responseCode = "401", description = "Falta o es inválido el header X-API-KEY", content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })
   public ResponseEntity<NotificationResponse> createNotification(@Valid @RequestBody final NotificationRequest request) {
      return ResponseEntity.ok(notificationService.createNotification(request));
   }
}
