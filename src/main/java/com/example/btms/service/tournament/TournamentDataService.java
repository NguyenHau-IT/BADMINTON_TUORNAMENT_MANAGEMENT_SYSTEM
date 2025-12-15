package com.example.btms.service.tournament;

import com.example.btms.model.tournament.GiaiDau;
import com.example.btms.repository.web.tuornament.GiaiDauRepository;
import com.example.btms.repository.web.tuornament.TournamentGalleryRepository;
import com.example.btms.web.dto.*;
import com.example.btms.web.mapper.TournamentMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service để xử lý business logic cho giải đấu
 * 
 * REFACTORED VERSION 2.0 - Web Platform:
 * - ❌ Removed JSON file logic (old)
 * - ✅ Connected to SQL Server database via Repository
 * - ✅ Using Mapper to convert Entity ↔ DTO
 * - ✅ Support pagination, filtering, searching
 * - ✅ Business logic methods for various use cases
 * 
 * @author BTMS Team
 * @version 2.0 - Database-driven
 */
@Service
@Transactional(readOnly = true)  // Default: read-only transactions (optimization)
public class TournamentDataService {

    private static final Logger logger = LoggerFactory.getLogger(TournamentDataService.class);
    
    // ========== DEPENDENCIES (Injected by Spring) ==========
    private final GiaiDauRepository giaiDauRepository;
    private final TournamentGalleryRepository galleryRepository;
    private final TournamentMapper tournamentMapper;

    /**
     * Constructor injection (Spring best practice)
     */
    public TournamentDataService(
            GiaiDauRepository giaiDauRepository,
            TournamentGalleryRepository galleryRepository,
            TournamentMapper tournamentMapper) {
        this.giaiDauRepository = giaiDauRepository;
        this.galleryRepository = galleryRepository;
        this.tournamentMapper = tournamentMapper;
        logger.info("TournamentDataService initialized - connected to SQL Server database");
    }

    // ========== BASIC CRUD OPERATIONS ==========

    /**
     * Lấy tất cả giải đấu (list view - TournamentDTO)
     * Use case: Trang danh sách giải đấu
     */
    public List<TournamentDTO> getAllTournaments() {
        logger.debug("Getting all tournaments");
        List<GiaiDau> entities = giaiDauRepository.findAll(Sort.by(Sort.Direction.DESC, "ngayBd"));
        return tournamentMapper.toDTOList(entities);
    }

    /**
     * Lấy tất cả giải đấu với pagination
     * Use case: Trang danh sách có phân trang
     */
    public Page<TournamentDTO> getAllTournaments(Pageable pageable) {
        logger.debug("Getting tournaments page: {}", pageable);
        Page<GiaiDau> entitiesPage = giaiDauRepository.findAll(pageable);
        return entitiesPage.map(tournamentMapper::toDTO);
    }

    /**
     * Lấy giải đấu theo ID (detail view - TournamentDetailDTO)
     * Use case: Trang chi tiết giải đấu /tournament/{id}
     * 
     * CACHE: Cache theo ID giải đấu
     */
    @Cacheable(value = "tournament-detail", key = "#id")
    public TournamentDetailDTO getTournamentById(Integer id) {
        logger.debug("Getting tournament by ID: {}", id);
        Optional<GiaiDau> entity = giaiDauRepository.findById(id);
        return entity.map(tournamentMapper::toDetailDTO).orElse(null);
    }

    /**
     * Kiểm tra giải đấu có tồn tại không
     */
    public boolean existsById(Integer id) {
        return giaiDauRepository.existsById(id);
    }

    /**
     * Đếm tổng số giải đấu
     * 
     * CACHE: Cache count, refresh khi có update
     */
    @Cacheable(value = "tournament-stats", key = "'total-count'")
    public long getTotalTournaments() {
        return giaiDauRepository.count();
    }

    // ========== SEARCH AUTOCOMPLETE ==========

    /**
     * Tìm kiếm giải đấu cho autocomplete
     * Use case: Search box dropdown gợi ý
     * 
     * @param keyword Từ khóa tìm kiếm (tên giải, tỉnh thành, mô tả)
     * @param limit Số lượng kết quả tối đa
     * @return List các giải đấu khớp với keyword
     */
    public List<TournamentSearchDTO> searchTournaments(String keyword, int limit) {
        logger.debug("Searching tournaments with keyword: '{}', limit: {}", keyword, limit);
        
        try {
            // Gọi repository để search
            List<GiaiDau> entities = giaiDauRepository.findByKeyword(keyword, limit);
            
            // Convert Entity -> SearchDTO
            List<TournamentSearchDTO> results = entities.stream()
                .map(this::mapToSearchDTO)
                .collect(Collectors.toList());
            
            logger.info("Found {} tournaments for keyword '{}'", results.size(), keyword);
            return results;
            
        } catch (Exception e) {
            logger.error("Error searching tournaments: {}", e.getMessage(), e);
            return List.of(); // Return empty list on error
        }
    }

    /**
     * Map GiaiDau Entity -> TournamentSearchDTO
     * Helper method cho search functionality
     */
    private TournamentSearchDTO mapToSearchDTO(GiaiDau entity) {
        TournamentSearchDTO dto = new TournamentSearchDTO();
        dto.setId(entity.getId());
        dto.setTenGiai(entity.getTenGiai());
        dto.setHinhAnh(entity.getHinhAnh());
        dto.setTinhThanh(entity.getTinhThanh());
        
        // Format ngày: LocalDate -> String "dd/MM/yyyy"
        if (entity.getNgayBd() != null) {
            dto.setNgayBatDau(entity.getNgayBd().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        }
        
        dto.setTrangThai(entity.getTrangThai());
        
        return dto;
    }

    // ========== FEATURED TOURNAMENTS ==========

    /**
     * Lấy các giải đấu nổi bật (card view - TournamentCardDTO)
     * Use case: Homepage carousel, featured section
     * 
     * PERFORMANCE: Cached - data hiếm khi thay đổi, được truy vấn nhiều lần
     * Cache eviction: Khi admin update tournament featured status
     */
    @Cacheable(value = "featured-tournaments", key = "'all'")
    public List<TournamentCardDTO> getFeaturedTournaments() {
        logger.debug("Getting featured tournaments");
        List<GiaiDau> entities = giaiDauRepository.findByNoiBat(true);
        return tournamentMapper.toCardDTOList(entities);
    }

    /**
     * Lấy N giải nổi bật (limit)
     * Use case: Homepage "Top Featured" section
     * 
     * PERFORMANCE: Cached với key bao gồm limit để cache riêng từng kích thước
     * Cache key: limit value (e.g., "4", "8", "12")
     */
    @Cacheable(value = "featured-tournaments", key = "#limit")
    public List<TournamentCardDTO> getFeaturedTournaments(int limit) {
        logger.debug("Getting {} featured tournaments", limit);
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "luotXem"));
        Page<GiaiDau> page = giaiDauRepository.findByNoiBat(true, pageable);
        return tournamentMapper.toCardDTOList(page.getContent());
    }

    /**
     * Lấy giải nổi bật đang active (đang mở đăng ký hoặc đang diễn ra)
     * Use case: "Hot Tournaments" banner
     */
    public List<TournamentCardDTO> getActiveFeaturedTournaments() {
        logger.debug("Getting active featured tournaments");
        List<GiaiDau> entities = giaiDauRepository.findActiveFeaturedTournaments();
        return tournamentMapper.toCardDTOList(entities);
    }

    // ========== FILTER BY STATUS ==========

    /**
     * Lấy giải đấu theo trạng thái
     */
    public List<TournamentDTO> getTournamentsByStatus(String trangThai) {
        logger.debug("Getting tournaments by status: {}", trangThai);
        List<GiaiDau> entities = giaiDauRepository.findByTrangThai(trangThai);
        return tournamentMapper.toDTOList(entities);
    }

    /**
     * Lấy giải đấu theo trạng thái với pagination
     */
    public Page<TournamentDTO> getTournamentsByStatus(String trangThai, Pageable pageable) {
        logger.debug("Getting tournaments by status: {} (paginated)", trangThai);
        Page<GiaiDau> page = giaiDauRepository.findByTrangThai(trangThai, pageable);
        return page.map(tournamentMapper::toDTO);
    }

    /**
     * Lấy các giải đấu sắp diễn ra (upcoming + registration)
     * Use case: "Upcoming Tournaments" page
     * 
     * CACHE: Cache danh sách upcoming, TTL ngắn (5 phút)
     */
    @Cacheable(value = "upcoming-tournaments", key = "'list'")
    public List<TournamentDTO> getUpcomingTournaments() {
        logger.debug("Getting upcoming tournaments");
        LocalDate today = LocalDate.now();
        LocalDate in30Days = today.plusDays(30);
        List<GiaiDau> entities = giaiDauRepository.findUpcomingTournaments(today, in30Days);
        return tournamentMapper.toDTOList(entities);
    }

    /**
     * Lấy các giải đấu sắp diễn ra với pagination
     */
    public Page<TournamentDTO> getUpcomingTournaments(Pageable pageable) {
        logger.debug("Getting upcoming tournaments (paginated)");
        LocalDate today = LocalDate.now();
        LocalDate in30Days = today.plusDays(30);
        Page<GiaiDau> page = giaiDauRepository.findUpcomingTournaments(today, in30Days, pageable);
        return page.map(tournamentMapper::toDTO);
    }

    /**
     * Lấy các giải đấu đang diễn ra (ongoing)
     * Use case: "Live Tournaments" page, homepage "Now Playing"
     * 
     * CACHE: Cache ngắn hạn cho ongoing (2 phút)
     */
    @Cacheable(value = "ongoing-tournaments", key = "'list'")
    public List<TournamentDTO> getOngoingTournaments() {
        logger.debug("Getting ongoing tournaments");
        LocalDate today = LocalDate.now();
        List<GiaiDau> entities = giaiDauRepository.findOngoingTournaments(today);
        return tournamentMapper.toDTOList(entities);
    }

    /**
     * Lấy các giải đấu đang mở đăng ký
     * Use case: "Register Now" page
     * 
     * CACHE: Cache danh sách registration (5 phút)
     */
    @Cacheable(value = "registration-tournaments", key = "'list'")
    public List<TournamentDTO> getOpenForRegistrationTournaments() {
        logger.debug("Getting tournaments open for registration");
        LocalDate today = LocalDate.now();
        List<GiaiDau> entities = giaiDauRepository.findOpenForRegistration(today);
        return tournamentMapper.toDTOList(entities);
    }

    /**
     * Lấy các giải đấu đang mở đăng ký với pagination
     */
    public Page<TournamentDTO> getOpenForRegistrationTournaments(Pageable pageable) {
        logger.debug("Getting tournaments open for registration (paginated)");
        LocalDate today = LocalDate.now();
        Page<GiaiDau> page = giaiDauRepository.findOpenForRegistration(today, pageable);
        return page.map(tournamentMapper::toDTO);
    }

    /**
     * Lấy các giải đấu theo nhiều trạng thái (IN clause)
     * Use case: Filter dropdown với multiple selection
     */
    public List<TournamentDTO> getTournamentsByStatuses(List<String> statuses) {
        logger.debug("Getting tournaments by statuses: {}", statuses);
        List<GiaiDau> entities = giaiDauRepository.findByTrangThaiIn(statuses);
        return tournamentMapper.toDTOList(entities);
    }

    /**
     * Lấy các giải đấu theo nhiều trạng thái với pagination
     */
    public Page<TournamentDTO> getTournamentsByStatuses(List<String> statuses, Pageable pageable) {
        logger.debug("Getting tournaments by statuses: {} (paginated)", statuses);
        Page<GiaiDau> page = giaiDauRepository.findByTrangThaiIn(statuses, pageable);
        return page.map(tournamentMapper::toDTO);
    }

    // ========== FILTER BY LOCATION ==========

    /**
     * Lấy giải đấu theo tỉnh/thành
     * Use case: Filter by province/city
     */
    public List<TournamentDTO> getTournamentsByProvince(String tinhThanh) {
        logger.debug("Getting tournaments by province: {}", tinhThanh);
        List<GiaiDau> entities = giaiDauRepository.findByTinhThanh(tinhThanh);
        return tournamentMapper.toDTOList(entities);
    }

    /**
     * Lấy giải đấu theo tỉnh/thành với pagination
     */
    public Page<TournamentDTO> getTournamentsByProvince(String tinhThanh, Pageable pageable) {
        logger.debug("Getting tournaments by province: {} (paginated)", tinhThanh);
        Page<GiaiDau> page = giaiDauRepository.findByTinhThanh(tinhThanh, pageable);
        return page.map(tournamentMapper::toDTO);
    }

    // ========== FILTER BY LEVEL & TYPE ==========

    /**
     * Lấy giải đấu theo cấp độ
     * Use case: Filter by skill level
     */
    public List<TournamentDTO> getTournamentsByLevel(String capDo) {
        logger.debug("Getting tournaments by level: {}", capDo);
        List<GiaiDau> entities = giaiDauRepository.findByCapDo(capDo);
        return tournamentMapper.toDTOList(entities);
    }

    /**
     * Lấy giải đấu theo thể loại
     * Use case: Filter by tournament type
     */
    public List<TournamentDTO> getTournamentsByType(String theLoai) {
        logger.debug("Getting tournaments by type: {}", theLoai);
        List<GiaiDau> entities = giaiDauRepository.findByTheLoai(theLoai);
        return tournamentMapper.toDTOList(entities);
    }

    // ========== SEARCH ==========

    /**
     * Search giải đấu (tên/địa điểm/tỉnh thành)
     * Use case: Search bar
     */
    public List<TournamentDTO> searchTournaments(String keyword) {
        logger.debug("Searching tournaments with keyword: {}", keyword);
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllTournaments();
        }
        List<GiaiDau> entities = giaiDauRepository.searchTournaments(keyword.trim());
        return tournamentMapper.toDTOList(entities);
    }

    /**
     * Search giải đấu với pagination
     */
    public Page<TournamentDTO> searchTournaments(String keyword, Pageable pageable) {
        logger.debug("Searching tournaments with keyword: {} (paginated)", keyword);
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllTournaments(pageable);
        }
        Page<GiaiDau> page = giaiDauRepository.searchTournaments(keyword.trim(), pageable);
        return page.map(tournamentMapper::toDTO);
    }

    /**
     * Advanced search với nhiều filters
     * Use case: Advanced search page với multiple criteria
     */
    public Page<TournamentDTO> advancedSearch(
            String keyword,
            String trangThai,
            String tinhThanh,
            String capDo,
            String theLoai,
            Boolean noiBat,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable) {
        
        logger.debug("Advanced search - keyword: {}, status: {}, province: {}, level: {}, type: {}, featured: {}, dates: {} to {}",
                keyword, trangThai, tinhThanh, capDo, theLoai, noiBat, fromDate, toDate);
        
        Page<GiaiDau> page = giaiDauRepository.advancedSearch(
                keyword, trangThai, tinhThanh, capDo, theLoai, noiBat, fromDate, toDate, pageable);
        
        return page.map(tournamentMapper::toDTO);
    }

    // ========== CALENDAR ==========

    /**
     * Lấy giải đấu trong một tháng (cho FullCalendar.js)
     * Use case: Tournament calendar page
     */
    public List<TournamentCalendarEventDTO> getTournamentsInMonth(int year, int month) {
        logger.debug("Getting tournaments in month: {}/{}", year, month);
        
        LocalDate monthStart = LocalDate.of(year, month, 1);
        LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);
        
        List<GiaiDau> entities = giaiDauRepository.findTournamentsInMonth(
                year, month, monthStart, monthEnd);
        
        return tournamentMapper.toCalendarEventDTOList(entities);
    }

    /**
     * Lấy giải đấu trong khoảng thời gian
     * Use case: Date range picker
     */
    public List<TournamentDTO> getTournamentsBetweenDates(LocalDate startDate, LocalDate endDate) {
        logger.debug("Getting tournaments between {} and {}", startDate, endDate);
        List<GiaiDau> entities = giaiDauRepository.findByNgayBdBetween(startDate, endDate);
        return tournamentMapper.toDTOList(entities);
    }

    // ========== TOP TOURNAMENTS ==========

    /**
     * Lấy top giải đấu có lượt xem cao nhất
     * Use case: "Most Viewed" section
     */
    public List<TournamentCardDTO> getTopViewedTournaments(int limit) {
        logger.debug("Getting top {} viewed tournaments", limit);
        Pageable pageable = PageRequest.of(0, limit);
        List<GiaiDau> entities = giaiDauRepository.findTopViewedTournaments(pageable);
        return tournamentMapper.toCardDTOList(entities);
    }

    /**
     * Lấy top giải đấu có đánh giá cao nhất
     * Use case: "Highest Rated" section
     */
    public List<TournamentCardDTO> getTopRatedTournaments(int limit) {
        logger.debug("Getting top {} rated tournaments", limit);
        Pageable pageable = PageRequest.of(0, limit);
        List<GiaiDau> entities = giaiDauRepository.findTopRatedTournaments(pageable);
        return tournamentMapper.toCardDTOList(entities);
    }

    // ========== STATISTICS ==========

    /**
     * Lấy thống kê chi tiết của một giải đấu
     * Use case: Admin dashboard, analytics page
     */
    public TournamentStatsDTO getTournamentStats(Integer id) {
        logger.debug("Getting tournament stats for ID: {}", id);
        Optional<GiaiDau> entity = giaiDauRepository.findById(id);
        return entity.map(tournamentMapper::toStatsDTO).orElse(null);
    }

    /**
     * Lấy thống kê theo trạng thái (số lượng giải đấu theo từng trạng thái)
     * Use case: Dashboard overview
     * 
     * PERFORMANCE: Cached - thống kê tổng hợp, query multiple counts
     * Cache eviction: Khi tournament status changes
     */
    @Cacheable(value = "tournament-stats", key = "'status-summary'")
    public Map<String, Long> getStatsByStatus() {
        logger.debug("Getting statistics by status");
        
        // Danh sách các trạng thái cần thống kê
        List<String> statuses = Arrays.asList("draft", "upcoming", "registration", "ongoing", "completed", "cancelled");
        
        return statuses.stream()
                .collect(Collectors.toMap(
                        status -> status,
                        status -> giaiDauRepository.countByTrangThai(status)
                ));
    }

    /**
     * Đếm số giải nổi bật
     */
    public long countFeaturedTournaments() {
        return giaiDauRepository.countFeaturedTournaments();
    }

    /**
     * Đếm số giải đấu theo trạng thái
     */
    public long countTournamentsByStatus(String trangThai) {
        return giaiDauRepository.countByTrangThai(trangThai);
    }

    /**
     * Lấy thống kê theo danh mục (capDo - category/level)
     * Use case: Tournament Home page - Category cards
     * 
     * @return Map với key là category và value là số lượng giải đấu
     */
    @Cacheable(value = "tournament-stats", key = "'category-summary'")
    public Map<String, Long> getStatsByCategory() {
        logger.debug("Getting statistics by category");
        
        // Danh sách categories cần thống kê (mapping với capDo trong DB)
        // professional, amateur, club, youth, corporate
        Map<String, String> categoryMapping = Map.of(
            "professional", "Chuyên nghiệp",
            "amateur", "Nghiệp dư", 
            "club", "Câu lạc bộ",
            "youth", "Thanh thiếu niên",
            "corporate", "Doanh nghiệp"
        );
        
        Map<String, Long> stats = new java.util.HashMap<>();
        
        for (Map.Entry<String, String> entry : categoryMapping.entrySet()) {
            String key = entry.getKey();
            String capDoValue = entry.getValue();
            long count = giaiDauRepository.findByCapDo(capDoValue).size();
            stats.put(key, count);
        }
        
        // Thêm tổng số
        stats.put("total", giaiDauRepository.count());
        
        return stats;
    }

    // ========== BUSINESS LOGIC HELPERS ==========

    /**
     * Kiểm tra giải đấu có đang mở đăng ký không
     * Use case: Enable/disable "Register" button
     */
    public boolean isRegistrationOpen(Integer id) {
        Optional<GiaiDau> entity = giaiDauRepository.findById(id);
        if (entity.isEmpty()) {
            return false;
        }
        
        GiaiDau giai = entity.get();
        LocalDate today = LocalDate.now();
        
        return "registration".equals(giai.getTrangThai()) &&
               giai.getNgayMoDangKi() != null &&
               giai.getNgayDongDangKi() != null &&
               !today.isBefore(giai.getNgayMoDangKi()) &&
               !today.isAfter(giai.getNgayDongDangKi());
    }

    /**
     * Kiểm tra giải đấu có đang diễn ra không
     */
    public boolean isOngoing(Integer id) {
        Optional<GiaiDau> entity = giaiDauRepository.findById(id);
        if (entity.isEmpty()) {
            return false;
        }
        
        GiaiDau giai = entity.get();
        LocalDate today = LocalDate.now();
        
        return "ongoing".equals(giai.getTrangThai()) &&
               giai.getNgayBd() != null &&
               giai.getNgayKt() != null &&
               !today.isBefore(giai.getNgayBd()) &&
               !today.isAfter(giai.getNgayKt());
    }

    /**
     * Kiểm tra giải đấu còn slot đăng ký không
     */
    public boolean hasAvailableSlots(Integer id) {
        Optional<GiaiDau> entity = giaiDauRepository.findById(id);
        if (entity.isEmpty()) {
            return false;
        }
        
        GiaiDau giai = entity.get();
        return giai.getSoLuongDaDangKy() != null &&
               giai.getSoLuongToiDa() != null &&
               giai.getSoLuongDaDangKy() < giai.getSoLuongToiDa();
    }

    // ========== CREATE/UPDATE/DELETE (for future use) ==========
    
    /**
     * Tạo giải đấu mới
     * Note: @Transactional(readOnly = false) để override class-level annotation
     * 
     * CACHE: Evict all tournament caches vì data mới được thêm
     */
    @Transactional
    @CacheEvict(value = {"featured-tournaments", "tournament-stats", "tournament-list", "tournament-detail", 
                         "upcoming-tournaments", "ongoing-tournaments", "registration-tournaments", "landing-page-stats"}, allEntries = true)
    public TournamentDetailDTO createTournament(TournamentDetailDTO dto) {
        logger.info("Creating new tournament: {}", dto.getTenGiai());
        GiaiDau entity = tournamentMapper.toEntityFromDetailDTO(dto);
        GiaiDau savedEntity = giaiDauRepository.save(entity);
        return tournamentMapper.toDetailDTO(savedEntity);
    }

    /**
     * Update giải đấu
     * 
     * CACHE: Evict all caches vì tournament data changed
     */
    @Transactional
    @CacheEvict(value = {"featured-tournaments", "tournament-stats", "tournament-list", "tournament-detail",
                         "upcoming-tournaments", "ongoing-tournaments", "registration-tournaments", "landing-page-stats"}, allEntries = true)
    public TournamentDetailDTO updateTournament(Integer id, TournamentDTO dto) {
        logger.info("Updating tournament ID: {}", id);
        
        Optional<GiaiDau> existingEntity = giaiDauRepository.findById(id);
        if (existingEntity.isEmpty()) {
            logger.warn("Tournament not found: {}", id);
            return null;
        }
        
        GiaiDau entity = existingEntity.get();
        tournamentMapper.updateEntityFromDTO(entity, dto);
        GiaiDau savedEntity = giaiDauRepository.save(entity);
        
        return tournamentMapper.toDetailDTO(savedEntity);
    }

    /**
     * Xóa giải đấu (soft delete - set status to cancelled)
     * 
     * CACHE: Evict all caches vì status changed
     */
    @Transactional
    @CacheEvict(value = {"featured-tournaments", "tournament-stats", "tournament-list", "tournament-detail",
                         "upcoming-tournaments", "ongoing-tournaments", "registration-tournaments", "landing-page-stats"}, allEntries = true)
    public boolean deleteTournament(Integer id) {
        logger.info("Deleting tournament ID: {}", id);
        
        Optional<GiaiDau> entity = giaiDauRepository.findById(id);
        if (entity.isEmpty()) {
            logger.warn("Tournament not found: {}", id);
            return false;
        }
        
        // Soft delete: chỉ đổi trạng thái thành cancelled
        GiaiDau giai = entity.get();
        giai.setTrangThai("cancelled");
        giaiDauRepository.save(giai);
        
        logger.info("Tournament {} marked as cancelled", id);
        return true;
    }

    /**
     * Hard delete (xóa vĩnh viễn - use with caution!)
     * 
     * CACHE: Evict all caches vì data deleted
     */
    @Transactional
    @CacheEvict(value = {"featured-tournaments", "tournament-stats", "tournament-list", "tournament-detail",
                         "upcoming-tournaments", "ongoing-tournaments", "registration-tournaments", "landing-page-stats"}, allEntries = true)
    public boolean hardDeleteTournament(Integer id) {
        logger.warn("Hard deleting tournament ID: {}", id);
        
        if (!giaiDauRepository.existsById(id)) {
            logger.warn("Tournament not found: {}", id);
            return false;
        }
        
        // Xóa gallery trước (foreign key constraint)
        galleryRepository.deleteByGiaiDau_Id(id);
        
        // Xóa giải đấu
        giaiDauRepository.deleteById(id);
        
        logger.info("Tournament {} permanently deleted", id);
        return true;
    }

    // ========== LANDING PAGE STATISTICS ==========

    /**
     * Lấy thống kê tổng quan cho landing page
     * Bao gồm: tổng giải đấu, registrations, và các metrics khác
     * 
     * @return Map chứa các thống kê
     */
    @Cacheable(value = "landing-page-stats", key = "'overview'")
    public Map<String, Long> getLandingPageStats() {
        logger.debug("Getting landing page statistics");
        
        // Tổng số giải đấu
        long totalTournaments = giaiDauRepository.count();
        
        // Số giải đang diễn ra
        long ongoingTournaments = giaiDauRepository.countByTrangThai("ongoing");
        
        // Số giải đã hoàn thành
        long completedTournaments = giaiDauRepository.countByTrangThai("completed");
        
        // Số giải sắp diễn ra
        long upcomingTournaments = giaiDauRepository.countByTrangThai("upcoming");
        
        // Số giải nổi bật
        long featuredTournaments = giaiDauRepository.countFeaturedTournaments();
        
        logger.debug("Stats: total={}, ongoing={}, completed={}, upcoming={}", 
            totalTournaments, ongoingTournaments, completedTournaments, upcomingTournaments);
        
        return Map.of(
            "totalTournaments", totalTournaments,
            "ongoingTournaments", ongoingTournaments,
            "completedTournaments", completedTournaments,
            "upcomingTournaments", upcomingTournaments,
            "featuredTournaments", featuredTournaments
        );
    }

    /**
     * Tính tổng số registrations từ tất cả giải đấu
     * Sử dụng để hiển thị "Tổng số vận động viên đăng ký"
     * 
     * @return Tổng số registrations
     */
    public long getTotalRegistrations() {
        // Tính tổng từ tất cả giải đấu có registrations
        // Nếu cần performance cao hơn, có thể tạo query riêng trong Repository
        try {
            // Lấy tất cả tournaments và tính tổng registrations
            List<GiaiDau> allTournaments = giaiDauRepository.findAll();
            return allTournaments.stream()
                .mapToLong(t -> t.getSoLuongDaDangKy() != null ? t.getSoLuongDaDangKy() : 0)
                .sum();
        } catch (Exception e) {
            logger.warn("Error calculating total registrations: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Ước tính số trận đấu dựa trên số giải đấu đã hoàn thành
     * Formula: completedTournaments * avgMatchesPerTournament (estimated 20)
     * 
     * @return Ước tính số trận đấu
     */
    @Cacheable(value = "landing-page-stats", key = "'estimated-matches'")
    public long getEstimatedMatches() {
        try {
            // Giải đã hoàn thành thường có đầy đủ trận đấu
            long completedTournaments = giaiDauRepository.countByTrangThai("completed");
            
            // Giải đang diễn ra có khoảng 50% số trận
            long ongoingTournaments = giaiDauRepository.countByTrangThai("ongoing");
            
            // Ước tính trung bình 20 trận/giải (tùy thuộc quy mô)
            // Có thể điều chỉnh con số này dựa trên data thực tế
            int avgMatchesPerTournament = 20;
            
            long estimatedMatches = (completedTournaments * avgMatchesPerTournament) 
                                  + (ongoingTournaments * avgMatchesPerTournament / 2);
            
            // Đảm bảo có giá trị tối thiểu cho demo
            return Math.max(estimatedMatches, 100);
        } catch (Exception e) {
            logger.warn("Error estimating matches: {}", e.getMessage());
            return 100; // Default value
        }
    }
}
