package com.example.btms.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

/**
 * Cấu hình OpenAPI / Swagger cho BTMS
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("BTMS - Badminton Tournament Management System API")
                        .description("API Documentation cho hệ thống quản lý giải đấu cầu lông")
                        .version("1.5.0")
                        .contact(new Contact()
                                .name("BTMS Development Team")
                                .email("support@btms.local")
                                .url("https://github.com/NguyenHau-IT/BTMS-OVR"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(Arrays.asList(
                        new Server()
                                .url("http://localhost:2345")
                                .description("Local Development Server"),
                        new Server()
                                .url("http://0.0.0.0:2345")
                                .description("Network Server")));
    }
}
