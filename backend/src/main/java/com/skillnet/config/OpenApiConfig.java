package com.skillnet.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI skillnetOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Skillnet API")
                        .description("REST API for Skillnet platform — users, courses, enrollments, lessons and payments.")
                        .version("v1")
                        .contact(new Contact().name("Skillnet").email("info@skillnet.local")));
    }
}
