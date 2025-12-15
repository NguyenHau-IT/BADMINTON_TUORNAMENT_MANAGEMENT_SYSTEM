package com.example.btms.repository.referee;

import com.example.btms.model.referee.TrongTai;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC Repository class cho CRUD operations với bảng TRONG_TAI
 */
public class TrongTaiRepository {
    private final Connection connection;

    public TrongTaiRepository(Connection connection) {
        this.connection = connection;
    }

    /**
     * Tạo trọng tài mới (CREATE)
     */
    public TrongTai save(TrongTai trongTai) throws SQLException {
        String sql = """
                INSERT INTO TRONG_TAI (MA_TRONG_TAI, HO_TEN, NGAY_SINH, GIOI_TINH,
                                     SO_DIEN_THOAI, EMAIL, MAT_KHAU, IDCLB, GHI_CHU)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, trongTai.getMaTrongTai());
            ps.setString(2, trongTai.getHoTen());
            ps.setDate(3, trongTai.getNgaySinh() != null ? Date.valueOf(trongTai.getNgaySinh()) : null);
            ps.setBoolean(4, trongTai.getGioiTinh() != null ? trongTai.getGioiTinh() : true);
            ps.setString(5, trongTai.getSoDienThoai());
            ps.setString(6, trongTai.getEmail());
            ps.setString(7, trongTai.getMatKhau());
            ps.setObject(8, trongTai.getIdClb());
            ps.setString(9, trongTai.getGhiChu());

            int rowsAffected = ps.executeUpdate();
            if (rowsAffected > 0) {
                return trongTai;
            } else {
                throw new SQLException("Creating referee failed, no rows affected.");
            }
        }
    }

    /**
     * Tìm trọng tài theo ID (READ)
     */
    public Optional<TrongTai> findById(String maTrongTai) throws SQLException {
        String sql = "SELECT * FROM TRONG_TAI WHERE MA_TRONG_TAI = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, maTrongTai);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToTrongTai(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Tìm trọng tài theo email
     */
    public Optional<TrongTai> findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM TRONG_TAI WHERE EMAIL = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToTrongTai(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Lấy tất cả trọng tài
     */
    public List<TrongTai> findAll() throws SQLException {
        String sql = "SELECT * FROM TRONG_TAI ORDER BY HO_TEN";
        List<TrongTai> results = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                results.add(mapRowToTrongTai(rs));
            }
        }
        return results;
    }

    /**
     * Kiểm tra email đã tồn tại
     */
    public boolean existsByEmail(String email) throws SQLException {
        String sql = "SELECT COUNT(1) FROM TRONG_TAI WHERE EMAIL = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /**
     * Kiểm tra trọng tài có tồn tại theo ID
     */
    public boolean existsById(String maTrongTai) throws SQLException {
        String sql = "SELECT COUNT(1) FROM TRONG_TAI WHERE MA_TRONG_TAI = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, maTrongTai);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /**
     * Xóa trọng tài theo ID
     */
    public void deleteById(String maTrongTai) throws SQLException {
        String sql = "DELETE FROM TRONG_TAI WHERE MA_TRONG_TAI = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, maTrongTai);
            ps.executeUpdate();
        }
    }

    /**
     * Đếm tổng số trọng tài
     */
    public long count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM TRONG_TAI";

        try (PreparedStatement ps = connection.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;
        }
    }

    /**
     * Map ResultSet row to TrongTai object
     */
    private TrongTai mapRowToTrongTai(ResultSet rs) throws SQLException {
        TrongTai trongTai = new TrongTai();
        trongTai.setMaTrongTai(rs.getString("MA_TRONG_TAI"));
        trongTai.setHoTen(rs.getString("HO_TEN"));

        Date ngaySinhDate = rs.getDate("NGAY_SINH");
        if (ngaySinhDate != null) {
            trongTai.setNgaySinh(ngaySinhDate.toLocalDate());
        }

        trongTai.setGioiTinh(rs.getBoolean("GIOI_TINH"));
        trongTai.setSoDienThoai(rs.getString("SO_DIEN_THOAI"));
        trongTai.setEmail(rs.getString("EMAIL"));
        trongTai.setMatKhau(rs.getString("MAT_KHAU"));

        Object idClb = rs.getObject("IDCLB");
        if (idClb != null) {
            trongTai.setIdClb((Integer) idClb);
        }

        trongTai.setGhiChu(rs.getString("GHI_CHU"));
        return trongTai;
    }
}
