package com.devflow.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    static final String BEARER_SCHEME = "bearerAuth";
    static final String API_KEY_SCHEME = "apiKeyAuth";

    @Bean
    public OpenAPI devFlowOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("DevFlow API")
                        .version("v1")
                        .description("""
                                Project tracking and deployment history for software teams.

                                Most endpoints require a bearer access token obtained from `/api/auth/login`.
                                CI endpoints under `/api/deployments` accept a scoped API key in the
                                `X-DevFlow-Api-Key` header instead, so a build agent never needs user credentials.
                                """)
                        .contact(new Contact().name("DevFlow"))
                        .license(new License().name("MIT")))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Access token from /api/auth/login"))
                        .addSecuritySchemes(API_KEY_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-DevFlow-Api-Key")
                                .description("Project-scoped key used by CI pipelines")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
