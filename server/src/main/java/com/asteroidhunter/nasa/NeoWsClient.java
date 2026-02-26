package com.asteroidhunter.nasa;

import com.asteroidhunter.nasa.dto.NeoWsFeedResponse;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class NeoWsClient {

    private static final int ERROR_BODY_SNIPPET_MAX_LENGTH = 300;

    private final WebClient neoWsWebClient;
    private final String nasaApiKey;

    public NeoWsClient(
            WebClient neoWsWebClient,
            @Value("${nasa.apiKey:${NASA_API_KEY:}}") String nasaApiKey) {
        this.neoWsWebClient = neoWsWebClient;
        this.nasaApiKey = nasaApiKey == null ? "" : nasaApiKey.trim();
    }

    public NeoWsFeedResponse getFeed(LocalDate startDate, LocalDate endDate) {
        if (nasaApiKey.isBlank()) {
            throw new IllegalStateException(
                    "NASA_API_KEY is not configured. Set env var NASA_API_KEY or property nasa.apiKey.");
        }

        return neoWsWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/neo/rest/v1/feed")
                        .queryParam("start_date", startDate)
                        .queryParam("end_date", endDate)
                        .queryParam("api_key", nasaApiKey)
                        .build())
                .exchangeToMono(response -> mapResponse(response.statusCode(), response))
                .block();
    }

    private Mono<NeoWsFeedResponse> mapResponse(
            HttpStatusCode statusCode,
            org.springframework.web.reactive.function.client.ClientResponse response) {
        if (statusCode.is2xxSuccessful()) {
            return response.bodyToMono(NeoWsFeedResponse.class);
        }

        return response.bodyToMono(String.class)
                .defaultIfEmpty("")
                .flatMap(body -> Mono.error(new NeoWsException(
                        statusCode.value(),
                        summarizeBody(body))));
    }

    private String summarizeBody(String body) {
        String normalized = body == null ? "" : body.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= ERROR_BODY_SNIPPET_MAX_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, ERROR_BODY_SNIPPET_MAX_LENGTH) + "...";
    }
}
