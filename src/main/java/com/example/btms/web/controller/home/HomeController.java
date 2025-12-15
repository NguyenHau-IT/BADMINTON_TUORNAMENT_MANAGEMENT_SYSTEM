package com.example.btms.web.controller.home;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.btms.service.tournament.TournamentDataService;
import com.example.btms.web.dto.TournamentCardDTO;
import com.example.btms.web.dto.TournamentDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
public class HomeController {

    private static final Logger logger = LoggerFactory.getLogger(HomeController.class);

    private final TournamentDataService tournamentDataService;

    public HomeController(TournamentDataService tournamentDataService) {
        this.tournamentDataService = tournamentDataService;
    }

    /**
     * Hiển thị trang chủ (landing page)
     * 
     * @param model Model để truyền dữ liệu xuống view
     * @return Template path: main-home/main-home
     */
    @GetMapping({"/", "/home"})
    public String showHome(Model model) {
        logger.info("HomeController.showHome() called");
        
        // Khởi tạo biến với giá trị mặc định
        long totalTournaments = 0;
        long totalPlayers = 0;
        long totalMatches = 0;
        
        try {
            logger.debug("Attempting to load tournament data...");
            
            // Lấy dữ liệu giải đấu từ service (DTO objects)
            List<TournamentCardDTO> featuredTournaments = tournamentDataService.getFeaturedTournaments();
            logger.debug("Featured tournaments loaded: {}", (featuredTournaments != null ? featuredTournaments.size() : 0));
            
            List<TournamentDTO> upcomingTournaments = tournamentDataService.getUpcomingTournaments();
            logger.debug("Upcoming tournaments loaded: {}", (upcomingTournaments != null ? upcomingTournaments.size() : 0));
            
            // Lấy thống kê từ database
            Map<String, Long> stats = tournamentDataService.getLandingPageStats();
            totalTournaments = stats.getOrDefault("totalTournaments", 0L);
            
            // Tổng số người đăng ký (players)
            totalPlayers = tournamentDataService.getTotalRegistrations();
            
            // Ước tính số trận đấu
            totalMatches = tournamentDataService.getEstimatedMatches();
            
            logger.debug("Stats: tournaments={}, players={}, matches={}", 
                totalTournaments, totalPlayers, totalMatches);
            
            // Thêm dữ liệu giải đấu vào model
            model.addAttribute("featuredTournaments", featuredTournaments != null ? featuredTournaments : new ArrayList<>());
            model.addAttribute("upcomingTournaments", upcomingTournaments != null ? upcomingTournaments : new ArrayList<>());
            
            logger.debug("Tournament data loaded successfully");
        } catch (Exception e) {
            // Log error và set empty lists
            logger.error("Error loading tournament data: {}", e.getMessage(), e);
            model.addAttribute("featuredTournaments", new ArrayList<>());
            model.addAttribute("upcomingTournaments", new ArrayList<>());
        }
        
        // Thêm các thông số thống kê cho stats section (từ database)
        model.addAttribute("totalTournaments", totalTournaments);
        model.addAttribute("totalPlayers", totalPlayers);
        model.addAttribute("totalClubs", 50); // Có thể thêm service cho clubs sau
        model.addAttribute("totalMatches", totalMatches);
        model.addAttribute("growthRate", 35);
        
        // Thêm thông tin phiên bản app
        model.addAttribute("appVersion", "1.0.0");
        model.addAttribute("releaseDate", "Tháng 11, 2025");
        
        // Thêm metadata cho SEO
        model.addAttribute("pageTitle", "BTMS - Hệ thống Quản lý Giải đấu Cầu lông Chuyên nghiệp");
        model.addAttribute("pageDescription", 
            "BTMS - Hệ thống quản lý giải đấu cầu lông hiện đại với điều khiển từ xa, " +
            "real-time scoring và quản lý đa sân chuyên nghiệp. Miễn phí 100%!");
        model.addAttribute("pageKeywords", 
            "cầu lông, giải đấu, quản lý giải đấu, badminton, tournament, BTMS, " +
            "real-time scoring, điều khiển từ xa, quản lý sân");
        
        // Thêm flag để highlight menu item
        model.addAttribute("activePage", "home");
        
        logger.debug("Returning template: main-home/main-home");
        
        return "main-home/main-home";
    }
    
    /**
     * Endpoint để kiểm tra health của controller
     * Useful cho testing và monitoring
     * 
     * @return Simple text response
     */
    @GetMapping("/health")
    public String health(Model model) {
        model.addAttribute("status", "OK");
        model.addAttribute("timestamp", System.currentTimeMillis());
        model.addAttribute("message", "HomeController is running");
        
        // Có thể tạo template riêng cho health check hoặc return JSON
        // Tạm thời redirect về home
        return "redirect:/";
    }
    
    /**
     * Test endpoint đơn giản
     */
    @GetMapping("/test")
    public String test(Model model) {
        model.addAttribute("message", "Test page is working!");
        model.addAttribute("activePage", "home");
        return "test/simple-test";
    }
}
