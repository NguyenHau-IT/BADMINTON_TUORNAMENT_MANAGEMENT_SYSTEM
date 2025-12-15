package com.example.btms.repository.web.vdv;

import com.example.btms.model.player.VanDongVienEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository cho VAN_DONG_VIEN
 * Dùng cho Rankings và Players modules
 * 
 * @author BTMS Team
 * @version 2.0 - Web Platform
 */
@Repository
public interface VanDongVienRepository extends JpaRepository<VanDongVienEntity, Integer> {

    // ========== BASIC QUERIES ==========
    
    /**
     * Tìm VĐV theo trạng thái
     */
    List<VanDongVienEntity> findByTrangThai(String trangThai);
    
    Page<VanDongVienEntity> findByTrangThai(String trangThai, Pageable pageable);

    /**
     * Tìm VĐV theo giới tính
     */
    List<VanDongVienEntity> findByGioiTinh(String gioiTinh);
    
    Page<VanDongVienEntity> findByGioiTinh(String gioiTinh, Pageable pageable);

    /**
     * Tìm VĐV theo CLB
     */
    List<VanDongVienEntity> findByIdClb(Integer idClb);
    
    Page<VanDongVienEntity> findByIdClb(Integer idClb, Pageable pageable);

    /**
     * Tìm VĐV theo tên (LIKE search)
     */
    List<VanDongVienEntity> findByHoTenContainingIgnoreCase(String hoTen);
    
    Page<VanDongVienEntity> findByHoTenContainingIgnoreCase(String hoTen, Pageable pageable);

    // ========== RANKINGS QUERIES ==========

    /**
     * Top VĐV nam theo xếp hạng quốc gia
     */
    @Query("SELECT v FROM VanDongVienEntity v WHERE v.gioiTinh = 'M' " +
           "AND v.trangThai = 'active' AND v.xepHangQuocGia IS NOT NULL " +
           "ORDER BY v.xepHangQuocGia ASC")
    List<VanDongVienEntity> findTopMenByNationalRanking(Pageable pageable);

    /**
     * Top VĐV nữ theo xếp hạng quốc gia
     */
    @Query("SELECT v FROM VanDongVienEntity v WHERE v.gioiTinh = 'F' " +
           "AND v.trangThai = 'active' AND v.xepHangQuocGia IS NOT NULL " +
           "ORDER BY v.xepHangQuocGia ASC")
    List<VanDongVienEntity> findTopWomenByNationalRanking(Pageable pageable);

    /**
     * Bảng xếp hạng quốc gia theo giới tính với pagination
     */
    @Query("SELECT v FROM VanDongVienEntity v WHERE v.gioiTinh = :gioiTinh " +
           "AND v.trangThai = 'active' AND v.xepHangQuocGia IS NOT NULL " +
           "ORDER BY v.xepHangQuocGia ASC")
    Page<VanDongVienEntity> findNationalRankings(@Param("gioiTinh") String gioiTinh, Pageable pageable);

    /**
     * Top VĐV theo tỉ lệ thắng
     */
    @Query("SELECT v FROM VanDongVienEntity v WHERE v.trangThai = 'active' " +
           "AND v.tiLeThang IS NOT NULL AND (v.tongTranThang + v.tongTranThua) >= 10 " +
           "ORDER BY v.tiLeThang DESC")
    List<VanDongVienEntity> findTopByWinRate(Pageable pageable);

    /**
     * Top VĐV có nhiều trận thắng nhất
     */
    @Query("SELECT v FROM VanDongVienEntity v WHERE v.trangThai = 'active' " +
           "AND v.tongTranThang IS NOT NULL " +
           "ORDER BY v.tongTranThang DESC")
    List<VanDongVienEntity> findTopByWins(Pageable pageable);

    /**
     * Đếm số VĐV active
     */
    @Query("SELECT COUNT(v) FROM VanDongVienEntity v WHERE v.trangThai = 'active'")
    long countActivePlayers();

    /**
     * Đếm VĐV theo giới tính
     */
    @Query("SELECT COUNT(v) FROM VanDongVienEntity v WHERE v.gioiTinh = :gioiTinh " +
           "AND v.trangThai = 'active'")
    long countByGender(@Param("gioiTinh") String gioiTinh);

    // ========== PLAYERS MODULE QUERIES ==========

    /**
     * Search VĐV theo nhiều tiêu chí
     */
    @Query("SELECT v FROM VanDongVienEntity v WHERE v.trangThai = 'active' " +
           "AND (:hoTen IS NULL OR LOWER(v.hoTen) LIKE LOWER(CONCAT('%', :hoTen, '%'))) " +
           "AND (:gioiTinh IS NULL OR v.gioiTinh = :gioiTinh) " +
           "AND (:idClb IS NULL OR v.idClb = :idClb)")
    Page<VanDongVienEntity> searchPlayers(
            @Param("hoTen") String hoTen,
            @Param("gioiTinh") String gioiTinh,
            @Param("idClb") Integer idClb,
            Pageable pageable);

    /**
     * Lấy VĐV với thông tin CLB (native query với JOIN)
     */
    @Query(value = "SELECT v.*, c.TEN_CLB as TEN_CLB FROM VAN_DONG_VIEN v " +
                   "LEFT JOIN CAU_LAC_BO c ON v.ID_CLB = c.ID " +
                   "WHERE v.TRANG_THAI = 'active' " +
                   "ORDER BY v.XEP_HANG_QUOC_GIA ASC",
           countQuery = "SELECT COUNT(*) FROM VAN_DONG_VIEN WHERE TRANG_THAI = 'active'",
           nativeQuery = true)
    Page<Object[]> findAllPlayersWithClub(Pageable pageable);

    /**
     * Lấy chi tiết VĐV với CLB
     */
    @Query(value = "SELECT v.*, c.TEN_CLB as TEN_CLB FROM VAN_DONG_VIEN v " +
                   "LEFT JOIN CAU_LAC_BO c ON v.ID_CLB = c.ID " +
                   "WHERE v.ID = :id",
           nativeQuery = true)
    Object[] findPlayerDetailWithClub(@Param("id") Integer id);

    /**
     * VĐV mới (rising stars) - đăng ký gần đây
     */
    @Query("SELECT v FROM VanDongVienEntity v WHERE v.trangThai = 'active' " +
           "ORDER BY v.thoiGianTao DESC")
    List<VanDongVienEntity> findRecentPlayers(Pageable pageable);

    /**
     * Kiểm tra email đã tồn tại
     */
    boolean existsByEmail(String email);

    /**
     * Kiểm tra số điện thoại đã tồn tại
     */
    boolean existsByDienThoai(String dienThoai);
}
