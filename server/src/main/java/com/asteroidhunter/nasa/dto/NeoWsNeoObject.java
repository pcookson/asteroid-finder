package com.asteroidhunter.nasa.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NeoWsNeoObject(
        String id,
        String name,
        String nasa_jpl_url,
        Double absolute_magnitude_h,
        boolean is_potentially_hazardous_asteroid,
        NeoWsEstimatedDiameter estimated_diameter,
        List<NeoWsCloseApproachData> close_approach_data) {
}
