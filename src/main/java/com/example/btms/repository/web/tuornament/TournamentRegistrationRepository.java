package com.example.btms.repository.web.tuornament;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.btms.model.tournament.TournamentRegistration;

import java.util.List;
import java.util.Optional;

@Repository
public interface TournamentRegistrationRepository extends JpaRepository<TournamentRegistration, Integer> {
    
    /**
     * Find registration by tournament ID (ID_GIAI_DAU) and email
     */
    Optional<TournamentRegistration> findByTournamentIdAndEmail(Integer tournamentId, String email);
    
    /**
     * Find all registrations for a tournament
     */
    List<TournamentRegistration> findByTournamentId(Integer tournamentId);
    
    /**
     * Find registration by code (MA_DANG_KY)
     */
    Optional<TournamentRegistration> findByRegistrationCode(String registrationCode);
    
    /**
     * Count registrations for a tournament
     */
    long countByTournamentId(Integer tournamentId);
    
    /**
     * Count registrations by status (TRANG_THAI)
     */
    long countByTournamentIdAndTrangThai(Integer tournamentId, String trangThai);
    
    /**
     * Check if email already registered for tournament
     * Excludes cancelled and rejected registrations
     */
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END " +
           "FROM TournamentRegistration r " +
           "WHERE r.tournamentId = :tournamentId " +
           "AND r.email = :email " +
           "AND r.trangThai NOT IN ('cancelled', 'rejected')")
    boolean existsByTournamentIdAndEmail(@Param("tournamentId") Integer tournamentId, 
                                        @Param("email") String email);
}
