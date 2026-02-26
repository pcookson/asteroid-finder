package com.asteroidhunter.neo.model;

import java.time.Instant;

public record NeoSummary(
        String id,
        String name,
        boolean isHazardous,
        double diameterMinMeters,
        double diameterMaxMeters,
        Instant closeApproachTime,
        String orbitingBody,
        double missDistanceKm,
        double missDistanceLunar,
        double relativeVelocityKmPerSec) {
}
