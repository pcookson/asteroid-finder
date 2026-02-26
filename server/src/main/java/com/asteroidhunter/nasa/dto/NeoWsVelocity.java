package com.asteroidhunter.nasa.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NeoWsVelocity(
        String kilometers_per_second,
        String kilometers_per_hour) {
}
