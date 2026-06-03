package com.recruitment.backend.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class CacheManagementService {

    private final CacheManager cacheManager;

    public void evictAllMatchCaches() {
        evictCache("matchJobScore");
        evictCache("recommendJobs");
        evictCache("recommendCvs");
        log.info("Evicted all match caches");
    }

    public void evictCacheForCv(java.util.UUID cvId) {
        evictCache("recommendJobs");
        evictCache("recommendCvs");
        evictCache("matchJobScore");
        log.info("Evicted caches related to CV: {}", cvId);
    }

    public void evictCacheForJob(java.util.UUID jobId) {
        evictCache("recommendJobs");
        evictCache("recommendCvs");
        evictCache("matchJobScore");
        log.info("Evicted caches related to Job: {}", jobId);
    }

    public void evictOperationsCaches() {
        evictCache("operationsOverview");
        evictCache("scoreSummary");
        log.info("Evicted operations caches");
    }

    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        List<String> cacheNames = new ArrayList<>(cacheManager.getCacheNames());
        stats.put("cacheNames", cacheNames);
        stats.put("totalCaches", cacheNames.size());
        return stats;
    }

    private void evictCache(String cacheName) {
        var cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }
}
