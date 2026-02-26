package com.asteroidhunter.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppTimeZoneConfig {

    @Bean
    public ZoneId appZoneId(@Value("${app.timezone:America/Toronto}") String timeZoneId) {
        return ZoneId.of(timeZoneId);
    }

    @Bean
    public Clock appClock(ZoneId appZoneId) {
        return Clock.system(appZoneId);
    }
}
