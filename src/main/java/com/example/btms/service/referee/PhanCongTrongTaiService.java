package com.example.btms.service.referee;

import com.example.btms.model.referee.PhanCongTrongTai;
import com.example.btms.repository.referee.PhanCongTrongTaiRepository;
import com.example.btms.service.db.DatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service class for managing PhanCongTrongTai (Referee Assignment) operations
 * using JDBC
 */
@Service
public class PhanCongTrongTaiService {

    private static final Logger logger = LoggerFactory.getLogger(PhanCongTrongTaiService.class);
    private final DatabaseService databaseService;
    private PhanCongTrongTaiRepository repository;

    public PhanCongTrongTaiService(DatabaseService databaseService) {
        this.databaseService = databaseService;
        initializeRepository();
    }

    /**
     * Khởi tạo repository với connection hiện tại
     */
    private void initializeRepository() {
        Connection connection = databaseService.current();
        if (connection != null) {
            this.repository = new PhanCongTrongTaiRepository(connection);
        }
    }

    /**
     * Đảm bảo repository được khởi tạo
     */
    private void ensureRepository() throws SQLException {
        if (repository == null) {
            Connection connection = databaseService.current();
            if (connection == null) {
                throw new SQLException("No database connection available");
            }
            repository = new PhanCongTrongTaiRepository(connection);
        }
    }

    /**
     * Get all referee assignments
     */
    public List<PhanCongTrongTai> getAllAssignments() {
        logger.info("Fetching all referee assignments");
        try {
            ensureRepository();
            return repository.findAll();
        } catch (SQLException e) {
            logger.error("Error fetching all assignments", e);
            throw new RuntimeException("Failed to fetch assignments", e);
        }
    }

    /**
     * Get assignment by ID
     */
    public Optional<PhanCongTrongTai> getAssignmentById(String maPhanCong) {
        logger.info("Fetching assignment by ID: {}", maPhanCong);
        try {
            ensureRepository();
            return repository.findById(maPhanCong);
        } catch (SQLException e) {
            logger.error("Error fetching assignment by ID: " + maPhanCong, e);
            throw new RuntimeException("Failed to fetch assignment", e);
        }
    }

    /**
     * Get assignment by referee and match
     */
    public Optional<PhanCongTrongTai> getAssignment(String maTrongTai, String maTranDau) {
        logger.info("Fetching assignment for referee: {} and match: {}", maTrongTai, maTranDau);
        try {
            ensureRepository();
            return repository.findByMaTrongTaiAndMaTranDau(maTrongTai, maTranDau);
        } catch (SQLException e) {
            logger.error("Error fetching assignment for referee: " + maTrongTai + " and match: " + maTranDau, e);
            throw new RuntimeException("Failed to fetch assignment", e);
        }
    }

    /**
     * Get all assignments for a specific referee
     */
    public List<PhanCongTrongTai> getAssignmentsByReferee(String maTrongTai) {
        logger.info("Fetching assignments for referee: {}", maTrongTai);
        try {
            ensureRepository();
            return repository.findByMaTrongTai(maTrongTai);
        } catch (SQLException e) {
            logger.error("Error fetching assignments for referee: " + maTrongTai, e);
            throw new RuntimeException("Failed to fetch assignments", e);
        }
    }

    /**
     * Get all referees assigned to a specific match
     */
    public List<PhanCongTrongTai> getAssignmentsByMatch(String maTranDau) {
        logger.info("Fetching assignments for match: {}", maTranDau);
        try {
            ensureRepository();
            return repository.findByMaTranDau(maTranDau);
        } catch (SQLException e) {
            logger.error("Error fetching assignments for match: " + maTranDau, e);
            throw new RuntimeException("Failed to fetch assignments", e);
        }
    }

    /**
     * Get assignments by role
     */
    public List<PhanCongTrongTai> getAssignmentsByRole(String vaiTro) {
        logger.info("Fetching assignments for role: {}", vaiTro);
        try {
            ensureRepository();
            return repository.findByVaiTro(vaiTro);
        } catch (SQLException e) {
            logger.error("Error fetching assignments for role: " + vaiTro, e);
            throw new RuntimeException("Failed to fetch assignments", e);
        }
    }

    /**
     * Count assignments for a specific referee
     */
    public long countAssignmentsByReferee(String maTrongTai) {
        try {
            ensureRepository();
            return repository.countByMaTrongTai(maTrongTai);
        } catch (SQLException e) {
            logger.error("Error counting assignments for referee: " + maTrongTai, e);
            throw new RuntimeException("Failed to count assignments", e);
        }
    }

    /**
     * Count referees assigned to a specific match
     */
    public long countAssignmentsByMatch(String maTranDau) {
        try {
            ensureRepository();
            return repository.countByMaTranDau(maTranDau);
        } catch (SQLException e) {
            logger.error("Error counting assignments for match: " + maTranDau, e);
            throw new RuntimeException("Failed to count assignments", e);
        }
    }

    /**
     * Check if a referee is already assigned to a match
     */
    public boolean isRefereeAssignedToMatch(String maTrongTai, String maTranDau) {
        try {
            ensureRepository();
            return repository.existsByMaTrongTaiAndMaTranDau(maTrongTai, maTranDau);
        } catch (SQLException e) {
            logger.error("Error checking referee assignment", e);
            throw new RuntimeException("Failed to check assignment", e);
        }
    }

    /**
     * Create new referee assignment
     */
    public PhanCongTrongTai createAssignment(PhanCongTrongTai assignment) {
        logger.info("Creating new referee assignment: {}", assignment.getMaPhanCong());

        try {
            ensureRepository();

            // Validate that referee is not already assigned to this match
            if (repository.existsByMaTrongTaiAndMaTranDau(assignment.getMaTrongTai(), assignment.getMaTranDau())) {
                throw new IllegalArgumentException(
                        "Trọng tài " + assignment.getMaTrongTai() + " đã được phân công vào trận "
                                + assignment.getMaTranDau());
            }

            // Generate ID if not provided
            if (assignment.getMaPhanCong() == null || assignment.getMaPhanCong().isEmpty()) {
                assignment.setMaPhanCong(UUID.randomUUID().toString());
            }

            PhanCongTrongTai saved = repository.save(assignment);
            logger.info("✅ Assignment created successfully - ID: {}", saved.getMaPhanCong());
            return saved;
        } catch (SQLException e) {
            logger.error("Error creating assignment", e);
            throw new RuntimeException("Failed to create assignment", e);
        }
    }

    /**
     * Delete assignment by ID
     */
    public void deleteAssignment(String maPhanCong) {
        logger.info("Deleting assignment: {}", maPhanCong);
        try {
            ensureRepository();
            repository.deleteById(maPhanCong);
            logger.info("✅ Assignment deleted successfully - ID: {}", maPhanCong);
        } catch (SQLException e) {
            logger.error("Error deleting assignment", e);
            throw new RuntimeException("Failed to delete assignment", e);
        }
    }

    /**
     * Delete all assignments for a referee
     */
    public void deleteAssignmentsByReferee(String maTrongTai) {
        logger.info("Deleting all assignments for referee: {}", maTrongTai);
        try {
            ensureRepository();
            repository.deleteByMaTrongTai(maTrongTai);
            logger.info("✅ All assignments for referee {} deleted successfully", maTrongTai);
        } catch (SQLException e) {
            logger.error("Error deleting assignments for referee", e);
            throw new RuntimeException("Failed to delete assignments", e);
        }
    }

    /**
     * Delete all assignments for a match
     */
    public void deleteAssignmentsByMatch(String maTranDau) {
        logger.info("Deleting all assignments for match: {}", maTranDau);
        try {
            ensureRepository();
            repository.deleteByMaTranDau(maTranDau);
            logger.info("✅ All assignments for match {} deleted successfully", maTranDau);
        } catch (SQLException e) {
            logger.error("Error deleting assignments for match", e);
            throw new RuntimeException("Failed to delete assignments", e);
        }
    }
}
