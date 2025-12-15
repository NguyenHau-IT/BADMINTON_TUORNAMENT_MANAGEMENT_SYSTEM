package com.example.btms.service.referee;

import com.example.btms.model.referee.TrongTai;
import com.example.btms.repository.referee.TrongTaiRepository;
import com.example.btms.service.db.DatabaseService;
import com.example.btms.model.db.SQLSRVConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Service class for managing TrongTai (Referee) operations using JDBC
 */
@Service
public class TrongTaiService {

    private static final Logger logger = LoggerFactory.getLogger(TrongTaiService.class);
    private final SQLSRVConnectionManager manager = new SQLSRVConnectionManager();
    private final DatabaseService databaseService = new DatabaseService(manager);
    private TrongTaiRepository repository;

    public TrongTaiService() {
        initializeRepository();
    }

    /**
     * Get DatabaseService để MainFrame có thể set config
     */
    public DatabaseService getDatabaseService() {
        return databaseService;
    }

    /**
     * Khởi tạo repository với connection hiện tại
     */
    private void initializeRepository() {
        Connection connection = databaseService.current();
        if (connection != null) {
            this.repository = new TrongTaiRepository(connection);
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
            repository = new TrongTaiRepository(connection);
        }
    }

    /**
     * Get all referees
     */
    public List<TrongTai> getAllTrongTai() {
        logger.info("Fetching all referees");
        try {
            ensureRepository();
            return repository.findAll();
        } catch (SQLException e) {
            logger.error("Error fetching all referees", e);
            throw new RuntimeException("Failed to fetch referees", e);
        }
    }

    /**
     * Get referee by ID
     */
    public Optional<TrongTai> getTrongTaiById(String maTrongTai) {
        logger.info("Fetching referee with ID: {}", maTrongTai);
        try {
            ensureRepository();
            return repository.findById(maTrongTai);
        } catch (SQLException e) {
            logger.error("Error fetching referee by ID: " + maTrongTai, e);
            throw new RuntimeException("Failed to fetch referee", e);
        }
    }

    /**
     * Get referee by email
     */
    public Optional<TrongTai> getTrongTaiByEmail(String email) {
        logger.info("Fetching referee with email: {}", email);
        try {
            ensureRepository();
            return repository.findByEmail(email);
        } catch (SQLException e) {
            logger.error("Error fetching referee by email: " + email, e);
            throw new RuntimeException("Failed to fetch referee", e);
        }
    }

    /**
     * Create new referee
     */
    public TrongTai createTrongTai(TrongTai trongTai) {
        logger.info("Creating new referee: {}", trongTai.getMaTrongTai());

        try {
            ensureRepository();

            // Validate unique constraints
            if (trongTai.getEmail() != null && repository.existsByEmail(trongTai.getEmail())) {
                throw new IllegalArgumentException("Email đã tồn tại: " + trongTai.getEmail());
            }

            TrongTai saved = repository.save(trongTai);
            logger.info("✅ Referee created successfully - ID: {}", saved.getMaTrongTai());
            return saved;
        } catch (SQLException e) {
            logger.error("Error creating referee", e);
            throw new RuntimeException("Failed to create referee", e);
        }
    }

    /**
     * Update referee
     */
    public TrongTai updateTrongTai(String maTrongTai, TrongTai trongTai) {
        logger.info("Updating referee: {}", maTrongTai);

        try {
            ensureRepository();

            Optional<TrongTai> existingOpt = repository.findById(maTrongTai);
            if (existingOpt.isEmpty()) {
                throw new IllegalArgumentException("Không tìm thấy trọng tài với mã: " + maTrongTai);
            }

            TrongTai existing = existingOpt.get();

            // Update fields
            if (trongTai.getHoTen() != null) {
                existing.setHoTen(trongTai.getHoTen());
            }
            if (trongTai.getNgaySinh() != null) {
                existing.setNgaySinh(trongTai.getNgaySinh());
            }
            if (trongTai.getGioiTinh() != null) {
                existing.setGioiTinh(trongTai.getGioiTinh());
            }
            if (trongTai.getSoDienThoai() != null) {
                existing.setSoDienThoai(trongTai.getSoDienThoai());
            }
            if (trongTai.getEmail() != null) {
                existing.setEmail(trongTai.getEmail());
            }
            if (trongTai.getMatKhau() != null) {
                existing.setMatKhau(trongTai.getMatKhau());
            }
            if (trongTai.getIdClb() != null) {
                existing.setIdClb(trongTai.getIdClb());
            }
            if (trongTai.getGhiChu() != null) {
                existing.setGhiChu(trongTai.getGhiChu());
            }

            TrongTai updated = repository.save(existing);
            logger.info("✅ Referee updated successfully - ID: {}", updated.getMaTrongTai());
            return updated;
        } catch (SQLException e) {
            logger.error("Error updating referee", e);
            throw new RuntimeException("Failed to update referee", e);
        }
    }

    /**
     * Delete referee
     */
    public void deleteTrongTai(String maTrongTai) {
        logger.info("Deleting referee: {}", maTrongTai);

        try {
            ensureRepository();

            if (!repository.existsById(maTrongTai)) {
                throw new IllegalArgumentException("Không tìm thấy trọng tài với mã: " + maTrongTai);
            }

            repository.deleteById(maTrongTai);
            logger.info("✅ Referee deleted successfully - ID: {}", maTrongTai);
        } catch (SQLException e) {
            logger.error("Error deleting referee", e);
            throw new RuntimeException("Failed to delete referee", e);
        }
    }

    // Legacy methods để maintain compatibility với existing code
    public List<TrongTai> findAll() {
        return getAllTrongTai();
    }

    public Optional<TrongTai> findById(String id) {
        return getTrongTaiById(id);
    }

    public Optional<TrongTai> findByEmail(String email) {
        return getTrongTaiByEmail(email);
    }
}
