package com.asteroidhunter.neo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.asteroidhunter.nasa.dto.NeoWsCloseApproachData;
import com.asteroidhunter.nasa.dto.NeoWsDiameterRange;
import com.asteroidhunter.nasa.dto.NeoWsEstimatedDiameter;
import com.asteroidhunter.nasa.dto.NeoWsFeedResponse;
import com.asteroidhunter.nasa.dto.NeoWsMissDistance;
import com.asteroidhunter.nasa.dto.NeoWsNeoObject;
import com.asteroidhunter.nasa.dto.NeoWsVelocity;
import com.asteroidhunter.neo.model.NeoSummary;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NeoFeedNormalizerTest {

    private final NeoFeedNormalizer normalizer = new NeoFeedNormalizer();

    @Test
    void normalizeForDateReturnsEmptyListWhenDateKeyMissing() {
        NeoWsFeedResponse feed = new NeoWsFeedResponse(null, 0, Map.of());

        List<NeoSummary> result = normalizer.normalizeForDate(feed, LocalDate.parse("2026-02-01"));

        assertTrue(result.isEmpty());
    }

    @Test
    void normalizeForDateChoosesMatchingCloseApproachDate() {
        LocalDate date = LocalDate.parse("2026-02-01");
        NeoWsNeoObject neo = neoObject(
                "neo-1",
                List.of(
                        closeApproach("2026-01-31", 1000L, "1000", "1.0", "10.0", "Mars"),
                        closeApproach("2026-02-01", 2000L, "900", "2.0", "11.0", "Earth")));
        NeoWsFeedResponse feed = feedForDate(date, List.of(neo));

        List<NeoSummary> result = normalizer.normalizeForDate(feed, date);

        assertEquals(1, result.size());
        NeoSummary summary = result.getFirst();
        assertEquals(Instant.ofEpochMilli(2000L), summary.closeApproachTime());
        assertEquals(900.0, summary.missDistanceKm());
        assertEquals("Earth", summary.orbitingBody());
    }

    @Test
    void normalizeForDateBreaksTieBySmallestMissDistanceForMatchingDate() {
        LocalDate date = LocalDate.parse("2026-02-01");
        NeoWsNeoObject neo = neoObject(
                "neo-2",
                List.of(
                        closeApproach("2026-02-01", 5000L, "2500", "6.0", "22.0", "Earth"),
                        closeApproach("2026-02-01", 4000L, "1200", "3.0", "20.0", "Earth")));
        NeoWsFeedResponse feed = feedForDate(date, List.of(neo));

        List<NeoSummary> result = normalizer.normalizeForDate(feed, date);

        assertEquals(1, result.size());
        NeoSummary summary = result.getFirst();
        assertEquals(1200.0, summary.missDistanceKm());
        assertEquals(3.0, summary.missDistanceLunar());
        assertEquals(20.0, summary.relativeVelocityKmPerSec());
        assertEquals(Instant.ofEpochMilli(4000L), summary.closeApproachTime());
    }

    @Test
    void normalizeForDateSortsByCloseApproachTimeThenMissDistance() {
        LocalDate date = LocalDate.parse("2026-02-01");

        NeoWsNeoObject later = neoObject(
                "neo-later",
                List.of(closeApproach("2026-02-01", 3000L, "5000", "10.0", "30.0", "Earth")));
        NeoWsNeoObject earlierFarther = neoObject(
                "neo-earlier-farther",
                List.of(closeApproach("2026-02-01", 1000L, "9000", "20.0", "10.0", "Earth")));
        NeoWsNeoObject earlierCloser = neoObject(
                "neo-earlier-closer",
                List.of(closeApproach("2026-02-01", 1000L, "1000", "2.0", "11.0", "Earth")));

        NeoWsFeedResponse feed = feedForDate(date, List.of(later, earlierFarther, earlierCloser));

        List<NeoSummary> result = normalizer.normalizeForDate(feed, date);

        assertEquals(3, result.size());
        assertEquals("neo-earlier-closer", result.get(0).id());
        assertEquals("neo-earlier-farther", result.get(1).id());
        assertEquals("neo-later", result.get(2).id());
    }

    @Test
    void normalizeForDateHandlesMissingNestedDataWithoutCrashing() {
        LocalDate date = LocalDate.parse("2026-02-01");
        NeoWsNeoObject neo = new NeoWsNeoObject(
                "neo-missing",
                "Missing Fields",
                null,
                null,
                false,
                null,
                List.of(new NeoWsCloseApproachData(
                        "2026-02-01",
                        null,
                        null,
                        null,
                        null,
                        null)));
        NeoWsFeedResponse feed = feedForDate(date, List.of(neo));

        List<NeoSummary> result = normalizer.normalizeForDate(feed, date);

        assertEquals(1, result.size());
        NeoSummary summary = result.getFirst();
        assertNotNull(summary.closeApproachTime());
        assertEquals("Earth", summary.orbitingBody());
        assertTrue(Double.isNaN(summary.missDistanceKm()));
        assertTrue(Double.isNaN(summary.relativeVelocityKmPerSec()));
    }

    private NeoWsFeedResponse feedForDate(LocalDate date, List<NeoWsNeoObject> objects) {
        return new NeoWsFeedResponse(null, objects.size(), Map.of(date.toString(), objects));
    }

    private NeoWsNeoObject neoObject(String id, List<NeoWsCloseApproachData> approaches) {
        return new NeoWsNeoObject(
                id,
                "Asteroid " + id,
                null,
                null,
                true,
                new NeoWsEstimatedDiameter(new NeoWsDiameterRange(12.5, 20.5)),
                approaches);
    }

    private NeoWsCloseApproachData closeApproach(
            String date,
            Long epochMillis,
            String missDistanceKm,
            String missDistanceLunar,
            String velocityKmPerSec,
            String orbitingBody) {
        return new NeoWsCloseApproachData(
                date,
                null,
                epochMillis,
                new NeoWsVelocity(velocityKmPerSec, null),
                new NeoWsMissDistance(null, missDistanceLunar, missDistanceKm),
                orbitingBody);
    }
}
