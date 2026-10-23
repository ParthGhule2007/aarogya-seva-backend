package com.example.consultationservice.config;

import feign.Client;
import feign.RequestInterceptor;
import feign.hc5.ApacheHttp5Client;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {

    @Bean
    public Client feignClient() {
        return new ApacheHttp5Client();
    }

    @Bean
    public RequestInterceptor serviceSourceHeaderInterceptor() {
        return requestTemplate -> {
            if (!requestTemplate.headers().containsKey("X-Service-Source")) {
                requestTemplate.header("X-Service-Source", "Consultation-Service");
            }
        };
    }
}
