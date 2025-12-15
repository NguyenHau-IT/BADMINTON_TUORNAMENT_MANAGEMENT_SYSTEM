package com.example.btms.repository.web.club;

import com.example.btms.model.club.CauLacBoEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository cho CAU_LAC_BO
 * Dùng cho Rankings module
 * 
 * @author BTMS Team
 * @version 2.0 - Web Platform
 */
@Repository
public interface CauLacBoRepository extends JpaRepository<CauLacBoEntity, Integer> {

    // ========== BASIC QUERIES ==========
    
    /**
     * Tìm CLB theo tên (LIKE search)
     */
    List<CauLacBoEntity> findByTenClbContainingIgnoreCase(String tenClb);
    
    Page<CauLacBoEntity> findByTenClbContainingIgnoreCase(String tenClb, Pageable pageable);

    /**
     * Tìm CLB theo tên ngắn
     */
    CauLacBoEntity findByTenNgan(String tenNgan);

    // ========== RANKINGS QUERIES ==========

    /**
     * Lấy danh sách CLB với số thành viên
     */
    @Query(value = "SELECT c.ID, c.TEN_CLB, c.TEN_NGAN, COUNT(v.ID) as SO_THANH_VIEN " +
                   "FROM CAU_LAC_BO c " +
                   "LEFT JOIN VAN_DONG_VIEN v ON c.ID = v.ID_CLB AND v.TRANG_THAI = 'active' " +
                   "GROUP BY c.ID, c.TEN_CLB, c.TEN_NGAN " +
                   "ORDER BY SO_THANH_VIEN DESC",
           countQuery = "SELECT COUNT(*) FROM CAU_LAC_BO",
           nativeQuery = true)
    Page<Object[]> findAllClubsWithMemberCount(Pageable pageable);

    /**
     * Top CLB có nhiều thành viên nhất
     */
    @Query(value = "SELECT TOP (:limit) c.ID, c.TEN_CLB, c.TEN_NGAN, COUNT(v.ID) as SO_THANH_VIEN " +
                   "FROM CAU_LAC_BO c " +
                   "LEFT JOIN VAN_DONG_VIEN v ON c.ID = v.ID_CLB AND v.TRANG_THAI = 'active' " +
                   "GROUP BY c.ID, c.TEN_CLB, c.TEN_NGAN " +
                   "ORDER BY SO_THANH_VIEN DESC",
           nativeQuery = true)
    List<Object[]> findTopClubsByMemberCount(@Param("limit") int limit);

    /**
     * CLB có nhiều VĐV top nhất (VĐV có xếp hạng quốc gia <= 100)
     */
    @Query(value = "SELECT c.ID, c.TEN_CLB, c.TEN_NGAN, " +
                   "COUNT(CASE WHEN v.XEP_HANG_QUOC_GIA <= 100 THEN 1 END) as TOP_PLAYERS, " +
                   "COUNT(v.ID) as TOTAL_PLAYERS " +
                   "FROM CAU_LAC_BO c " +
                   "LEFT JOIN VAN_DONG_VIEN v ON c.ID = v.ID_CLB AND v.TRANG_THAI = 'active' " +
                   "GROUP BY c.ID, c.TEN_CLB, c.TEN_NGAN " +
                   "HAVING COUNT(CASE WHEN v.XEP_HANG_QUOC_GIA <= 100 THEN 1 END) > 0 " +
                   "ORDER BY TOP_PLAYERS DESC",
           nativeQuery = true)
    List<Object[]> findClubsWithTopPlayers();

    /**
     * Đếm tổng số CLB
     */
    @Query("SELECT COUNT(c) FROM CauLacBoEntity c")
    long countAllClubs();

    /**
     * Tìm CLB có VĐV tham gia giải đấu cụ thể
     */
    @Query(value = "SELECT DISTINCT c.ID, c.TEN_CLB, c.TEN_NGAN " +
                   "FROM CAU_LAC_BO c " +
                   "INNER JOIN VAN_DONG_VIEN v ON c.ID = v.ID_CLB " +
                   "INNER JOIN DANG_KI_CA_NHAN dk ON v.ID = dk.ID_VDV " +
                   "WHERE dk.ID_GIAI = :idGiai",
           nativeQuery = true)
    List<Object[]> findClubsInTournament(@Param("idGiai") Integer idGiai);
}
