package br.com.vivabem50.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/** Documentação interativa em /swagger-ui.html e contrato OpenAPI em /v3/api-docs. */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "VivaBem 50+ API",
                version = "v1",
                description = "API do VivaBem 50+. Faça login, clique em Authorize e cole o token."),
        security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}
