package com.asteroidhunter.nasa;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

class NeoWsClientTest {

    @Test
    void getFeedParsesTypedResponse() {
        String body = """
                {
                  "element_count": 1,
                  "near_earth_objects": {
                    "2026-02-01": [
                      {
                        "id": "123",
                        "name": "Test Asteroid",
                        "is_potentially_hazardous_asteroid": false,
                        "estimated_diameter": {
                          "meters": {
                            "estimated_diameter_min": 1.23,
                            "estimated_diameter_max": 4.56
                          }
                        },
                        "close_approach_data": [
                          {
                            "close_approach_date": "2026-02-01",
                            "relative_velocity": {
                              "kilometers_per_second": "12.34"
                            },
                            "miss_distance": {
                              "lunar": "5.67",
                              "kilometers": "12345.6"
                            },
                            "orbiting_body": "Earth"
                          }
                        ]
                      }
                    ]
                  }
                }
                """;

        WebClient webClient = WebClient.builder()
                .baseUrl("https://api.nasa.gov")
                .exchangeFunction(request -> Mono.just(
                        ClientResponse.create(HttpStatus.OK)
                                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                .body(body)
                                .build()))
                .build();

        NeoWsClient client = new NeoWsClient(webClient, "demo-key");
        var response = client.getFeed(LocalDate.parse("2026-02-01"), LocalDate.parse("2026-02-02"));

        assertEquals(1, response.element_count());
        assertNotNull(response.near_earth_objects());
        assertEquals(1, response.near_earth_objects().get("2026-02-01").size());
        assertEquals("123", response.near_earth_objects().get("2026-02-01").getFirst().id());
    }

    @Test
    void getFeedThrowsClearMessageWhenApiKeyMissing() {
        NeoWsClient client = new NeoWsClient(WebClient.builder().baseUrl("http://localhost").build(), "");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> client.getFeed(LocalDate.parse("2026-02-01"), LocalDate.parse("2026-02-02")));

        assertTrue(exception.getMessage().contains("NASA_API_KEY"));
    }

    @Test
    void getFeedThrowsNeoWsExceptionOnNon2xx() {
        WebClient webClient = WebClient.builder()
                .baseUrl("https://api.nasa.gov")
                .exchangeFunction(request -> Mono.just(
                        ClientResponse.create(HttpStatus.UNAUTHORIZED)
                                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                .body("{\"error\":\"bad api key\"}")
                                .build()))
                .build();

        NeoWsClient client = new NeoWsClient(webClient, "bad-key");

        NeoWsException exception = assertThrows(
                NeoWsException.class,
                () -> client.getFeed(LocalDate.parse("2026-02-01"), LocalDate.parse("2026-02-02")));

        assertEquals(401, exception.getStatus());
        assertTrue(exception.getBodySnippet().contains("bad api key"));
    }
}
