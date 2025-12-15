package com.example.btms.web.controller.players;

import com.example.btms.model.player.VanDongVienEntity;
import com.example.btms.service.players.PlayersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * Controller cho module Vận động viên
 * Quản lý thông tin VĐV, đăng ký VĐV mới
 * 
 * @author BTMS Team
 * @version 2.0 - Web Platform (Database Integration)
 */
@Controller
@RequestMapping("/players")
public class PlayersController {

    @Autowired
    private PlayersService playersService;

    /**
     * Trang danh sách VĐV
     */
    @GetMapping({"", "/", "/list"})
    public String playersList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) Integer club,
            @RequestParam(required = false) String category,
            Model model) {
        
        // Convert category to gender if applicable
        String gioiTinh = null;
        if (category != null) {
            if (category.startsWith("men")) gioiTinh = "M";
            else if (category.startsWith("women")) gioiTinh = "F";
        }
        
        Page<VanDongVienEntity> playersPage = playersService.getPlayers(search, gioiTinh, club, page, 20);
        
        // Convert entities to maps for template compatibility
        List<Map<String, Object>> playersList = playersService.convertPlayersToMapList(playersPage.getContent());
        
        model.addAttribute("players", playersList);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", playersPage.getTotalPages() > 0 ? playersPage.getTotalPages() : 1);
        model.addAttribute("totalPlayers", playersService.getTotalPlayers());
        
        // Filters
        model.addAttribute("selectedSearch", search);
        model.addAttribute("selectedProvince", province);
        model.addAttribute("selectedClub", club);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("provinces", playersService.getProvinces());
        model.addAttribute("clubs", playersService.getClubs());
        model.addAttribute("categories", playersService.getCategories());
        
        // SEO
        model.addAttribute("pageTitle", "Danh sách Vận động viên - BTMS");
        model.addAttribute("pageDescription", "Danh sách VĐV cầu lông Việt Nam");
        model.addAttribute("activePage", "players");
        
        return "players/players-list";
    }

    /**
     * VĐV hàng đầu
     */
    @GetMapping("/top")
    public String topPlayers(Model model) {
        model.addAttribute("topMen", playersService.getTopMenPlayers(10));
        model.addAttribute("topWomen", playersService.getTopWomenPlayers(10));
        model.addAttribute("risingStars", playersService.getRisingStars(6));
        model.addAttribute("mostTitles", playersService.getMostTitledPlayers(5));
        
        // SEO
        model.addAttribute("pageTitle", "VĐV Hàng đầu - BTMS");
        model.addAttribute("activePage", "players");
        
        return "players/players-top";
    }

    /**
     * Chi tiết VĐV
     */
    @GetMapping("/{id}")
    public String playerDetail(@PathVariable int id, Model model) {
        Map<String, Object> player = playersService.getPlayerDetail(id);
        
        if (player == null) {
            return "redirect:/players?error=not-found";
        }
        
        model.addAttribute("player", player);
        model.addAttribute("stats", playersService.getPlayerStats(id));
        model.addAttribute("tournaments", playersService.getPlayerTournaments(id));
        model.addAttribute("achievements", playersService.getPlayerAchievements(id));
        model.addAttribute("gallery", playersService.getPlayerGallery(id));
        
        // SEO
        model.addAttribute("pageTitle", player.get("name") + " - BTMS");
        model.addAttribute("activePage", "players");
        
        return "players/player-detail";
    }

    /**
     * Trang đăng ký VĐV mới
     */
    @GetMapping("/register")
    public String registerPlayer(Model model) {
        model.addAttribute("provinces", playersService.getProvinces());
        model.addAttribute("clubs", playersService.getClubs());
        model.addAttribute("categories", playersService.getCategories());
        
        // SEO
        model.addAttribute("pageTitle", "Đăng ký Vận động viên - BTMS");
        model.addAttribute("activePage", "players");
        
        return "players/player-register";
    }
}
