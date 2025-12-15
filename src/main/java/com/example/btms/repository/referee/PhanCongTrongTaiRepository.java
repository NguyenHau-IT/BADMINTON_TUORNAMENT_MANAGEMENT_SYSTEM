package com.example.btms.repository.referee;

import com.example.btms.model.referee.PhanCongTrongTai;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for PhanCongTrongTai (Referee Assignment) entity
 */
@Repository
public interface PhanCongTrongTaiRepository extends JpaRepository<PhanCongTrongTai, String> {

    /**
     * Find all assignments for a specific referee
     */
    List<PhanCongTrongTai> findByMaTrongTai(String maTrongTai);

    /**
     * Find all referees assigned to a specific match
     */
    List<PhanCongTrongTai> findByMaTranDau(String maTranDau);

    /**
     * Find by referee and match
     */
    Optional<PhanCongTrongTai> findByMaTrongTaiAndMaTranDau(String maTrongTai, String maTranDau);

    /**
     * Find by role
     */
    List<PhanCongTrongTai> findByVaiTro(String vaiTro);

    /**
     * Find by match and role
     */
    List<PhanCongTrongTai> findByMaTranDauAndVaiTro(String maTranDau, String vaiTro);

    /**
     * Count assignments for a specific referee
     */
    long countByMaTrongTai(String maTrongTai);

    /**
     * Count referees assigned to a specific match
     */
    long countByMaTranDau(String maTranDau);

    /**
     * Check if a referee is already assigned to a match
     */
    boolean existsByMaTrongTaiAndMaTranDau(String maTrongTai, String maTranDau);

    /**
     * Delete all assignments for a specific referee
     */
    void deleteByMaTrongTai(String maTrongTai);

    /**
     * Delete all assignments for a specific match
     */
    void deleteByMaTranDau(String maTranDau);

    /**
     * Find all referees not assigned to a specific match
     */
    @Query("SELECT tt.maTrongTai FROM TrongTai tt " +
            "WHERE tt.maTrongTai NOT IN " +
            "(SELECT pc.maTrongTai FROM PhanCongTrongTai pc WHERE pc.maTranDau = :maTranDau)")
    List<String> findUnassignedRefereesForMatch(@Param("maTranDau") String maTranDau);

    /**
     * Find all matches assigned to a referee with details
     */
    @Query("SELECT pc FROM PhanCongTrongTai pc WHERE pc.maTrongTai = :maTrongTai ORDER BY pc.maTranDau")
    List<PhanCongTrongTai> findMatchesByReferee(@Param("maTrongTai") String maTrongTai);

    /**
     * Find all referees assigned to a match with details
     */
    @Query("SELECT pc FROM PhanCongTrongTai pc WHERE pc.maTranDau = :maTranDau ORDER BY pc.maTrongTai")
    List<PhanCongTrongTai> findRefereesByMatch(@Param("maTranDau") String maTranDau);
}
