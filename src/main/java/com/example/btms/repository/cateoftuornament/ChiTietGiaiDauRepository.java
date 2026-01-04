package com.example.btms.repository.cateoftuornament;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.example.btms.model.cateoftuornament.ChiTietGiaiDau;

public class ChiTietGiaiDauRepository {
    private final Connection conn;

    public ChiTietGiaiDauRepository(Connection conn) {
        this.conn = conn;
    }

    // CREATE - Thêm chi tiết giải đấu
    public void addChiTietGiaiDau(ChiTietGiaiDau chiTiet) {
        String sql = "INSERT INTO CHI_TIET_GIAI_DAU (ID_GIAI, ID_NOI_DUNG, TUOI_DUOI, TUOI_TREN, SO_DO) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, chiTiet.getIdGiaiDau());
            pstmt.setInt(2, chiTiet.getIdNoiDung());
            pstmt.setInt(3, chiTiet.getTuoiDuoi());
            pstmt.setInt(4, chiTiet.getTuoiTren());
            pstmt.setInt(5, chiTiet.getSoDo());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // READ - Lấy toàn bộ danh sách
    public List<ChiTietGiaiDau> getAllChiTietGiaiDau() {
        List<ChiTietGiaiDau> list = new ArrayList<>();
        String sql = "SELECT * FROM CHI_TIET_GIAI_DAU";
        try (Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                int soDo = 1; // Default
                try {
                    soDo = rs.getInt("SO_DO");
                } catch (SQLException ignored) {
                }
                ChiTietGiaiDau chiTiet = new ChiTietGiaiDau(
                        rs.getInt("ID_GIAI"),
                        rs.getInt("ID_NOI_DUNG"),
                        rs.getInt("TUOI_DUOI"),
                        rs.getInt("TUOI_TREN"),
                        soDo);
                list.add(chiTiet);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // READ - Lấy theo ID (cặp ID_GIAI_DAU + ID_NOI_DUNG)
    public ChiTietGiaiDau getChiTietGiaiDauById(int idGiaiDau, int idNoiDung) {
        String sql = "SELECT * FROM CHI_TIET_GIAI_DAU WHERE ID_GIAI = ? AND ID_NOI_DUNG = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idGiaiDau);
            pstmt.setInt(2, idNoiDung);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int soDo = 1; // Default
                    try {
                        soDo = rs.getInt("SO_DO");
                    } catch (SQLException ignored) {
                    }
                    return new ChiTietGiaiDau(
                            rs.getInt("ID_GIAI"),
                            rs.getInt("ID_NOI_DUNG"),
                            rs.getInt("TUOI_DUOI"),
                            rs.getInt("TUOI_TREN"),
                            soDo);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // UPDATE - Cập nhật chi tiết giải đấu
    public void updateChiTietGiaiDau(ChiTietGiaiDau chiTiet) {
        String sql = "UPDATE CHI_TIET_GIAI_DAU SET TUOI_DUOI = ?, TUOI_TREN = ?, SO_DO = ? WHERE ID_GIAI = ? AND ID_NOI_DUNG = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, chiTiet.getTuoiDuoi());
            pstmt.setInt(2, chiTiet.getTuoiTren());
            pstmt.setInt(3, chiTiet.getSoDo());
            pstmt.setInt(4, chiTiet.getIdGiaiDau());
            pstmt.setInt(5, chiTiet.getIdNoiDung());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // DELETE - Xóa chi tiết giải đấu
    public void deleteChiTietGiaiDau(int idGiaiDau, int idNoiDung) {
        String sql = "DELETE FROM CHI_TIET_GIAI_DAU WHERE ID_GIAI = ? AND ID_NOI_DUNG = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idGiaiDau);
            pstmt.setInt(2, idNoiDung);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
