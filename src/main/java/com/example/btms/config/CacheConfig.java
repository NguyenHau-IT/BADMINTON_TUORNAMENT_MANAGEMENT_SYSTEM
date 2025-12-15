package com.example.btms.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cache Configuration cho Web Platform
 * 
 * Sử dụng Spring Cache để tối ưu performance:
 * - Cache featured tournaments (ít thay đổi, truy vấn nhiều)
 * - Cache tournament stats (tính toán phức tạp)
 * - Cache search results (query nặng)
 * 
 * Cache Provider: ConcurrentMapCache (in-memory, simple)
 * - Tốt cho development và small-scale deployment
 * - Production có thể upgrade lên Redis/Hazelcast
 * 
 * @author BTMS Team
 * @version 1.0
 */
@Configuration
@EnableCaching
public class CacheConfig {
    
    /**
     * Define cache manager với các cache names
     * 
     * Cache Categories:
     * - featured-tournaments: Giải nổi bật (ít thay đổi)
     * - tournament-stats: Thống kê (cần fresh data)
     * - tournament-list: Danh sách (frequently updated)
     * - tournament-detail: Chi tiết giải đấu theo ID
     * - upcoming-tournaments: Giải sắp diễn ra
     * - ongoing-tournaments: Giải đang diễn ra
     * - registration-tournaments: Giải mở đăng ký
     * - landing-page-stats: Stats cho landing page
     * 
     * NOTE: ConcurrentMapCache không support TTL auto-eviction
     * Để có TTL, cần upgrade lên Caffeine hoặc Redis
     */
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
            "featured-tournaments",
            "tournament-stats", 
            "tournament-list",
            "tournament-search",
            "tournament-detail",
            "upcoming-tournaments",
            "ongoing-tournaments",
            "registration-tournaments",
            "landing-page-stats"
        );
    }
}
