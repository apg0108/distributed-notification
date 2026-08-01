package com.sagant.distributednotification.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

@Configuration
@OpenAPIDefinition(info = @Info(title = "Distributed Notification API", version = "v1",
      description = "Servicio de notificaciones distribuido — prueba técnica Java Senior (Sagant)."))
@SecurityScheme(name = OpenApiConfig.API_KEY_SECURITY_SCHEME, type = SecuritySchemeType.APIKEY, in = SecuritySchemeIn.HEADER,
      paramName = "X-API-KEY")
public class OpenApiConfig {

   public static final String API_KEY_SECURITY_SCHEME = "ApiKeyAuth";
}
