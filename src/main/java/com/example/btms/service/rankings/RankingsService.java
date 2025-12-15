package com.example.btms.service.rankings;

import com.example.btms.model.player.VanDongVienEntity;
import com.example.btms.repository.web.club.CauLacBoRepository;
import com.example.btms.repository.web.vdv.VanDongVienRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service cho Rankings module
 * Xử lý logic bảng xếp hạng VĐV và CLB
 * 
 * @author BTMS Team
 * @version 2.0 - Web Platform
 */
@Service
public class RankingsService {

    @Autowired
    private VanDongVienRepository vanDongVienRepository;

    @Autowired
    private CauLacBoRepository cauLacBoRepository;

    // ========== VĐV RANKINGS ==========

    /**
     * Lấy top VĐV nam theo xếp hạng quốc gia
     */
    public List<Map<String, Object>> getTopMenSingles(int limit) {
        List<VanDongVienEntity> players = vanDongVienRepository
                .findTopMenByNationalRanking(PageRequest.of(0, limit));
        return convertPlayersToRankingList(players, "men-singles");
    }

    /**
     * Lấy top VĐV nữ theo xếp hạng quốc gia
     */
    public List<Map<String, Object>> getTopWomenSingles(int limit) {
        List<VanDongVienEntity> players = vanDongVienRepository
                .findTopWomenByNationalRanking(PageRequest.of(0, limit));
        return convertPlayersToRankingList(players, "women-singles");
    }

    /**
     * Lấy bảng xếp hạng quốc gia theo category với pagination
     */
    public Page<VanDongVienEntity> getNationalRankings(String category, int page, int size) {
        String gioiTinh = switch (category) {
            case "men-singles", "men-doubles" -> "M";
            case "women-singles", "women-doubles" -> "F";
            default -> null;
        };
        
        if (gioiTinh == null) {
            return vanDongVienRepository.findByTrangThai("active", PageRequest.of(page - 1, size));
        }
        
        return vanDongVienRepository.findNationalRankings(gioiTinh, PageRequest.of(page - 1, size));
    }

    /**
     * Lấy thống kê tổng quan
     */
    public Map<String, Object> getRankingsStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalPlayers", vanDongVienRepository.countActivePlayers());
        stats.put("totalClubs", cauLacBoRepository.countAllClubs());
        stats.put("menPlayers", vanDongVienRepository.countByGender("M"));
        stats.put("womenPlayers", vanDongVienRepository.countByGender("F"));
        return stats;
    }

    /**
     * Lấy chi tiết xếp hạng VĐV
     */
    public Map<String, Object> getPlayerRankingDetail(Integer playerId) {
        Optional<VanDongVienEntity> optPlayer = vanDongVienRepository.findById(playerId);
        
        if (optPlayer.isEmpty()) {
            return null;
        }
        
        VanDongVienEntity player = optPlayer.get();
        Map<String, Object> detail = new HashMap<>();
        
        // Basic info
        detail.put("id", player.getId());
        detail.put("name", player.getHoTen());
        detail.put("avatar", player.getAnhDaiDien() != null ? player.getAnhDaiDien() : "/images/players/default-avatar.png");
        detail.put("rank", player.getXepHangQuocGia());
        detail.put("worldRank", player.getXepHangTheGioi());
        detail.put("gender", player.getGioiTinhText());
        detail.put("birthYear", player.getNgaySinh() != null ? player.getNgaySinh().getYear() : null);
        detail.put("age", player.getTuoi());
        detail.put("height", player.getChieuCao() != null ? player.getChieuCao() + " cm" : null);
        detail.put("weight", player.getCanNang() != null ? player.getCanNang() + " kg" : null);
        detail.put("bio", player.getTieuSu());
        
        // Stats
        detail.put("totalMatches", player.getTongSoTran());
        detail.put("wins", player.getTongTranThang());
        detail.put("losses", player.getTongTranThua());
        detail.put("winRate", player.getTiLeThangPercent());
        
        // Club info
        if (player.getIdClb() != null) {
            cauLacBoRepository.findById(player.getIdClb())
                    .ifPresent(club -> detail.put("club", club.getTenClb()));
        }
        
        return detail;
    }

    // ========== CLB RANKINGS ==========

    /**
     * Lấy top CLB
     */
    public List<Map<String, Object>> getTopClubs(int limit) {
        List<Object[]> results = cauLacBoRepository.findTopClubsByMemberCount(limit);
        return convertClubResults(results);
    }

    /**
     * Lấy bảng xếp hạng CLB với pagination
     */
    public Page<Object[]> getClubRankings(int page, int size) {
        return cauLacBoRepository.findAllClubsWithMemberCount(PageRequest.of(page - 1, size));
    }

    /**
     * Convert CLB query results to Map list
     */
    public List<Map<String, Object>> convertClubResults(List<Object[]> results) {
        List<Map<String, Object>> clubs = new ArrayList<>();
        int rank = 1;
        
        for (Object[] row : results) {
            Map<String, Object> club = new HashMap<>();
            club.put("rank", rank++);
            club.put("id", row[0]);
            club.put("name", row[1]);
            club.put("shortName", row[2]);
            club.put("members", ((Number) row[3]).intValue());
            club.put("logo", "/images/clubs/default-logo.png");
            // Mock data for titles and points
            club.put("titles", (int)(Math.random() * 10));
            club.put("points", 5000 - (rank * 100) + (int)(Math.random() * 500));
            clubs.add(club);
        }
        
        return clubs;
    }

    // ========== HELPER METHODS ==========

    /**
     * Convert VĐV list to ranking format
     */
    private List<Map<String, Object>> convertPlayersToRankingList(List<VanDongVienEntity> players, String category) {
        List<Map<String, Object>> rankings = new ArrayList<>();
        
        for (VanDongVienEntity player : players) {
            Map<String, Object> rankItem = new HashMap<>();
            rankItem.put("id", player.getId());
            rankItem.put("rank", player.getXepHangQuocGia());
            rankItem.put("name", player.getHoTen());
            rankItem.put("category", category);
            rankItem.put("avatar", player.getAnhDaiDien() != null ? player.getAnhDaiDien() : "/images/players/default-avatar.png");
            
            // Calculate points from wins/win rate
            int points = calculatePoints(player);
            rankItem.put("points", points);
            
            // Trend (mock for now - would need history table)
            rankItem.put("trend", getTrendMock(player.getId()));
            
            rankings.add(rankItem);
        }
        
        return rankings;
    }

    /**
     * Calculate ranking points (simplified formula)
     */
    private int calculatePoints(VanDongVienEntity player) {
        int basePoints = 10000;
        if (player.getXepHangQuocGia() != null) {
            basePoints -= player.getXepHangQuocGia() * 100;
        }
        if (player.getTongTranThang() != null) {
            basePoints += player.getTongTranThang() * 10;
        }
        return Math.max(basePoints, 100);
    }

    /**
     * Get trend mock (up/down/same)
     */
    private String getTrendMock(Integer playerId) {
        int mod = playerId % 3;
        return switch (mod) {
            case 0 -> "up";
            case 1 -> "down";
            default -> "same";
        };
    }

    /**
     * Get category display name
     */
    public String getCategoryName(String category) {
        return switch (category) {
            case "men-singles" -> "Đơn Nam";
            case "women-singles" -> "Đơn Nữ";
            case "men-doubles" -> "Đôi Nam";
            case "women-doubles" -> "Đôi Nữ";
            case "mixed-doubles" -> "Đôi Nam Nữ";
            default -> "Tất cả";
        };
    }
}
