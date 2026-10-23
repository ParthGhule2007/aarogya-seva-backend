package com.ruralhealthcare.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ruralHealthcareOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Rural Healthcare Platform API")
                        .description("Backend-first Modular Monolith for rural healthcare screening, facility discovery, and referrals.")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Rural Health Engineering Team")
                                .email("dev@ruralhealth.org"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}
