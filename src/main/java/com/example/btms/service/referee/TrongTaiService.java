package com.example.btms.service.referee;

import com.example.btms.model.referee.TrongTai;
import com.example.btms.repository.referee.TrongTaiRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service class for managing TrongTai (Referee) operations
 */
@Service
public class TrongTaiService {

    private static final Logger logger = LoggerFactory.getLogger(TrongTaiService.class);

    @Autowired
    private TrongTaiRepository trongTaiRepository;

    /**
     * Get all referees
     */
    public List<TrongTai> getAllTrongTai() {
        logger.info("Fetching all referees");
        return trongTaiRepository.findAll();
    }

    /**
     * Get referee by ID
     */
    public Optional<TrongTai> getTrongTaiById(String maTrongTai) {
        logger.info("Fetching referee with ID: {}", maTrongTai);
        return trongTaiRepository.findById(maTrongTai);
    }

    /**
     * Get referee by email
     */
    public Optional<TrongTai> getTrongTaiByEmail(String email) {
        logger.info("Fetching referee with email: {}", email);
        return trongTaiRepository.findByEmail(email);
    }

    /**
     * Get referee by phone number
     */
    public Optional<TrongTai> getTrongTaiBySoDienThoai(String soDienThoai) {
        logger.info("Fetching referee with phone: {}", soDienThoai);
        return trongTaiRepository.findBySoDienThoai(soDienThoai);
    }

    /**
     * Get all referees by club
     */
    public List<TrongTai> getTrongTaiByClub(Integer idClb) {
        logger.info("Fetching referees for club ID: {}", idClb);
        return trongTaiRepository.findByIdClb(idClb);
    }

    /**
     * Get all referees by gender
     */
    public List<TrongTai> getTrongTaiByGioiTinh(Boolean gioiTinh) {
        String gender = (gioiTinh != null && gioiTinh) ? "Nam" : "Nữ";
        logger.info("Fetching referees with gender: {}", gender);
        return trongTaiRepository.findByGioiTinh(gioiTinh);
    }

    /**
     * Search referees by name
     */
    public List<TrongTai> searchTrongTaiByName(String hoTen) {
        logger.info("Searching referees with name containing: {}", hoTen);
        return trongTaiRepository.searchByHoTen(hoTen);
    }

    /**
     * Get referees without club
     */
    public List<TrongTai> getRefereesWithoutClub() {
        logger.info("Fetching referees without club assignment");
        return trongTaiRepository.findRefereesWithoutClub();
    }

    /**
     * Create new referee
     */
    @Transactional
    public TrongTai createTrongTai(TrongTai trongTai) {
        logger.info("Creating new referee: {}", trongTai.getMaTrongTai());

        // Validate unique constraints
        if (trongTai.getEmail() != null && trongTaiRepository.existsByEmail(trongTai.getEmail())) {
            throw new IllegalArgumentException("Email đã tồn tại: " + trongTai.getEmail());
        }

        if (trongTai.getSoDienThoai() != null && trongTaiRepository.existsBySoDienThoai(trongTai.getSoDienThoai())) {
            throw new IllegalArgumentException("Số điện thoại đã tồn tại: " + trongTai.getSoDienThoai());
        }

        TrongTai saved = trongTaiRepository.save(trongTai);
        logger.info("✅ Referee created successfully - ID: {}", saved.getMaTrongTai());
        return saved;
    }

    /**
     * Update referee
     */
    @Transactional
    public TrongTai updateTrongTai(String maTrongTai, TrongTai trongTai) {
        logger.info("Updating referee: {}", maTrongTai);

        Optional<TrongTai> existingOpt = trongTaiRepository.findById(maTrongTai);
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

        TrongTai updated = trongTaiRepository.save(existing);
        logger.info("✅ Referee updated successfully - ID: {}", updated.getMaTrongTai());
        return updated;
    }

    /**
     * Delete referee
     */
    @Transactional
    public void deleteTrongTai(String maTrongTai) {
        logger.info("Deleting referee: {}", maTrongTai);

        if (!trongTaiRepository.existsById(maTrongTai)) {
            throw new IllegalArgumentException("Không tìm thấy trọng tài với mã: " + maTrongTai);
        }

        trongTaiRepository.deleteById(maTrongTai);
        logger.info("✅ Referee deleted successfully - ID: {}", maTrongTai);
    }

    /**
     * Check if referee exists by ID
     */
    public boolean existsById(String maTrongTai) {
        return trongTaiRepository.existsById(maTrongTai);
    }

    /**
     * Count total referees
     */
    public long countTotalReferees() {
        return trongTaiRepository.count();
    }

    /**
     * Count referees by club
     */
    public long countRefereesByClub(Integer idClb) {
        return trongTaiRepository.countByIdClb(idClb);
    }

    /**
     * Get referees by club and gender
     */
    public List<TrongTai> getTrongTaiByClubAndGender(Integer idClb, Boolean gioiTinh) {
        logger.info("Fetching referees for club: {} with gender: {}", idClb, gioiTinh);
        return trongTaiRepository.findByIdClbAndGioiTinh(idClb, gioiTinh);
    }
}
