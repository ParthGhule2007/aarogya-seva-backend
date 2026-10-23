package com.ruralhealthcare.medicine.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalTime;

@Configuration
public class OpenApiConfig {

    static {
        SpringDocUtils.getConfig().replaceWithClass(LocalTime.class, String.class);
    }

    @Bean
    public OpenAPI medicineOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Aarogya Seva - Medicine Reminder Service")
                        .description("Microservice for managing rural patient medication reminders, visual photo references, dose scheduling, and consecutive missed-dose escalation.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Aarogya Seva Health Platform")
                                .email("contact@aarogyaseva.org")));
    }
}
