package com.brickdeck.api.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI brickDeckOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("BrickDeck Catalog API")
                        .description("LEGO collection intelligence platform - catalog, sets, themes, and set inventory.")
                        .version("v1"));
    }
}
