package com.example.btms.service.tournament;

import com.example.btms.model.tournament.TournamentRegistration;
import com.example.btms.repository.web.tuornament.TournamentRegistrationRepository;
import com.example.btms.web.dto.TournamentRegistrationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service("tournamentRegistrationService")
public class TournamentRegistrationService {
    
    private static final Logger logger = LoggerFactory.getLogger(TournamentRegistrationService.class);
    
    @Autowired
    private TournamentRegistrationRepository registrationRepository;
    
    /**
     * Check if user already registered for tournament
     */
    public boolean isAlreadyRegistered(Integer tournamentId, String email) {
        return registrationRepository.existsByTournamentIdAndEmail(tournamentId, email);
    }
    
    /**
     * Register user for tournament
     */
    @Transactional
    public TournamentRegistration registerUser(TournamentRegistrationRequest request) {
        logger.info("Processing registration for tournament: {}, email: {}", 
            request.getTournamentId(), request.getEmail());
        
        // Check duplicate
        if (isAlreadyRegistered(request.getTournamentId(), request.getEmail())) {
            throw new IllegalStateException("Email này đã đăng ký giải đấu rồi");
        }
        
        // Create registration entity
        TournamentRegistration registration = new TournamentRegistration();
        registration.setTournamentId(request.getTournamentId());
        registration.setRegistrationCode(generateRegistrationCode(request.getTournamentId()));
        registration.setHoTen(request.getHoTen());
        registration.setEmail(request.getEmail());
        registration.setDienThoai(request.getDienThoai());
        
        // Parse date
        if (request.getNgaySinh() != null && !request.getNgaySinh().isEmpty()) {
            try {
                registration.setNgaySinh(LocalDate.parse(request.getNgaySinh()));
            } catch (Exception e) {
                logger.warn("Failed to parse birth date: {}", request.getNgaySinh());
            }
        }
        
        registration.setGioiTinh(request.getGioiTinh());
        registration.setCccd(request.getCccd());
        registration.setNoiDung(request.getNoiDung());
        registration.setTrinhDo(request.getTrinhDo());
        registration.setCauLacBo(request.getCauLacBo());
        registration.setTenDongDoi(request.getTenDongDoi());
        registration.setEmailDongDoi(request.getEmailDongDoi());
        registration.setPhuongThucThanhToan(request.getPhuongThucThanhToan());
        registration.setDongYDieuKhoan(request.getDongYDieuKhoan());
        registration.setXacNhanSucKhoe(request.getXacNhanSucKhoe());
        registration.setGhiChu(request.getGhiChu());
        registration.setIpAddress(request.getIpAddress());
        registration.setUserAgent(request.getUserAgent());
        registration.setTrangThai("pending");
        
        // Save to database
        TournamentRegistration saved = registrationRepository.save(registration);
        logger.info("✅ Registration saved successfully - ID: {}, Code: {}", 
            saved.getId(), saved.getRegistrationCode());
        
        return saved;
    }
    
    /**
     * Generate unique registration code
     */
    private String generateRegistrationCode(Integer tournamentId) {
        return "REG-" + tournamentId + "-" + System.currentTimeMillis();
    }
    
    /**
     * Get registration by code
     */
    public Optional<TournamentRegistration> getByRegistrationCode(String code) {
        return registrationRepository.findByRegistrationCode(code);
    }
    
    /**
     * Get registration count for tournament
     */
    public long getRegistrationCount(Integer tournamentId) {
        return registrationRepository.countByTournamentId(tournamentId);
    }
}
