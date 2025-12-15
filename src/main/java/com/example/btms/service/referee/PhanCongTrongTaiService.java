package com.example.btms.service.referee;

import com.example.btms.model.referee.PhanCongTrongTai;
import com.example.btms.repository.referee.PhanCongTrongTaiRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service class for managing PhanCongTrongTai (Referee Assignment) operations
 */
@Service
public class PhanCongTrongTaiService {

    private static final Logger logger = LoggerFactory.getLogger(PhanCongTrongTaiService.class);

    @Autowired
    private PhanCongTrongTaiRepository phanCongRepository;

    /**
     * Get all referee assignments
     */
    public List<PhanCongTrongTai> getAllAssignments() {
        logger.info("Fetching all referee assignments");
        return phanCongRepository.findAll();
    }

    /**
     * Get assignment by ID
     */
    public Optional<PhanCongTrongTai> getAssignmentById(String maPhanCong) {
        logger.info("Fetching assignment by ID: {}", maPhanCong);
        return phanCongRepository.findById(maPhanCong);
    }

    /**
     * Get assignment by referee and match
     */
    public Optional<PhanCongTrongTai> getAssignment(String maTrongTai, String maTranDau) {
        logger.info("Fetching assignment for referee: {} and match: {}", maTrongTai, maTranDau);
        return phanCongRepository.findByMaTrongTaiAndMaTranDau(maTrongTai, maTranDau);
    }

    /**
     * Get all assignments for a specific referee
     */
    public List<PhanCongTrongTai> getAssignmentsByReferee(String maTrongTai) {
        logger.info("Fetching assignments for referee: {}", maTrongTai);
        return phanCongRepository.findByMaTrongTai(maTrongTai);
    }

    /**
     * Get all referees assigned to a specific match
     */
    public List<PhanCongTrongTai> getAssignmentsByMatch(String maTranDau) {
        logger.info("Fetching assignments for match: {}", maTranDau);
        return phanCongRepository.findByMaTranDau(maTranDau);
    }

    /**
     * Get assignments by role
     */
    public List<PhanCongTrongTai> getAssignmentsByRole(String vaiTro) {
        logger.info("Fetching assignments for role: {}", vaiTro);
        return phanCongRepository.findByVaiTro(vaiTro);
    }

    /**
     * Get assignments by match and role
     */
    public List<PhanCongTrongTai> getAssignmentsByMatchAndRole(String maTranDau, String vaiTro) {
        logger.info("Fetching assignments for match: {} and role: {}", maTranDau, vaiTro);
        return phanCongRepository.findByMaTranDauAndVaiTro(maTranDau, vaiTro);
    }

    /**
     * Count assignments for a specific referee
     */
    public long countAssignmentsByReferee(String maTrongTai) {
        return phanCongRepository.countByMaTrongTai(maTrongTai);
    }

    /**
     * Count referees assigned to a specific match
     */
    public long countAssignmentsByMatch(String maTranDau) {
        return phanCongRepository.countByMaTranDau(maTranDau);
    }

    /**
     * Check if a referee is already assigned to a match
     */
    public boolean isRefereeAssignedToMatch(String maTrongTai, String maTranDau) {
        return phanCongRepository.existsByMaTrongTaiAndMaTranDau(maTrongTai, maTranDau);
    }

    /**
     * Create new assignment
     */
    @Transactional
    public PhanCongTrongTai createAssignment(PhanCongTrongTai phanCong) {
        logger.info("Creating new assignment: {}", phanCong.getMaPhanCong());

        // Validate unique constraint
        if (isRefereeAssignedToMatch(phanCong.getMaTrongTai(), phanCong.getMaTranDau())) {
            throw new IllegalStateException("Trọng tài đã được phân công cho trận đấu này");
        }

        PhanCongTrongTai saved = phanCongRepository.save(phanCong);
        logger.info("✅ Assignment created successfully - ID: {}", saved.getMaPhanCong());
        return saved;
    }

    /**
     * Assign a referee to a match
     */
    @Transactional
    public PhanCongTrongTai assignRefereeToMatch(String maTrongTai, String maTranDau, String vaiTro) {
        logger.info("Assigning referee {} to match {} with role {}", maTrongTai, maTranDau, vaiTro);

        // Check if already assigned
        if (isRefereeAssignedToMatch(maTrongTai, maTranDau)) {
            throw new IllegalStateException("Trọng tài đã được phân công cho trận đấu này");
        }

        String maPhanCong = "PC_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        PhanCongTrongTai assignment = new PhanCongTrongTai(maPhanCong, maTrongTai, maTranDau, vaiTro);
        PhanCongTrongTai saved = phanCongRepository.save(assignment);
        logger.info("✅ Assignment created successfully");
        return saved;
    }

    /**
     * Assign multiple referees to a match
     */
    @Transactional
    public List<PhanCongTrongTai> assignRefereesToMatch(List<String> maTrongTaiList, String maTranDau, String vaiTro) {
        logger.info("Assigning {} referees to match {}", maTrongTaiList.size(), maTranDau);

        List<PhanCongTrongTai> assignments = maTrongTaiList.stream()
                .filter(maTrongTai -> !isRefereeAssignedToMatch(maTrongTai, maTranDau))
                .map(maTrongTai -> {
                    String maPhanCong = "PC_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                    return new PhanCongTrongTai(maPhanCong, maTrongTai, maTranDau, vaiTro);
                })
                .toList();

        List<PhanCongTrongTai> saved = phanCongRepository.saveAll(assignments);
        logger.info("✅ {} assignments created successfully", saved.size());
        return saved;
    }

    /**
     * Update assignment
     */
    @Transactional
    public PhanCongTrongTai updateAssignment(String maPhanCong, PhanCongTrongTai phanCong) {
        logger.info("Updating assignment: {}", maPhanCong);

        Optional<PhanCongTrongTai> existingOpt = phanCongRepository.findById(maPhanCong);
        if (existingOpt.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy phân công với mã: " + maPhanCong);
        }

        PhanCongTrongTai existing = existingOpt.get();

        if (phanCong.getVaiTro() != null) {
            existing.setVaiTro(phanCong.getVaiTro());
        }
        if (phanCong.getGhiChu() != null) {
            existing.setGhiChu(phanCong.getGhiChu());
        }

        PhanCongTrongTai updated = phanCongRepository.save(existing);
        logger.info("✅ Assignment updated successfully - ID: {}", updated.getMaPhanCong());
        return updated;
    }

    /**
     * Remove an assignment by ID
     */
    @Transactional
    public void removeAssignment(String maPhanCong) {
        logger.info("Removing assignment: {}", maPhanCong);

        if (!phanCongRepository.existsById(maPhanCong)) {
            throw new IllegalArgumentException("Không tìm thấy phân công này");
        }

        phanCongRepository.deleteById(maPhanCong);
        logger.info("✅ Assignment removed successfully");
    }

    /**
     * Remove all assignments for a specific referee
     */
    @Transactional
    public void removeAllAssignmentsByReferee(String maTrongTai) {
        logger.info("Removing all assignments for referee: {}", maTrongTai);
        phanCongRepository.deleteByMaTrongTai(maTrongTai);
        logger.info("✅ All assignments removed successfully");
    }

    /**
     * Remove all assignments for a specific match
     */
    @Transactional
    public void removeAllAssignmentsByMatch(String maTranDau) {
        logger.info("Removing all assignments for match: {}", maTranDau);
        phanCongRepository.deleteByMaTranDau(maTranDau);
        logger.info("✅ All assignments removed successfully");
    }

    /**
     * Get all referees not assigned to a specific match
     */
    public List<String> getUnassignedRefereesForMatch(String maTranDau) {
        logger.info("Fetching unassigned referees for match: {}", maTranDau);
        return phanCongRepository.findUnassignedRefereesForMatch(maTranDau);
    }

    /**
     * Get detailed assignments for a referee
     */
    public List<PhanCongTrongTai> getMatchesByReferee(String maTrongTai) {
        logger.info("Fetching detailed matches for referee: {}", maTrongTai);
        return phanCongRepository.findMatchesByReferee(maTrongTai);
    }

    /**
     * Get detailed assignments for a match
     */
    public List<PhanCongTrongTai> getRefereesByMatch(String maTranDau) {
        logger.info("Fetching detailed referees for match: {}", maTranDau);
        return phanCongRepository.findRefereesByMatch(maTranDau);
    }

    /**
     * Replace all referees for a match
     */
    @Transactional
    public List<PhanCongTrongTai> replaceRefereesForMatch(String maTranDau, List<String> newMaTrongTaiList,
            String vaiTro) {
        logger.info("Replacing all referees for match: {}", maTranDau);

        // Remove all existing assignments
        removeAllAssignmentsByMatch(maTranDau);

        // Assign new referees
        return assignRefereesToMatch(newMaTrongTaiList, maTranDau, vaiTro);
    }
}
