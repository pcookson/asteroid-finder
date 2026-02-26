package com.asteroidhunter.nasa.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NeoWsMissDistance(
        String astronomical,
        String lunar,
        String kilometers) {
}
