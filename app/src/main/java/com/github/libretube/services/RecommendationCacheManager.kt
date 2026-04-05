import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class RecommendationCacheManager {
    private final Map<String, CachedRecommendation> cache = new HashMap<>();
    private static final long EXPIRY_DURATION = 30 * 60 * 1000; // 30 minutes

    public void addRecommendation(String key, String recommendation) {
        cache.put(key, new CachedRecommendation(recommendation, Instant.now().toEpochMilli()));
    }

    public String getRecommendation(String key) {
        CachedRecommendation cached = cache.get(key);
        if (cached == null || isExpired(cached)) {
            cache.remove(key);
            return null; // or fetch a new recommendation
        }
        return cached.recommendation;
    }

    public void invalidateCache(String key) {
        cache.remove(key);
    }

    private boolean isExpired(CachedRecommendation cached) {
        return Instant.now().toEpochMilli() - cached.timestamp > EXPIRY_DURATION;
    }

    private static class CachedRecommendation {
        String recommendation;
        long timestamp;

        CachedRecommendation(String recommendation, long timestamp) {
            this.recommendation = recommendation;
            this.timestamp = timestamp;
        }
    }
}