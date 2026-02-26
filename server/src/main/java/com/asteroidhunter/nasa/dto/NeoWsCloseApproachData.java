package com.asteroidhunter.nasa.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NeoWsCloseApproachData(
        String close_approach_date,
        String close_approach_date_full,
        Long epoch_date_close_approach,
        NeoWsVelocity relative_velocity,
        NeoWsMissDistance miss_distance,
        String orbiting_body) {
}
