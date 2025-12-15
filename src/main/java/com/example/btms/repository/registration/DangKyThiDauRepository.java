package com.example.btms.repository.registration;

import com.example.btms.model.registration.DangKyThiDau;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for DangKyThiDau entity
 * Handles database operations for tournament registrations
 * 
 * @author BTMS Team
 * @version 1.0
 * @since 2025-11-22
 */
@Repository
public interface DangKyThiDauRepository extends JpaRepository<DangKyThiDau, Integer> {
    
    /**
     * Find registration by registration code
     */
    Optional<DangKyThiDau> findByMaDangKy(String maDangKy);
    
    /**
     * Check if email already registered for a tournament
     * (excluding cancelled/rejected registrations)
     */
    @Query("SELECT COUNT(d) > 0 FROM DangKyThiDau d " +
           "WHERE d.idGiaiDau = :idGiaiDau " +
           "AND d.email = :email " +
           "AND d.trangThai NOT IN ('cancelled', 'rejected')")
    boolean existsByIdGiaiDauAndEmail(@Param("idGiaiDau") Integer idGiaiDau, 
                                       @Param("email") String email);
    
    /**
     * Find all registrations for a tournament
     */
    List<DangKyThiDau> findByIdGiaiDauOrderByNgayTaoDesc(Integer idGiaiDau);
    
    /**
     * Find registrations by tournament and status
     */
    List<DangKyThiDau> findByIdGiaiDauAndTrangThaiOrderByNgayTaoDesc(Integer idGiaiDau, String trangThai);
    
    /**
     * Find registrations by email
     */
    List<DangKyThiDau> findByEmailOrderByNgayTaoDesc(String email);
    
    /**
     * Count registrations for a tournament
     */
    long countByIdGiaiDau(Integer idGiaiDau);
    
    /**
     * Count registrations by status for a tournament
     */
    long countByIdGiaiDauAndTrangThai(Integer idGiaiDau, String trangThai);
    
    /**
     * Find registrations by tournament and payment status
     */
    List<DangKyThiDau> findByIdGiaiDauAndTrangThaiThanhToanOrderByNgayTaoDesc(
            Integer idGiaiDau, String trangThaiThanhToan);
    
    /**
     * Count pending registrations for a tournament
     */
    @Query("SELECT COUNT(d) FROM DangKyThiDau d " +
           "WHERE d.idGiaiDau = :idGiaiDau " +
           "AND d.trangThai = 'pending'")
    long countPendingRegistrations(@Param("idGiaiDau") Integer idGiaiDau);
}
