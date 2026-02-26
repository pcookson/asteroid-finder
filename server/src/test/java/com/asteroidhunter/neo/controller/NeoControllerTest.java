package com.asteroidhunter.neo.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.asteroidhunter.common.ApiExceptionHandler;
import com.asteroidhunter.nasa.NeoWsException;
import com.asteroidhunter.neo.model.NeoSummary;
import com.asteroidhunter.neo.service.NeoTodayService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(NeoController.class)
@Import(ApiExceptionHandler.class)
class NeoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NeoTodayService neoTodayService;

    @Test
    void getTodayNeosReturnsJsonArray() throws Exception {
        given(neoTodayService.getTodayNeos()).willReturn(List.of(
                new NeoSummary(
                        "123",
                        "Test Asteroid",
                        true,
                        1.2,
                        3.4,
                        Instant.parse("2026-02-26T12:00:00Z"),
                        "Earth",
                        45678.9,
                        0.12,
                        17.5)));

        mockMvc.perform(get("/api/neos/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value("123"))
                .andExpect(jsonPath("$[0].name").value("Test Asteroid"))
                .andExpect(jsonPath("$[0].isHazardous").value(true))
                .andExpect(jsonPath("$[0].orbitingBody").value("Earth"))
                .andExpect(jsonPath("$[0].relativeVelocityKmPerSec").value(17.5));
    }

    @Test
    void getTodayNeosMapsNeoWsExceptionTo502() throws Exception {
        given(neoTodayService.getTodayNeos()).willThrow(new NeoWsException(429, "rate limited"));

        mockMvc.perform(get("/api/neos/today"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("NASA_NEO_WS_ERROR"))
                .andExpect(jsonPath("$.message").value("NASA NeoWs request failed"))
                .andExpect(jsonPath("$.status").value(429));
    }

    @Test
    void getTodayNeosMapsMissingApiKeyTo500() throws Exception {
        given(neoTodayService.getTodayNeos()).willThrow(
                new IllegalStateException("NASA_API_KEY is not configured"));

        mockMvc.perform(get("/api/neos/today"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("CONFIG_ERROR"))
                .andExpect(jsonPath("$.message").value("NASA_API_KEY is not configured"));
    }
}
