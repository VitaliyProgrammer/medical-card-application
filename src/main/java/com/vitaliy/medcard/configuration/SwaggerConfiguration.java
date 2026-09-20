package com.vitaliy.medcard.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfiguration {

    private static final String BEARER_SCHEME_NAME = "bearerAuth";

    static {
        SpringDocUtils.getConfig().replaceWithSchema(LocalDate.class,
                new StringSchema().example("20-09-2026").description("Format: dd-MM-yyyy"));
        SpringDocUtils.getConfig().replaceWithSchema(LocalDateTime.class,
                new StringSchema().example("20-09-2026, 15:23")
                        .description("Format: dd-MM-yyyy, HH:mm"));
    }

    @Bean
    public OpenAPI medCardOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Medical Card API")
                        .description("Personal health record service for patients and doctors")
                        .version("v0.1"))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME_NAME))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME_NAME,
                        new SecurityScheme()
                                .name(BEARER_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
