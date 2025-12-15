package com.example.btms.service.registration;

import com.example.btms.web.dto.TournamentRegistrationRequest;
import com.example.btms.model.registration.DangKyThiDau;
import com.example.btms.repository.registration.DangKyThiDauRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Optional;

/**
 * Service for handling tournament registrations
 * Business logic for DANG_KY_THI_DAU operations
 * 
 * @author BTMS Team
 * @version 1.0
 * @since 2025-11-22
 */
@Service
@Transactional
public class RegistrationService {
    
    private static final Logger logger = LoggerFactory.getLogger(RegistrationService.class);
    
    @Autowired
    private DangKyThiDauRepository registrationRepository;
    
    /**
     * Create new tournament registration
     */
    public DangKyThiDau createRegistration(TournamentRegistrationRequest request) {
        logger.info("Creating registration for tournament {} - email: {}", 
                    request.getTournamentId(), request.getEmail());
        
        // Validate request
        validateRegistrationRequest(request);
        
        // Check duplicate registration
        if (isAlreadyRegistered(request.getTournamentId(), request.getEmail())) {
            throw new IllegalStateException("Email này đã đăng ký cho giải đấu");
        }
        
        // Generate registration code
        String registrationCode = generateRegistrationCode(request.getTournamentId());
        
        // Create entity
        DangKyThiDau registration = new DangKyThiDau();
        
        // Map basic info
        registration.setIdGiaiDau(request.getTournamentId());
        registration.setHoTen(request.getHoTen());
        registration.setEmail(request.getEmail());
        registration.setDienThoai(request.getDienThoai());
        registration.setNgaySinh(LocalDate.parse(request.getNgaySinh()));
        registration.setGioiTinh(request.getGioiTinh());
        registration.setCccd(request.getCccd());
        
        // Map registration details
        registration.setNoiDung(request.getNoiDung());
        registration.setTrinhDo(request.getTrinhDo());
        registration.setCauLacBo(request.getCauLacBo());
        registration.setTenHlv(request.getTenHlv());
        
        // Map partner info (for doubles)
        registration.setTenDongDoi(request.getTenDongDoi());
        registration.setEmailDongDoi(request.getEmailDongDoi());
        registration.setSdtDongDoi(request.getSdtDongDoi());
        
        // Map payment & notes
        registration.setPhuongThucThanhToan(request.getPhuongThucThanhToan());
        registration.setGhiChu(request.getGhiChu());
        
        // Map agreements
        registration.setDongYDieuKhoan(request.getDongYDieuKhoan());
        registration.setXacNhanSucKhoe(request.getXacNhanSucKhoe());
        
        // Set status
        registration.setTrangThai("pending");
        registration.setTrangThaiThanhToan("unpaid");
        registration.setMaDangKy(registrationCode);
        
        // Set metadata
        registration.setIpAddress(request.getIpAddress());
        registration.setUserAgent(request.getUserAgent());
        
        // Save to database
        DangKyThiDau saved = registrationRepository.save(registration);
        logger.info("✅ Registration saved successfully - ID: {}, Code: {}", 
                    saved.getId(), saved.getMaDangKy());
        
        return saved;
    }
    
    /**
     * Check if email already registered for tournament
     */
    public boolean isAlreadyRegistered(Integer tournamentId, String email) {
        return registrationRepository.existsByIdGiaiDauAndEmail(tournamentId, email);
    }
    
    /**
     * Find registration by code
     */
    public Optional<DangKyThiDau> findByRegistrationCode(String code) {
        return registrationRepository.findByMaDangKy(code);
    }
    
    /**
     * Get all registrations for a tournament
     */
    public List<DangKyThiDau> getRegistrationsByTournament(Integer tournamentId) {
        return registrationRepository.findByIdGiaiDauOrderByNgayTaoDesc(tournamentId);
    }
    
    /**
     * Get registrations by status
     */
    public List<DangKyThiDau> getRegistrationsByStatus(Integer tournamentId, String status) {
        return registrationRepository.findByIdGiaiDauAndTrangThaiOrderByNgayTaoDesc(tournamentId, status);
    }
    
    /**
     * Count total registrations
     */
    public long countRegistrations(Integer tournamentId) {
        return registrationRepository.countByIdGiaiDau(tournamentId);
    }
    
    /**
     * Count pending registrations
     */
    public long countPendingRegistrations(Integer tournamentId) {
        return registrationRepository.countPendingRegistrations(tournamentId);
    }
    
    /**
     * Update registration status
     */
    public DangKyThiDau updateStatus(Integer registrationId, String newStatus) {
        DangKyThiDau registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new IllegalArgumentException("Đăng ký không tồn tại"));
        
        registration.setTrangThai(newStatus);
        return registrationRepository.save(registration);
    }
    
    /**
     * Update payment status
     */
    public DangKyThiDau updatePaymentStatus(Integer registrationId, String paymentStatus) {
        DangKyThiDau registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new IllegalArgumentException("Đăng ký không tồn tại"));
        
        registration.setTrangThaiThanhToan(paymentStatus);
        if ("paid".equals(paymentStatus)) {
            registration.setNgayThanhToan(java.time.LocalDateTime.now());
        }
        return registrationRepository.save(registration);
    }
    
    /**
     * Generate unique registration code
     */
    private String generateRegistrationCode(Integer tournamentId) {
        return "REG-" + tournamentId + "-" + System.currentTimeMillis();
    }
    
    /**
     * Validate registration request
     */
    private void validateRegistrationRequest(TournamentRegistrationRequest request) {
        // Validate required fields
        if (request.getHoTen() == null || request.getHoTen().trim().isEmpty()) {
            throw new IllegalArgumentException("Họ tên không được để trống");
        }
        
        if (request.getEmail() == null || !request.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("Email không hợp lệ");
        }
        
        if (request.getDienThoai() == null || !request.getDienThoai().matches("^0\\d{9,10}$")) {
            throw new IllegalArgumentException("Số điện thoại không hợp lệ (phải có 10-11 số và bắt đầu bằng 0)");
        }
        
        if (request.getCccd() == null || !request.getCccd().matches("^\\d{12}$")) {
            throw new IllegalArgumentException("CCCD/CMND không hợp lệ (phải có 12 số)");
        }
        
        // Validate age (must be at least 10 years old)
        if (request.getNgaySinh() != null) {
            LocalDate birthDate = LocalDate.parse(request.getNgaySinh());
            int age = Period.between(birthDate, LocalDate.now()).getYears();
            if (age < 10) {
                throw new IllegalArgumentException("Tuổi tối thiểu là 10 tuổi");
            }
            if (age > 100) {
                throw new IllegalArgumentException("Ngày sinh không hợp lệ");
            }
        }
        
        // Validate doubles registration
        if (request.getNoiDung() != null && request.getNoiDung().contains("doubles")) {
            if (request.getTenDongDoi() == null || request.getTenDongDoi().trim().isEmpty()) {
                throw new IllegalArgumentException("Vui lòng nhập tên đồng đội cho nội dung đôi");
            }
        }
        
        // Validate agreements
        if (request.getDongYDieuKhoan() == null || !request.getDongYDieuKhoan()) {
            throw new IllegalArgumentException("Vui lòng đồng ý với điều khoản và điều kiện");
        }
        
        if (request.getXacNhanSucKhoe() == null || !request.getXacNhanSucKhoe()) {
            throw new IllegalArgumentException("Vui lòng xác nhận sức khỏe tốt");
        }
        
        logger.debug("✅ Validation passed for email: {}", request.getEmail());
    }
}
