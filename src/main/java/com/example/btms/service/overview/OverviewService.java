package com.example.btms.service.overview;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.btms.web.dto.OverviewStatsDTO;
import com.example.btms.web.dto.ContentStatsDTO;
import com.example.btms.model.tournament.GiaiDau;

/**
 * Service để lấy thống kê tổng quan của giải đấu
 */
public class OverviewService {

    private Connection connection;

    public OverviewService(Connection connection) {
        this.connection = connection;
    }

    /**
     * Lấy thống kê tổng quan cho giải đấu hiện tại
     */
    public OverviewStatsDTO getOverviewStats(GiaiDau giaiDau) {
        if (giaiDau == null) {
            return new OverviewStatsDTO(0, 0, 0, "Chưa chọn giải", "Không có dữ liệu");
        }

        try {
            int totalContents = getTotalContents(giaiDau.getId());
            int totalPlayersInTournament = getTotalPlayersInTournament(giaiDau.getId());
            int totalClubs = getTotalClubs();

            return new OverviewStatsDTO(
                    totalContents,
                    totalPlayersInTournament,
                    totalClubs,
                    giaiDau.getTenGiai(),
                    "Đang diễn ra");
        } catch (SQLException e) {
            System.err.println("Lỗi lấy thống kê tổng quan: " + e.getMessage());
            return new OverviewStatsDTO(0, 0, 0, giaiDau.getTenGiai(), "Lỗi dữ liệu");
        }
    }

    /**
     * Lấy thống kê chi tiết theo từng nội dung cho giải đấu hiện tại
     */
    public Map<String, Integer> getContentStats(int tournamentId) {
        Map<String, Integer> stats = new HashMap<>();

        try {
            String sql = """
                    SELECT nd.TEN_NOI_DUNG,
                           (SELECT COUNT(*) FROM DANG_KI_CA_NHAN dkcn
                            WHERE dkcn.ID_NOI_DUNG = nd.ID AND dkcn.ID_GIAI = ?) +
                           (SELECT COUNT(*) * 2 FROM DANG_KI_DOI dkd
                            WHERE dkd.ID_NOI_DUNG = nd.ID AND dkd.ID_GIAI = ?) as so_vdv
                    FROM NOI_DUNG nd
                    JOIN CHI_TIET_GIAI_DAU ctgd ON nd.ID = ctgd.ID_NOI_DUNG
                    WHERE ctgd.ID_GIAI = ?
                    ORDER BY nd.TEN_NOI_DUNG
                    """;

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setInt(1, tournamentId);
                stmt.setInt(2, tournamentId);
                stmt.setInt(3, tournamentId);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String tenNoiDung = rs.getString("TEN_NOI_DUNG");
                        int soVDV = rs.getInt("so_vdv");
                        stats.put(tenNoiDung, soVDV);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi lấy thống kê nội dung: " + e.getMessage());
        }
        return stats;
    }

    /**
     * Lấy thống kê chi tiết với thông tin trận đấu cho từng nội dung
     */
    public List<ContentStatsDTO> getDetailedContentStats(int tournamentId) {
        List<ContentStatsDTO> detailedStats = new ArrayList<>();

        try {
            String sql = """
                    SELECT nd.TEN_NOI_DUNG, nd.ID,
                           (SELECT COUNT(*) FROM DANG_KI_CA_NHAN dkcn
                            WHERE dkcn.ID_NOI_DUNG = nd.ID AND dkcn.ID_GIAI = ?) +
                           (SELECT COUNT(*) * 2 FROM DANG_KI_DOI dkd
                            WHERE dkd.ID_NOI_DUNG = nd.ID AND dkd.ID_GIAI = ?) as so_vdv
                    FROM NOI_DUNG nd
                    JOIN CHI_TIET_GIAI_DAU ctgd ON nd.ID = ctgd.ID_NOI_DUNG
                    WHERE ctgd.ID_GIAI = ?
                    ORDER BY nd.TEN_NOI_DUNG
                    """;

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setInt(1, tournamentId);
                stmt.setInt(2, tournamentId);
                stmt.setInt(3, tournamentId);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String tenNoiDung = rs.getString("TEN_NOI_DUNG");
                        int noiDungId = rs.getInt("ID");
                        int soVDV = rs.getInt("so_vdv");

                        // Tính số trận dự kiến (số VDV - 1, tối thiểu là 0)
                        int soTranDuKien = Math.max(0, soVDV - 1);

                        // Đếm số trận đã thi đấu
                        int soTranDaThiDau = countMatchesPlayed(noiDungId, tournamentId);

                        // Xác định trạng thái
                        String trangThai;
                        if (soVDV == 0) {
                            trangThai = "Chưa có VDV";
                        } else if (soTranDaThiDau >= soTranDuKien) {
                            trangThai = "Hoàn thành";
                        } else {
                            trangThai = "Chưa hoàn thành";
                        }

                        detailedStats.add(new ContentStatsDTO(
                                tenNoiDung, soVDV, soTranDuKien, soTranDaThiDau, trangThai));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi lấy thống kê chi tiết nội dung: " + e.getMessage());
        }
        return detailedStats;
    }

    /**
     * Đếm số trận đã thi đấu cho một nội dung
     */
    private int countMatchesPlayed(int noiDungId, int tournamentId) {
        try {
            // Đếm từ cả KET_QUA_CA_NHAN và KET_QUA_DOI
            String sql = """
                    SELECT
                        (SELECT COUNT(*) FROM KET_QUA_CA_NHAN kqcn
                         WHERE kqcn.ID_NOI_DUNG = ? AND kqcn.ID_GIAI = ?) +
                        (SELECT COUNT(*) FROM KET_QUA_DOI kqd
                         WHERE kqd.ID_NOI_DUNG = ? AND kqd.ID_GIAI = ?) as total_matches
                    """;

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setInt(1, noiDungId);
                stmt.setInt(2, tournamentId);
                stmt.setInt(3, noiDungId);
                stmt.setInt(4, tournamentId);

                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next() ? rs.getInt("total_matches") : 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi đếm trận đấu: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Lấy thống kê chi tiết theo từng câu lạc bộ
     */
    public Map<String, Integer> getClubStats() {
        Map<String, Integer> stats = new HashMap<>();

        try {
            String sql = """
                    SELECT clb.TEN_CLB,
                           (SELECT COUNT(*) FROM VAN_DONG_VIEN vdv
                            WHERE vdv.ID_CLB = clb.ID) as so_vdv
                    FROM CAU_LAC_BO clb
                    ORDER BY clb.TEN_CLB
                    """;

            try (PreparedStatement stmt = connection.prepareStatement(sql);
                    ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    String tenCLB = rs.getString("TEN_CLB");
                    int soVDV = rs.getInt("so_vdv");
                    stats.put(tenCLB, soVDV);
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi lấy thống kê câu lạc bộ: " + e.getMessage());
        }
        return stats;
    }

    private int getTotalContents(int tournamentId) throws SQLException {
        String sql = "SELECT COUNT(DISTINCT nd.ID) FROM NOI_DUNG nd JOIN CHI_TIET_GIAI_DAU ctgd ON nd.ID = ctgd.ID_NOI_DUNG WHERE ctgd.ID_GIAI = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, tournamentId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private int getTotalPlayersInTournament(int tournamentId) throws SQLException {
        String sql = """
                SELECT
                    (SELECT COUNT(*) FROM DANG_KI_CA_NHAN dkcn
                     WHERE dkcn.ID_GIAI = ?) +
                    (SELECT COUNT(*) * 2 FROM DANG_KI_DOI dkd
                     WHERE dkd.ID_GIAI = ?) as total
                """;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, tournamentId);
            stmt.setInt(2, tournamentId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt("total") : 0;
            }
        }
    }

    private int getTotalClubs() throws SQLException {
        String sql = "SELECT COUNT(*) FROM CAU_LAC_BO";
        try (PreparedStatement stmt = connection.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }
}