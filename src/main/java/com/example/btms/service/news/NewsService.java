package com.example.btms.service.news;

import com.example.btms.model.news.NewsArticleEntity;
import com.example.btms.repository.web.Article.NewsArticleRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/**
 * Service cho News module
 * Xử lý logic tin tức, bài viết
 * 
 * @author BTMS Team
 * @version 2.0 - Web Platform
 */
@Service
public class NewsService {

    @Autowired
    private NewsArticleRepository newsRepository;

    // ========== NEWS HOME ==========

    /**
     * Lấy tin nổi bật cho trang chủ
     */
    public List<Map<String, Object>> getFeaturedNews(int limit) {
        List<NewsArticleEntity> articles = newsRepository.findFeaturedNews(PageRequest.of(0, limit));
        
        // Nếu không có tin nổi bật, lấy tin mới nhất
        if (articles.isEmpty()) {
            articles = newsRepository.findLatestNews(PageRequest.of(0, limit));
        }
        
        return convertArticlesToMapList(articles);
    }

    /**
     * Lấy tin mới nhất
     */
    public List<Map<String, Object>> getLatestNews(int limit) {
        List<NewsArticleEntity> articles = newsRepository.findLatestNews(PageRequest.of(0, limit));
        return convertArticlesToMapList(articles);
    }

    /**
     * Lấy thống kê tin tức
     */
    public Map<String, Object> getNewsStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalNews", newsRepository.countPublishedArticles());
        
        // Đếm tin trong tuần này
        LocalDateTime startOfWeek = LocalDateTime.now()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .withHour(0).withMinute(0).withSecond(0);
        stats.put("thisWeek", newsRepository.countThisWeekArticles(startOfWeek));
        
        return stats;
    }

    /**
     * Lấy danh sách categories với số lượng bài
     */
    public List<Map<String, String>> getNewsCategories() {
        List<Map<String, String>> categories = new ArrayList<>();
        
        categories.add(createCategory("events", "Sự kiện", "bi-calendar-event", 
                String.valueOf(newsRepository.countByCategory("events"))));
        categories.add(createCategory("results", "Kết quả", "bi-trophy", 
                String.valueOf(newsRepository.countByCategory("results"))));
        categories.add(createCategory("interviews", "Phỏng vấn", "bi-mic", 
                String.valueOf(newsRepository.countByCategory("interviews"))));
        categories.add(createCategory("tips", "Mẹo & Kỹ thuật", "bi-lightbulb", 
                String.valueOf(newsRepository.countByCategory("tips"))));
        categories.add(createCategory("equipment", "Dụng cụ", "bi-bag", 
                String.valueOf(newsRepository.countByCategory("equipment"))));
        
        return categories;
    }

    // ========== NEWS LIST ==========

    /**
     * Lấy tin theo danh mục với pagination
     */
    public Page<NewsArticleEntity> getNewsByCategory(String category, int page, int size) {
        return newsRepository.findPublishedByCategory(category, PageRequest.of(page - 1, size));
    }

    /**
     * Lấy tất cả tin với pagination
     */
    public Page<NewsArticleEntity> getAllNews(int page, int size) {
        return newsRepository.findLatestNewsPage(PageRequest.of(page - 1, size));
    }

    /**
     * Tìm kiếm tin tức
     */
    public Page<NewsArticleEntity> searchNews(String keyword, int page, int size) {
        return newsRepository.searchNews(keyword, PageRequest.of(page - 1, size));
    }

    // ========== NEWS DETAIL ==========

    /**
     * Lấy chi tiết bài viết
     */
    @Transactional
    public Map<String, Object> getArticleDetail(Integer id) {
        Optional<NewsArticleEntity> optArticle = newsRepository.findById(id);
        
        if (optArticle.isEmpty()) {
            return null;
        }
        
        // Tăng lượt xem
        newsRepository.incrementViewCount(id);
        
        NewsArticleEntity article = optArticle.get();
        return convertArticleToDetailMap(article);
    }

    /**
     * Lấy tin liên quan
     */
    public List<Map<String, Object>> getRelatedNews(Integer articleId, String category, int limit) {
        List<NewsArticleEntity> articles = newsRepository.findRelatedNews(
                category, articleId, PageRequest.of(0, limit));
        return convertArticlesToMapList(articles);
    }

    /**
     * Lấy tin phổ biến nhất
     */
    public List<Map<String, Object>> getPopularNews(int limit) {
        List<NewsArticleEntity> articles = newsRepository.findPopularNews(PageRequest.of(0, limit));
        return convertArticlesToMapList(articles);
    }

    // ========== HELPER METHODS ==========

    /**
     * Convert article list to Map list
     */
    private List<Map<String, Object>> convertArticlesToMapList(List<NewsArticleEntity> articles) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (NewsArticleEntity article : articles) {
            result.add(convertArticleToMap(article));
        }
        
        return result;
    }

    /**
     * Convert single article to Map
     */
    private Map<String, Object> convertArticleToMap(NewsArticleEntity article) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", article.getId());
        map.put("title", article.getTieuDe());
        map.put("excerpt", article.getTomTat());
        map.put("image", article.getAnhDaiDien() != null ? article.getAnhDaiDien() : "/images/news/default.jpg");
        map.put("category", article.getDanhMuc());
        map.put("categoryName", article.getDanhMucText());
        map.put("publishDate", article.getNgayXuatBan() != null ? 
                article.getNgayXuatBan().toLocalDate().toString() : null);
        map.put("views", article.getLuotXem());
        map.put("featured", article.isFeatured());
        return map;
    }

    /**
     * Convert article to detail Map
     */
    private Map<String, Object> convertArticleToDetailMap(NewsArticleEntity article) {
        Map<String, Object> map = convertArticleToMap(article);
        map.put("content", article.getNoiDung());
        map.put("author", article.getTenTacGia() != null ? article.getTenTacGia() : "BTMS Admin");
        map.put("tags", List.of("Cầu lông", "Giải đấu", "Việt Nam")); // Mock tags
        return map;
    }

    /**
     * Create category map
     */
    private Map<String, String> createCategory(String slug, String name, String icon, String count) {
        Map<String, String> category = new HashMap<>();
        category.put("slug", slug);
        category.put("name", name);
        category.put("icon", icon);
        category.put("count", count);
        return category;
    }

    /**
     * Get category display name
     */
    public String getCategoryName(String slug) {
        return switch (slug) {
            case "events" -> "Sự kiện";
            case "results" -> "Kết quả";
            case "interviews" -> "Phỏng vấn";
            case "tips" -> "Mẹo & Kỹ thuật";
            case "equipment" -> "Dụng cụ";
            default -> "Tin tức";
        };
    }
}
