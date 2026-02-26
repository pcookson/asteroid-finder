package com.asteroidhunter.nasa.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NeoWsDiameterRange(
        double estimated_diameter_min,
        double estimated_diameter_max) {
}
