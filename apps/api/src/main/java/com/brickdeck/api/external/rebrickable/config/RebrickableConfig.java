package com.brickdeck.api.external.rebrickable.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(RebrickableProperties.class)
public class RebrickableConfig {

    @Bean
    public RestClient rebrickableRestClient(RebrickableProperties properties) {
        if (!StringUtils.hasText(properties.apiKey())) {
            throw new IllegalStateException(
                    "Missing REBRICKABLE_API_KEY. Set it as an environment variable before running the API."
            );
        }

        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "key " + properties.apiKey().trim())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}