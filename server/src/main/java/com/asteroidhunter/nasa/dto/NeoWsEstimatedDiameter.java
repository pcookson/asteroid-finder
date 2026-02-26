package com.asteroidhunter.nasa.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NeoWsEstimatedDiameter(NeoWsDiameterRange meters) {
}
