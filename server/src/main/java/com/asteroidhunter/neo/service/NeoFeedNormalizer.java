package com.asteroidhunter.neo.service;

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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class NeoFeedNormalizer {

    private static final double LUNAR_DISTANCE_KM = 384400.0;
    private static final DateTimeFormatter NASA_CLOSE_APPROACH_DATE_TIME_FORMAT =
            new DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .appendPattern("yyyy-MMM-dd HH:mm")
                    .optionalStart()
                    .appendLiteral(':')
                    .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
                    .optionalEnd()
                    .toFormatter(Locale.US);

    public List<NeoSummary> normalizeForDate(NeoWsFeedResponse feed, LocalDate date) {
        if (feed == null || date == null) {
            return List.of();
        }

        Map<String, List<NeoWsNeoObject>> grouped = feed.near_earth_objects();
        if (grouped == null) {
            return List.of();
        }

        List<NeoWsNeoObject> objectsForDate = grouped.get(date.toString());
        if (objectsForDate == null || objectsForDate.isEmpty()) {
            return List.of();
        }

        List<NeoSummary> summaries = new ArrayList<>();
        for (NeoWsNeoObject neoObject : objectsForDate) {
            if (neoObject == null) {
                continue;
            }
            summaries.add(toSummary(neoObject, date));
        }

        summaries.sort(Comparator
                .comparing(NeoSummary::closeApproachTime)
                .thenComparingDouble(NeoSummary::missDistanceKm));
        return List.copyOf(summaries);
    }

    private NeoSummary toSummary(NeoWsNeoObject neoObject, LocalDate date) {
        NeoWsDiameterRange metersRange = extractMetersRange(neoObject.estimated_diameter());
        NeoWsCloseApproachData bestApproach = chooseBestCloseApproach(neoObject.close_approach_data(), date);

        double missDistanceKm = parseDoubleOrNaN(bestApproach == null ? null : kilometers(bestApproach.miss_distance()));
        double missDistanceLunar = parseDoubleOrNaN(bestApproach == null ? null : lunar(bestApproach.miss_distance()));
        if (Double.isNaN(missDistanceLunar) && !Double.isNaN(missDistanceKm)) {
            missDistanceLunar = missDistanceKm / LUNAR_DISTANCE_KM;
        }

        double relativeVelocityKmPerSec =
                parseDoubleOrNaN(bestApproach == null ? null : kmPerSec(bestApproach.relative_velocity()));

        return new NeoSummary(
                safeString(neoObject.id()),
                safeString(neoObject.name()),
                neoObject.is_potentially_hazardous_asteroid(),
                metersRange == null ? Double.NaN : metersRange.estimated_diameter_min(),
                metersRange == null ? Double.NaN : metersRange.estimated_diameter_max(),
                resolveCloseApproachTime(bestApproach, date),
                defaultIfBlank(bestApproach == null ? null : bestApproach.orbiting_body(), "Earth"),
                missDistanceKm,
                missDistanceLunar,
                relativeVelocityKmPerSec);
    }

    private NeoWsDiameterRange extractMetersRange(NeoWsEstimatedDiameter estimatedDiameter) {
        if (estimatedDiameter == null) {
            return null;
        }
        return estimatedDiameter.meters();
    }

    private NeoWsCloseApproachData chooseBestCloseApproach(List<NeoWsCloseApproachData> closeApproachData, LocalDate date) {
        if (closeApproachData == null || closeApproachData.isEmpty()) {
            return null;
        }

        String dateKey = date.toString();
        NeoWsCloseApproachData bestMatching = null;
        double bestMatchingMissKm = Double.NaN;

        for (NeoWsCloseApproachData item : closeApproachData) {
            if (item == null || !Objects.equals(item.close_approach_date(), dateKey)) {
                continue;
            }
            double missKm = parseDoubleOrNaN(kilometers(item.miss_distance()));
            if (bestMatching == null || compareMissDistance(missKm, bestMatchingMissKm) < 0) {
                bestMatching = item;
                bestMatchingMissKm = missKm;
            }
        }
        if (bestMatching != null) {
            return bestMatching;
        }

        NeoWsCloseApproachData first = null;
        NeoWsCloseApproachData soonestWithEpoch = null;
        Long soonestEpoch = null;

        for (NeoWsCloseApproachData item : closeApproachData) {
            if (item == null) {
                continue;
            }
            if (first == null) {
                first = item;
            }
            Long epoch = item.epoch_date_close_approach();
            if (epoch != null && (soonestEpoch == null || epoch < soonestEpoch)) {
                soonestEpoch = epoch;
                soonestWithEpoch = item;
            }
        }

        return soonestWithEpoch != null ? soonestWithEpoch : first;
    }

    private int compareMissDistance(double left, double right) {
        if (Double.isNaN(left) && Double.isNaN(right)) {
            return 0;
        }
        if (Double.isNaN(left)) {
            return 1;
        }
        if (Double.isNaN(right)) {
            return -1;
        }
        return Double.compare(left, right);
    }

    private Instant resolveCloseApproachTime(NeoWsCloseApproachData approach, LocalDate fallbackDate) {
        if (approach != null && approach.epoch_date_close_approach() != null) {
            return Instant.ofEpochMilli(approach.epoch_date_close_approach());
        }

        if (approach != null) {
            Instant parsed = tryParseCloseApproachDateFull(approach.close_approach_date_full());
            if (parsed != null) {
                return parsed;
            }
        }

        return fallbackDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
    }

    private Instant tryParseCloseApproachDateFull(String closeApproachDateFull) {
        if (closeApproachDateFull == null || closeApproachDateFull.isBlank()) {
            return null;
        }

        try {
            LocalDateTime dateTime = LocalDateTime.parse(closeApproachDateFull, NASA_CLOSE_APPROACH_DATE_TIME_FORMAT);
            return dateTime.atZone(ZoneId.systemDefault()).toInstant();
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    // Parsing failures keep the item and mark the numeric field as NaN rather than dropping data.
    private double parseDoubleOrNaN(String value) {
        if (value == null || value.isBlank()) {
            return Double.NaN;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException ignored) {
            return Double.NaN;
        }
    }

    private String safeString(String value) {
        return value == null ? "" : value;
    }

    private String defaultIfBlank(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }

    private String kilometers(NeoWsMissDistance missDistance) {
        return missDistance == null ? null : missDistance.kilometers();
    }

    private String lunar(NeoWsMissDistance missDistance) {
        return missDistance == null ? null : missDistance.lunar();
    }

    private String kmPerSec(NeoWsVelocity velocity) {
        return velocity == null ? null : velocity.kilometers_per_second();
    }
}
