package com.tikzy.common.email;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(BrevoProperties.class)
public class BrevoEmailConfig {

    @Bean
    public RestClient brevoRestClient(RestClient.Builder builder, BrevoProperties properties) {
        return builder.baseUrl(properties.apiUrl()).build();
    }
}
