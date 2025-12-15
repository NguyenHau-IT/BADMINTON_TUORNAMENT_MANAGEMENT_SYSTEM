package com.example.btms.web.controller.news;

import com.example.btms.model.news.NewsArticleEntity;
import com.example.btms.service.news.NewsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * Controller cho module Tin tức
 * Quản lý các trang tin tức, sự kiện, kết quả
 * 
 * @author BTMS Team
 * @version 2.0 - Web Platform (Database Integration)
 */
@Controller
@RequestMapping("/news")
public class NewsController {

    @Autowired
    private NewsService newsService;

    /**
     * Trang chủ tin tức
     */
    @GetMapping({"", "/", "/home"})
    public String newsHome(Model model) {
        // Featured news từ database
        model.addAttribute("featuredNews", newsService.getFeaturedNews(3));
        
        // Latest news từ database
        model.addAttribute("latestNews", newsService.getLatestNews(9));
        
        // Categories với count từ database
        model.addAttribute("categories", newsService.getNewsCategories());
        
        // Stats từ database
        Map<String, Object> stats = newsService.getNewsStats();
        model.addAttribute("totalNews", stats.get("totalNews"));
        model.addAttribute("thisWeek", stats.get("thisWeek"));
        
        // SEO
        model.addAttribute("pageTitle", "Tin tức Cầu lông - BTMS");
        model.addAttribute("pageDescription", "Cập nhật tin tức mới nhất về các giải đấu cầu lông, kết quả thi đấu và sự kiện nổi bật");
        model.addAttribute("activePage", "news");
        
        return "news/news-home";
    }

    /**
     * Danh sách tin tức theo category
     */
    @GetMapping("/category/{category}")
    public String newsByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "1") int page,
            Model model) {
        
        Page<NewsArticleEntity> newsPage = newsService.getNewsByCategory(category, page, 12);
        
        model.addAttribute("category", category);
        model.addAttribute("categoryName", newsService.getCategoryName(category));
        model.addAttribute("newsList", newsPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", newsPage.getTotalPages() > 0 ? newsPage.getTotalPages() : 1);
        
        // SEO
        model.addAttribute("pageTitle", newsService.getCategoryName(category) + " - Tin tức BTMS");
        model.addAttribute("activePage", "news");
        
        return "news/news-list";
    }

    /**
     * Chi tiết bài viết
     */
    @GetMapping("/{id}")
    public String newsDetail(@PathVariable int id, Model model) {
        Map<String, Object> article = newsService.getArticleDetail(id);
        
        if (article == null) {
            return "redirect:/news?error=not-found";
        }
        
        model.addAttribute("article", article);
        
        // Related news từ database
        String category = (String) article.get("category");
        model.addAttribute("relatedNews", newsService.getRelatedNews(id, category, 4));
        
        // SEO
        model.addAttribute("pageTitle", article.get("title") + " - BTMS");
        model.addAttribute("pageDescription", article.get("excerpt"));
        model.addAttribute("activePage", "news");
        
        return "news/news-detail";
    }
}
