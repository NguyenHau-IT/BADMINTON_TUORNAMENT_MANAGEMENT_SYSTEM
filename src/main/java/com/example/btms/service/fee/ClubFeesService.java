package com.example.btms.service.fee;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.btms.model.club.CauLacBo;
import com.example.btms.model.player.DangKiCaNhan;
import com.example.btms.model.player.VanDongVien;
import com.example.btms.model.tournament.GiaiDau;
import com.example.btms.repository.club.CauLacBoRepository;
import com.example.btms.repository.player.DangKiCaNhanRepository;
import com.example.btms.repository.player.VanDongVienRepository;
import com.example.btms.repository.tuornament.GiaiDauRepository;
import com.example.btms.service.db.DatabaseService;
import com.example.btms.util.fees.FeesCalculator;
import com.example.btms.util.fees.FeesCalculator.ClubFeeInfo;
import com.example.btms.util.log.Log;

/**
 * Service để tính lệ phí theo câu lạc bộ
 */
@Service
public class ClubFeesService {
    private final DatabaseService databaseService;
    private final Log log = new Log();
    private Connection directConnection; // connection trực tiếp nếu DatabaseService không work

    public ClubFeesService(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    /**
     * Set connection trực tiếp (fallback khi DatabaseService không available)
     */
    public void setDirectConnection(Connection conn) {
        this.directConnection = conn;
    }

    /**
     * Lấy connection từ DatabaseService hoặc directConnection
     */
    private Connection getConnection() throws SQLException {
        // Nếu có connection trực tiếp, ưu tiên dùng
        if (directConnection != null && !directConnection.isClosed()) {
            return directConnection;
        }

        // Nếu không thì dùng DatabaseService
        if (databaseService != null) {
            var conn = databaseService.current();
            if (conn != null && !conn.isClosed()) {
                return conn;
            }
        }

        throw new SQLException("No database connection available");
    }

    /**
     * Lấy tất cả giải đấu
     */
    public List<GiaiDau> getAllTournaments() throws SQLException {
        var conn = getConnection();
        GiaiDauRepository repo = new GiaiDauRepository(conn);
        return repo.findAll();
    }

    /**
     * Tính lệ phí theo câu lạc bộ cho 1 giải đấu
     */
    public Map<Integer, ClubFeeInfo> calculateClubFees(int tournamentId) throws SQLException {
        var conn = getConnection();

        // Load data
        DangKiCaNhanRepository regRepo = new DangKiCaNhanRepository(conn);
        VanDongVienRepository playerRepo = new VanDongVienRepository(conn);
        CauLacBoRepository clubRepo = new CauLacBoRepository(conn);

        log.logTs("Tải dữ liệu cho giải ID: %d", tournamentId);

        List<DangKiCaNhan> registrations = regRepo.getByGiaiDau(tournamentId);
        log.logTs("Số lượng đăng ký: %d", registrations.size());

        List<VanDongVien> players = playerRepo.findAll();
        log.logTs("Số lượng VĐV: %d", players.size());

        List<CauLacBo> clubs = clubRepo.findAll();
        log.logTs("Số lượng CLB: %d", clubs.size());

        // Calculate fees
        Map<Integer, ClubFeeInfo> result = FeesCalculator.calculateClubFees(registrations, players, clubs);
        log.logTs("Tính lệ phí xong, số CLB có phí: %d", result.size());

        return result;
    }

    /**
     * Lấy chi tiết VĐV và nội dung đăng ký của một CLB
     */
    public java.util.List<java.util.Map<String, Object>> getClubDetails(int clubId, int tournamentId)
            throws SQLException {
        var conn = getConnection();

        DangKiCaNhanRepository regRepo = new DangKiCaNhanRepository(conn);
        VanDongVienRepository playerRepo = new VanDongVienRepository(conn);
        com.example.btms.repository.category.NoiDungRepository contentRepo = new com.example.btms.repository.category.NoiDungRepository(
                conn);

        // Lấy tất cả đăng ký của giải
        List<DangKiCaNhan> registrations = regRepo.getByGiaiDau(tournamentId);

        // Lấy tất cả VĐV
        List<VanDongVien> players = playerRepo.findAll();
        Map<Integer, VanDongVien> playerMap = new java.util.LinkedHashMap<>();
        for (VanDongVien p : players) {
            playerMap.put(p.getId(), p);
        }

        // Lấy tất cả nội dung
        List<com.example.btms.model.category.NoiDung> contents = contentRepo.findAll();
        Map<Integer, com.example.btms.model.category.NoiDung> contentMap = new java.util.LinkedHashMap<>();
        for (com.example.btms.model.category.NoiDung c : contents) {
            contentMap.put(c.getId(), c);
        }

        // Lọc đăng ký của CLB này
        java.util.List<java.util.Map<String, Object>> details = new java.util.ArrayList<>();
        for (DangKiCaNhan reg : registrations) {
            VanDongVien player = playerMap.get(reg.getIdVdv());
            if (player != null && player.getIdClb() == clubId) {
                com.example.btms.model.category.NoiDung content = contentMap.get(reg.getIdNoiDung());

                java.util.Map<String, Object> item = new java.util.LinkedHashMap<>();
                item.put("playerId", reg.getIdVdv());
                item.put("playerName", player.getHoTen());
                item.put("contentId", reg.getIdNoiDung());
                item.put("contentName", content != null ? content.getTenNoiDung() : "Unknown");
                details.add(item);
            }
        }

        return details;
    }
}
