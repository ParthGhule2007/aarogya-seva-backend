package com.ruralhealthcare.sos.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI documentation configuration for SOS Emergency Alert Service.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Aarogya Seva - SOS Emergency Alert Service")
                        .version("1.0.0")
                        .description("High-availability, independent microservice for rural emergency SOS alerts, " +
                                "multi-target recipient routing (Caregiver, Facility, Nearby People), and FCM push dispatch.")
                        .contact(new Contact()
                                .name("Aarogya Seva Rural Health Engineering")
                                .email("engineering@aarogyaseva.org"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}
