package com.example.btms.service.players;

import com.example.btms.model.club.CauLacBoEntity;
import com.example.btms.model.player.VanDongVienEntity;
import com.example.btms.repository.web.club.CauLacBoRepository;
import com.example.btms.repository.web.vdv.VanDongVienRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service cho Players module
 * Xử lý logic VĐV - danh sách, chi tiết, đăng ký
 * 
 * @author BTMS Team
 * @version 2.0 - Web Platform
 */
@Service
public class PlayersService {

    @Autowired
    private VanDongVienRepository vanDongVienRepository;

    @Autowired
    private CauLacBoRepository cauLacBoRepository;

    // ========== PLAYERS LIST ==========

    /**
     * Lấy danh sách VĐV với filters và pagination
     */
    public Page<VanDongVienEntity> getPlayers(String search, String gioiTinh, Integer idClb, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.ASC, "xepHangQuocGia"));
        
        return vanDongVienRepository.searchPlayers(search, gioiTinh, idClb, pageRequest);
    }

    /**
     * Lấy tổng số VĐV active
     */
    public long getTotalPlayers() {
        return vanDongVienRepository.countActivePlayers();
    }

    /**
     * Lấy danh sách tỉnh/thành (mock - cần bảng riêng)
     */
    public List<String> getProvinces() {
        return List.of("Hà Nội", "TP.HCM", "Đà Nẵng", "Hải Phòng", "Cần Thơ", 
                "Bình Dương", "Đồng Nai", "Khánh Hòa", "Thừa Thiên Huế", "Nghệ An");
    }

    /**
     * Lấy danh sách CLB
     */
    public List<CauLacBoEntity> getClubs() {
        return cauLacBoRepository.findAll(Sort.by(Sort.Direction.ASC, "tenClb"));
    }

    /**
     * Lấy danh sách categories/nội dung thi đấu
     */
    public List<Map<String, String>> getCategories() {
        List<Map<String, String>> categories = new ArrayList<>();
        categories.add(Map.of("value", "men-singles", "label", "Đơn Nam"));
        categories.add(Map.of("value", "women-singles", "label", "Đơn Nữ"));
        categories.add(Map.of("value", "men-doubles", "label", "Đôi Nam"));
        categories.add(Map.of("value", "women-doubles", "label", "Đôi Nữ"));
        categories.add(Map.of("value", "mixed-doubles", "label", "Đôi Nam Nữ"));
        return categories;
    }

    // ========== PLAYER DETAIL ==========

    /**
     * Lấy chi tiết VĐV
     */
    public Map<String, Object> getPlayerDetail(Integer id) {
        Optional<VanDongVienEntity> optPlayer = vanDongVienRepository.findById(id);
        
        if (optPlayer.isEmpty()) {
            return null;
        }
        
        VanDongVienEntity player = optPlayer.get();
        Map<String, Object> detail = new HashMap<>();
        
        // Basic info
        detail.put("id", player.getId());
        detail.put("name", player.getHoTen());
        detail.put("avatar", player.getAnhDaiDien() != null ? player.getAnhDaiDien() : "/images/players/default-avatar.png");
        detail.put("coverImage", "/images/players/default-cover.jpg");
        detail.put("gender", player.getGioiTinhText());
        detail.put("birthDate", player.getNgaySinh() != null ? player.getNgaySinh().toString() : null);
        detail.put("age", player.getTuoi());
        detail.put("height", player.getChieuCao() != null ? player.getChieuCao() + " cm" : null);
        detail.put("weight", player.getCanNang() != null ? player.getCanNang() + " kg" : null);
        detail.put("bio", player.getTieuSu());
        detail.put("phone", player.getDienThoai());
        detail.put("email", player.getEmail());
        
        // Ranking info
        detail.put("rank", player.getXepHangQuocGia());
        detail.put("worldRank", player.getXepHangTheGioi());
        
        // Club info
        if (player.getIdClb() != null) {
            cauLacBoRepository.findById(player.getIdClb())
                    .ifPresent(club -> {
                        detail.put("club", club.getTenClb());
                        detail.put("clubId", club.getId());
                    });
        }
        
        // Social media (mock)
        detail.put("socialMedia", Map.of(
                "facebook", "https://facebook.com/player" + id,
                "instagram", "https://instagram.com/player" + id
        ));
        
        return detail;
    }

    /**
     * Lấy thống kê VĐV
     */
    public Map<String, Object> getPlayerStats(Integer playerId) {
        Optional<VanDongVienEntity> optPlayer = vanDongVienRepository.findById(playerId);
        
        if (optPlayer.isEmpty()) {
            return new HashMap<>();
        }
        
        VanDongVienEntity player = optPlayer.get();
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalMatches", player.getTongSoTran());
        stats.put("wins", player.getTongTranThang() != null ? player.getTongTranThang() : 0);
        stats.put("losses", player.getTongTranThua() != null ? player.getTongTranThua() : 0);
        stats.put("winRate", player.getTiLeThangPercent());
        stats.put("highestRank", player.getXepHangQuocGia()); // Would need history
        
        // Mock additional stats
        stats.put("totalTournaments", (int)(Math.random() * 50) + 10);
        stats.put("titlesWon", (int)(Math.random() * 10));
        stats.put("runnerUp", (int)(Math.random() * 8));
        stats.put("currentStreak", (int)(Math.random() * 10));
        stats.put("longestStreak", (int)(Math.random() * 20) + 5);
        
        return stats;
    }

    /**
     * Lấy lịch sử giải đấu của VĐV (mock)
     */
    public List<Map<String, Object>> getPlayerTournaments(Integer playerId) {
        List<Map<String, Object>> tournaments = new ArrayList<>();
        
        // Mock data - would need JOIN with tournament tables
        tournaments.add(Map.of("name", "BTMS Cup 2025", "date", "11/2025", 
                "result", "Vô địch", "resultClass", "gold"));
        tournaments.add(Map.of("name", "Giải Hà Nội Open", "date", "10/2025", 
                "result", "Á quân", "resultClass", "silver"));
        tournaments.add(Map.of("name", "Vietnam Open 2025", "date", "09/2025", 
                "result", "Tứ kết", "resultClass", ""));
        tournaments.add(Map.of("name", "SEA Games 32", "date", "05/2025", 
                "result", "HCĐ", "resultClass", "bronze"));
        
        return tournaments;
    }

    /**
     * Lấy thành tích VĐV (mock)
     */
    public List<Map<String, Object>> getPlayerAchievements(Integer playerId) {
        List<Map<String, Object>> achievements = new ArrayList<>();
        
        achievements.add(Map.of("year", "2025", "title", "Vô địch BTMS Cup", "icon", "bi-trophy-fill"));
        achievements.add(Map.of("year", "2024", "title", "Vô địch Quốc gia", "icon", "bi-trophy-fill"));
        achievements.add(Map.of("year", "2023", "title", "HCV SEA Games", "icon", "bi-award-fill"));
        
        return achievements;
    }

    /**
     * Lấy gallery ảnh VĐV (mock)
     */
    public List<String> getPlayerGallery(Integer playerId) {
        List<String> gallery = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            gallery.add("/images/players/gallery-" + playerId + "-" + i + ".jpg");
        }
        return gallery;
    }

    // ========== TOP PLAYERS ==========

    /**
     * Lấy top VĐV nam
     */
    public List<Map<String, Object>> getTopMenPlayers(int limit) {
        List<VanDongVienEntity> players = vanDongVienRepository
                .findTopMenByNationalRanking(PageRequest.of(0, limit));
        return convertPlayersToList(players);
    }

    /**
     * Lấy top VĐV nữ
     */
    public List<Map<String, Object>> getTopWomenPlayers(int limit) {
        List<VanDongVienEntity> players = vanDongVienRepository
                .findTopWomenByNationalRanking(PageRequest.of(0, limit));
        return convertPlayersToList(players);
    }

    /**
     * Lấy VĐV triển vọng (mới)
     */
    public List<Map<String, Object>> getRisingStars(int limit) {
        List<VanDongVienEntity> players = vanDongVienRepository
                .findRecentPlayers(PageRequest.of(0, limit));
        
        List<Map<String, Object>> result = convertPlayersToList(players);
        
        // Add improvement mock
        for (int i = 0; i < result.size(); i++) {
            result.get(i).put("improvement", "+" + (15 - i * 2) + " bậc");
        }
        
        return result;
    }

    /**
     * Lấy VĐV có nhiều danh hiệu nhất
     */
    public List<Map<String, Object>> getMostTitledPlayers(int limit) {
        List<VanDongVienEntity> players = vanDongVienRepository
                .findTopByWins(PageRequest.of(0, limit));
        
        List<Map<String, Object>> result = convertPlayersToList(players);
        
        // Add titles mock
        int[] titles = {24, 18, 12, 10, 8};
        for (int i = 0; i < result.size() && i < titles.length; i++) {
            result.get(i).put("totalTitles", titles[i]);
        }
        
        return result;
    }

    // ========== PLAYER REGISTRATION ==========

    /**
     * Kiểm tra email đã tồn tại
     */
    public boolean isEmailExists(String email) {
        return vanDongVienRepository.existsByEmail(email);
    }

    /**
     * Kiểm tra số điện thoại đã tồn tại
     */
    public boolean isPhoneExists(String phone) {
        return vanDongVienRepository.existsByDienThoai(phone);
    }

    /**
     * Đăng ký VĐV mới
     */
    public VanDongVienEntity registerPlayer(VanDongVienEntity player) {
        return vanDongVienRepository.save(player);
    }

    // ========== HELPER METHODS ==========

    /**
     * Convert player list to Map list (for template compatibility)
     * Public version for controller use
     */
    public List<Map<String, Object>> convertPlayersToMapList(List<VanDongVienEntity> players) {
        return convertPlayersToList(players);
    }

    /**
     * Convert player list to Map list
     */
    private List<Map<String, Object>> convertPlayersToList(List<VanDongVienEntity> players) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (VanDongVienEntity player : players) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", player.getId());
            map.put("name", player.getHoTen());
            map.put("avatar", player.getAnhDaiDien() != null ? player.getAnhDaiDien() : "/images/players/default-avatar.png");
            map.put("gender", player.getGioiTinhText());
            map.put("age", player.getTuoi());
            map.put("rank", player.getXepHangQuocGia());
            map.put("points", calculatePoints(player));
            map.put("wins", player.getTongTranThang());
            map.put("winRate", player.getTiLeThangPercent());
            map.put("province", "Việt Nam"); // Default - DB không có field province
            
            // Get club name
            if (player.getIdClb() != null) {
                cauLacBoRepository.findById(player.getIdClb())
                        .ifPresent(club -> map.put("club", club.getTenClb()));
            } else {
                map.put("club", "Chưa có CLB");
            }
            
            result.add(map);
        }
        
        return result;
    }

    /**
     * Calculate points (simplified)
     */
    private int calculatePoints(VanDongVienEntity player) {
        int basePoints = 50000;
        if (player.getXepHangQuocGia() != null) {
            basePoints -= player.getXepHangQuocGia() * 200;
        }
        if (player.getTongTranThang() != null) {
            basePoints += player.getTongTranThang() * 10;
        }
        return Math.max(basePoints, 1000);
    }
}
