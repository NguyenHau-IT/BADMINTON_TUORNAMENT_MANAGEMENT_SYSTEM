package com.example.btms.web.controller.rankings;

import com.example.btms.service.rankings.RankingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller cho module Bảng xếp hạng
 * Quản lý rankings VĐV, CLB
 * 
 * @author BTMS Team
 * @version 2.0 - Web Platform (Database Integration)
 */
@Controller
@RequestMapping("/rankings")
public class RankingsController {

    @Autowired
    private RankingsService rankingsService;

    /**
     * Trang chủ bảng xếp hạng
     */
    @GetMapping({"", "/", "/home"})
    public String rankingsHome(Model model) {
        // Top players từ database
        model.addAttribute("topMenSingles", rankingsService.getTopMenSingles(10));
        model.addAttribute("topWomenSingles", rankingsService.getTopWomenSingles(10));
        model.addAttribute("topMenDoubles", rankingsService.getTopMenSingles(5)); // Reuse for now
        model.addAttribute("topWomenDoubles", rankingsService.getTopWomenSingles(5));
        
        // Top clubs từ database
        model.addAttribute("topClubs", rankingsService.getTopClubs(10));
        
        // Stats từ database
        Map<String, Object> stats = rankingsService.getRankingsStats();
        model.addAttribute("totalPlayers", stats.get("totalPlayers"));
        model.addAttribute("totalClubs", stats.get("totalClubs"));
        model.addAttribute("lastUpdated", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        
        // SEO
        model.addAttribute("pageTitle", "Bảng xếp hạng Cầu lông - BTMS");
        model.addAttribute("pageDescription", "Bảng xếp hạng VĐV và CLB cầu lông Việt Nam cập nhật mới nhất");
        model.addAttribute("activePage", "rankings");
        
        return "rankings/rankings-home";
    }

    /**
     * Bảng xếp hạng thế giới (vẫn dùng mock do cần data từ BWF)
     */
    @GetMapping("/world")
    public String worldRankings(
            @RequestParam(defaultValue = "men-singles") String category,
            @RequestParam(defaultValue = "1") int page,
            Model model) {
        
        model.addAttribute("category", category);
        model.addAttribute("categoryName", rankingsService.getCategoryName(category));
        model.addAttribute("rankings", getMockWorldRankings(category, page));
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", 10);
        model.addAttribute("scope", "world");
        
        // SEO
        model.addAttribute("pageTitle", "Xếp hạng Thế giới - " + rankingsService.getCategoryName(category));
        model.addAttribute("activePage", "rankings");
        
        return "rankings/rankings-list";
    }

    /**
     * Bảng xếp hạng quốc gia - Sử dụng database
     */
    @GetMapping("/national")
    public String nationalRankings(
            @RequestParam(defaultValue = "men-singles") String category,
            @RequestParam(defaultValue = "1") int page,
            Model model) {
        
        var rankingsPage = rankingsService.getNationalRankings(category, page, 20);
        
        model.addAttribute("category", category);
        model.addAttribute("categoryName", rankingsService.getCategoryName(category));
        model.addAttribute("rankings", rankingsPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", rankingsPage.getTotalPages() > 0 ? rankingsPage.getTotalPages() : 1);
        model.addAttribute("scope", "national");
        
        // SEO
        model.addAttribute("pageTitle", "Xếp hạng Quốc gia - " + rankingsService.getCategoryName(category));
        model.addAttribute("activePage", "rankings");
        
        return "rankings/rankings-list";
    }

    /**
     * Bảng xếp hạng CLB - Sử dụng database
     */
    @GetMapping("/club")
    public String clubRankings(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) String province,
            Model model) {
        
        var clubsPage = rankingsService.getClubRankings(page, 20);
        
        model.addAttribute("clubs", rankingsService.convertClubResults(clubsPage.getContent()));
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", clubsPage.getTotalPages() > 0 ? clubsPage.getTotalPages() : 1);
        model.addAttribute("selectedProvince", province);
        model.addAttribute("provinces", getProvinces());
        
        // SEO
        model.addAttribute("pageTitle", "Xếp hạng Câu lạc bộ - BTMS");
        model.addAttribute("activePage", "rankings");
        
        return "rankings/rankings-club";
    }

    /**
     * Chi tiết xếp hạng VĐV - Sử dụng database
     */
    @GetMapping("/player/{id}")
    public String playerRankingDetail(@PathVariable int id, Model model) {
        Map<String, Object> player = rankingsService.getPlayerRankingDetail(id);
        
        if (player == null) {
            return "redirect:/rankings?error=player-not-found";
        }
        
        model.addAttribute("player", player);
        model.addAttribute("rankHistory", getMockRankHistory(id)); // Mock - cần bảng history
        model.addAttribute("recentMatches", getMockRecentMatches(id)); // Mock - cần join tables
        
        // SEO
        model.addAttribute("pageTitle", player.get("name") + " - Xếp hạng BTMS");
        model.addAttribute("activePage", "rankings");
        
        return "rankings/player-ranking-detail";
    }

    // ========== MOCK DATA (Dữ liệu chưa có trong DB) ==========
    
    private List<Map<String, Object>> getMockWorldRankings(String category, int page) {
        List<Map<String, Object>> rankings = new ArrayList<>();
        String[] countries = {"Trung Quốc", "Indonesia", "Nhật Bản", "Đan Mạch", "Malaysia", 
            "Hàn Quốc", "Thái Lan", "Ấn Độ", "Việt Nam", "Đài Loan"};
        String[] names = {"Viktor Axelsen", "Anthony Sinisuka", "Kodai Naraoka", "Kento Momota", 
            "Lee Zii Jia", "An Se Young", "Chen Yu Fei", "Tai Tzu Ying"};
        
        int start = (page - 1) * 20;
        for (int i = 0; i < 20; i++) {
            int rank = start + i + 1;
            Map<String, Object> player = new HashMap<>();
            player.put("rank", rank);
            player.put("name", names[i % names.length] + " " + rank);
            player.put("category", category);
            player.put("points", 100000 - rank * 500);
            player.put("country", countries[i % countries.length]);
            player.put("avatar", "/images/players/default-avatar.png");
            rankings.add(player);
        }
        
        return rankings;
    }
    
    private List<Map<String, Object>> getMockRankHistory(int playerId) {
        List<Map<String, Object>> history = new ArrayList<>();
        int[] ranks = {1, 1, 2, 2, 3, 2, 1, 1, 2, 3, 4, 3};
        String[] months = {"12/2025", "11/2025", "10/2025", "09/2025", "08/2025", "07/2025",
            "06/2025", "05/2025", "04/2025", "03/2025", "02/2025", "01/2025"};
        
        for (int i = 0; i < ranks.length; i++) {
            history.add(Map.of("month", months[i], "rank", ranks[i], "points", 52000 - i * 500));
        }
        
        return history;
    }
    
    private List<Map<String, Object>> getMockRecentMatches(int playerId) {
        List<Map<String, Object>> matches = new ArrayList<>();
        
        matches.add(Map.of("date", "28/11/2025", "tournament", "BTMS Cup 2025", 
            "opponent", "Lê Đức Phát", "result", "W", "score", "21-18, 21-15"));
        matches.add(Map.of("date", "25/11/2025", "tournament", "BTMS Cup 2025", 
            "opponent", "Phạm Cao Cường", "result", "W", "score", "21-12, 21-19"));
        matches.add(Map.of("date", "20/11/2025", "tournament", "Giải Hà Nội Open", 
            "opponent", "Nguyễn Hải Đăng", "result", "L", "score", "19-21, 21-18, 18-21"));
        
        return matches;
    }
    
    private List<String> getProvinces() {
        return List.of("Hà Nội", "TP.HCM", "Đà Nẵng", "Hải Phòng", "Cần Thơ", 
            "Bình Dương", "Đồng Nai", "Khánh Hòa", "Thừa Thiên Huế", "Nghệ An");
    }
}
