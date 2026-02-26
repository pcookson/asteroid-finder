package com.asteroidhunter.neo.service;

import com.asteroidhunter.nasa.NeoWsClient;
import com.asteroidhunter.nasa.dto.NeoWsFeedResponse;
import com.asteroidhunter.neo.model.NeoSummary;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
public class NeoTodayService {

    private static final String NEOS_TODAY_CACHE = "neosToday";
    private static final Logger log = LoggerFactory.getLogger(NeoTodayService.class);

    private final NeoWsClient neoWsClient;
    private final NeoFeedNormalizer neoFeedNormalizer;
    private final ZoneId appZoneId;
    private final Clock appClock;
    private final CacheManager cacheManager;

    public NeoTodayService(
            NeoWsClient neoWsClient,
            NeoFeedNormalizer neoFeedNormalizer,
            ZoneId appZoneId,
            Clock appClock,
            CacheManager cacheManager) {
        this.neoWsClient = neoWsClient;
        this.neoFeedNormalizer = neoFeedNormalizer;
        this.appZoneId = appZoneId;
        this.appClock = appClock;
        this.cacheManager = cacheManager;
    }

    public List<NeoSummary> getTodayNeos() {
        LocalDate today = today();
        String cacheKey = cacheKeyToday();
        Cache cache = cacheManager.getCache(NEOS_TODAY_CACHE);

        if (cache != null) {
            List<NeoSummary> cached = cache.get(cacheKey, List.class);
            if (cached != null) {
                log.debug("Fetching NEOs from cache for {} ({})", today, appZoneId);
                return cached;
            }
        }

        log.debug("Fetching NEOs from NASA for {} ({})", today, appZoneId);
        NeoWsFeedResponse feed = neoWsClient.getFeed(today, today);
        List<NeoSummary> normalized = neoFeedNormalizer.normalizeForDate(feed, today);

        if (cache != null) {
            cache.put(cacheKey, normalized);
        }

        return normalized;
    }

    public String cacheKeyToday() {
        return today() + "|" + appZoneId;
    }

    private LocalDate today() {
        return LocalDate.now(appClock);
    }
}
