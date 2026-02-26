package com.asteroidhunter.neo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.asteroidhunter.nasa.NeoWsClient;
import com.asteroidhunter.nasa.dto.NeoWsCloseApproachData;
import com.asteroidhunter.nasa.dto.NeoWsDiameterRange;
import com.asteroidhunter.nasa.dto.NeoWsEstimatedDiameter;
import com.asteroidhunter.nasa.dto.NeoWsFeedResponse;
import com.asteroidhunter.nasa.dto.NeoWsMissDistance;
import com.asteroidhunter.nasa.dto.NeoWsNeoObject;
import com.asteroidhunter.nasa.dto.NeoWsVelocity;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@SpringBootTest(properties = "app.timezone=America/Toronto")
class NeoTodayServiceCachingTest {

    private static final ZoneId TORONTO = ZoneId.of("America/Toronto");
    private static final Instant FIXED_INSTANT = Instant.parse("2026-02-26T12:00:00Z");

    @Autowired
    private NeoTodayService neoTodayService;

    @Autowired
    private CacheManager cacheManager;

    @MockBean
    private NeoWsClient neoWsClient;

    @BeforeEach
    void clearCache() {
        var cache = cacheManager.getCache("neosToday");
        if (cache != null) {
            cache.clear();
        }
    }

    @Test
    void getTodayNeosUsesCacheAndCallsNasaOnlyOnce() {
        LocalDate today = LocalDate.now(Clock.fixed(FIXED_INSTANT, TORONTO));
        NeoWsFeedResponse feed = new NeoWsFeedResponse(
                null,
                1,
                Map.of(today.toString(), List.of(new NeoWsNeoObject(
                        "123",
                        "Cached Asteroid",
                        null,
                        null,
                        false,
                        new NeoWsEstimatedDiameter(new NeoWsDiameterRange(1.0, 2.0)),
                        List.of(new NeoWsCloseApproachData(
                                today.toString(),
                                null,
                                1_000L,
                                new NeoWsVelocity("12.5", null),
                                new NeoWsMissDistance(null, "0.5", "192200"),
                                "Earth"))))));

        given(neoWsClient.getFeed(eq(today), eq(today))).willReturn(feed);

        var first = neoTodayService.getTodayNeos();
        var second = neoTodayService.getTodayNeos();

        assertEquals(1, first.size());
        assertEquals(first, second);
        verify(neoWsClient, times(1)).getFeed(eq(today), eq(today));
    }

    @TestConfiguration
    static class FixedClockTestConfig {
        @Bean
        @Primary
        Clock testClock() {
            return Clock.fixed(FIXED_INSTANT, TORONTO);
        }
    }
}
