package com.example.btms.repository.referee;

import com.example.btms.model.referee.PhanCongTrongTai;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC Repository class cho CRUD operations với bảng PHAN_CONG_TRONG_TAI
 */
public class PhanCongTrongTaiRepository {
    private final Connection connection;

    public PhanCongTrongTaiRepository(Connection connection) {
        this.connection = connection;
    }

    /**
     * Tạo phân công trọng tài mới (CREATE)
     */
    public PhanCongTrongTai save(PhanCongTrongTai phanCong) throws SQLException {
        String sql = """
                INSERT INTO PHAN_CONG_TRONG_TAI (MA_PHAN_CONG, MA_TRONG_TAI, MA_TRAN_DAU, VAI_TRO, GHI_CHU)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, phanCong.getMaPhanCong());
            ps.setString(2, phanCong.getMaTrongTai());
            ps.setString(3, phanCong.getMaTranDau());
            ps.setString(4, phanCong.getVaiTro());
            ps.setString(5, phanCong.getGhiChu());

            int rowsAffected = ps.executeUpdate();
            if (rowsAffected > 0) {
                return phanCong;
            } else {
                throw new SQLException("Creating referee assignment failed, no rows affected.");
            }
        }
    }

    /**
     * Tìm phân công theo ID
     */
    public Optional<PhanCongTrongTai> findById(String maPhanCong) throws SQLException {
        String sql = "SELECT * FROM PHAN_CONG_TRONG_TAI WHERE MA_PHAN_CONG = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, maPhanCong);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToPhanCong(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Tìm phân công theo mã trọng tài
     */
    public List<PhanCongTrongTai> findByMaTrongTai(String maTrongTai) throws SQLException {
        String sql = "SELECT * FROM PHAN_CONG_TRONG_TAI WHERE MA_TRONG_TAI = ?";
        List<PhanCongTrongTai> results = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, maTrongTai);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRowToPhanCong(rs));
                }
            }
        }
        return results;
    }

    /**
     * Tìm phân công theo mã trận đấu
     */
    public List<PhanCongTrongTai> findByMaTranDau(String maTranDau) throws SQLException {
        String sql = "SELECT * FROM PHAN_CONG_TRONG_TAI WHERE MA_TRAN_DAU = ?";
        List<PhanCongTrongTai> results = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, maTranDau);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRowToPhanCong(rs));
                }
            }
        }
        return results;
    }

    /**
     * Tìm phân công theo trọng tài và trận đấu
     */
    public Optional<PhanCongTrongTai> findByMaTrongTaiAndMaTranDau(String maTrongTai, String maTranDau)
            throws SQLException {
        String sql = "SELECT * FROM PHAN_CONG_TRONG_TAI WHERE MA_TRONG_TAI = ? AND MA_TRAN_DAU = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, maTrongTai);
            ps.setString(2, maTranDau);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToPhanCong(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Tìm phân công theo vai trò
     */
    public List<PhanCongTrongTai> findByVaiTro(String vaiTro) throws SQLException {
        String sql = "SELECT * FROM PHAN_CONG_TRONG_TAI WHERE VAI_TRO = ?";
        List<PhanCongTrongTai> results = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, vaiTro);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRowToPhanCong(rs));
                }
            }
        }
        return results;
    }

    /**
     * Kiểm tra trọng tài đã được phân công vào trận chưa
     */
    public boolean existsByMaTrongTaiAndMaTranDau(String maTrongTai, String maTranDau) throws SQLException {
        String sql = "SELECT COUNT(1) FROM PHAN_CONG_TRONG_TAI WHERE MA_TRONG_TAI = ? AND MA_TRAN_DAU = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, maTrongTai);
            ps.setString(2, maTranDau);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /**
     * Đếm số phân công của trọng tài
     */
    public long countByMaTrongTai(String maTrongTai) throws SQLException {
        String sql = "SELECT COUNT(*) FROM PHAN_CONG_TRONG_TAI WHERE MA_TRONG_TAI = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, maTrongTai);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return 0;
    }

    /**
     * Đếm số trọng tài được phân công vào trận
     */
    public long countByMaTranDau(String maTranDau) throws SQLException {
        String sql = "SELECT COUNT(*) FROM PHAN_CONG_TRONG_TAI WHERE MA_TRAN_DAU = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, maTranDau);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return 0;
    }

    /**
     * Xóa phân công theo ID
     */
    public void deleteById(String maPhanCong) throws SQLException {
        String sql = "DELETE FROM PHAN_CONG_TRONG_TAI WHERE MA_PHAN_CONG = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, maPhanCong);
            ps.executeUpdate();
        }
    }

    /**
     * Xóa tất cả phân công của trọng tài
     */
    public void deleteByMaTrongTai(String maTrongTai) throws SQLException {
        String sql = "DELETE FROM PHAN_CONG_TRONG_TAI WHERE MA_TRONG_TAI = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, maTrongTai);
            ps.executeUpdate();
        }
    }

    /**
     * Xóa tất cả phân công của trận đấu
     */
    public void deleteByMaTranDau(String maTranDau) throws SQLException {
        String sql = "DELETE FROM PHAN_CONG_TRONG_TAI WHERE MA_TRAN_DAU = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, maTranDau);
            ps.executeUpdate();
        }
    }

    /**
     * Lấy tất cả phân công
     */
    public List<PhanCongTrongTai> findAll() throws SQLException {
        String sql = "SELECT * FROM PHAN_CONG_TRONG_TAI ORDER BY MA_PHAN_CONG";
        List<PhanCongTrongTai> results = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                results.add(mapRowToPhanCong(rs));
            }
        }
        return results;
    }

    /**
     * Map ResultSet row to PhanCongTrongTai object
     */
    private PhanCongTrongTai mapRowToPhanCong(ResultSet rs) throws SQLException {
        PhanCongTrongTai phanCong = new PhanCongTrongTai();
        phanCong.setMaPhanCong(rs.getString("MA_PHAN_CONG"));
        phanCong.setMaTrongTai(rs.getString("MA_TRONG_TAI"));
        phanCong.setMaTranDau(rs.getString("MA_TRAN_DAU"));
        phanCong.setVaiTro(rs.getString("VAI_TRO"));
        phanCong.setGhiChu(rs.getString("GHI_CHU"));
        return phanCong;
    }
}
