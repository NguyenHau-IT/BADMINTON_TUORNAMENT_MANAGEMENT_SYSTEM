package com.example.btms.repository.referee;

import com.example.btms.model.referee.TrongTai;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for TrongTai (Referee) entity
 */
@Repository
public interface TrongTaiRepository extends JpaRepository<TrongTai, String> {

    /**
     * Find referee by email
     */
    Optional<TrongTai> findByEmail(String email);

    /**
     * Find referee by phone number
     */
    Optional<TrongTai> findBySoDienThoai(String soDienThoai);

    /**
     * Find all referees by club ID
     */
    List<TrongTai> findByIdClb(Integer idClb);

    /**
     * Find all referees by gender
     */
    List<TrongTai> findByGioiTinh(Boolean gioiTinh);

    /**
     * Check if email already exists
     */
    boolean existsByEmail(String email);

    /**
     * Check if phone number already exists
     */
    boolean existsBySoDienThoai(String soDienThoai);

    /**
     * Search referees by name (case-insensitive, partial match)
     */
    @Query("SELECT t FROM TrongTai t WHERE LOWER(t.hoTen) LIKE LOWER(CONCAT('%', :hoTen, '%'))")
    List<TrongTai> searchByHoTen(@Param("hoTen") String hoTen);

    /**
     * Count referees by club
     */
    long countByIdClb(Integer idClb);

    /**
     * Find all referees with no club assigned
     */
    @Query("SELECT t FROM TrongTai t WHERE t.idClb IS NULL")
    List<TrongTai> findRefereesWithoutClub();

    /**
     * Find referees by club and gender
     */
    List<TrongTai> findByIdClbAndGioiTinh(Integer idClb, Boolean gioiTinh);
}
