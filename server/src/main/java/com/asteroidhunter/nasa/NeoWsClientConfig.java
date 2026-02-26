package com.asteroidhunter.nasa;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class NeoWsClientConfig {

    @Bean
    public WebClient neoWsWebClient(
            WebClient.Builder webClientBuilder,
            @Value("${NASA_NEO_BASE_URL:https://api.nasa.gov}") String baseUrl) {
        return webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
