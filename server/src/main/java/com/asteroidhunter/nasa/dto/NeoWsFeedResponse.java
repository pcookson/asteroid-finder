package com.asteroidhunter.nasa.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NeoWsFeedResponse(
        Map<String, Object> links,
        int element_count,
        Map<String, List<NeoWsNeoObject>> near_earth_objects) {
}
