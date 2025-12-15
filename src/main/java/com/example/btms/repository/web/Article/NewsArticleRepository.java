package com.example.btms.repository.web.Article;

import com.example.btms.model.news.NewsArticleEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository cho NEWS_ARTICLES
 * Dùng cho News module
 * 
 * @author BTMS Team
 * @version 2.0 - Web Platform
 */
@Repository
public interface NewsArticleRepository extends JpaRepository<NewsArticleEntity, Integer> {

    // ========== BASIC QUERIES ==========
    
    /**
     * Tìm bài viết theo trạng thái
     */
    List<NewsArticleEntity> findByTrangThai(String trangThai);
    
    Page<NewsArticleEntity> findByTrangThai(String trangThai, Pageable pageable);

    /**
     * Tìm bài viết theo danh mục
     */
    List<NewsArticleEntity> findByDanhMuc(String danhMuc);
    
    Page<NewsArticleEntity> findByDanhMuc(String danhMuc, Pageable pageable);

    /**
     * Tìm bài viết theo giải đấu
     */
    List<NewsArticleEntity> findByIdGiaiDau(Integer idGiaiDau);
    
    Page<NewsArticleEntity> findByIdGiaiDau(Integer idGiaiDau, Pageable pageable);

    /**
     * Tìm bài viết nổi bật
     */
    List<NewsArticleEntity> findByNoiBat(Boolean noiBat);
    
    Page<NewsArticleEntity> findByNoiBat(Boolean noiBat, Pageable pageable);

    // ========== NEWS HOME QUERIES ==========

    /**
     * Tin nổi bật đã published, sắp xếp theo ngày xuất bản mới nhất
     */
    @Query("SELECT n FROM NewsArticleEntity n WHERE n.noiBat = true " +
           "AND n.trangThai = 'published' " +
           "ORDER BY n.ngayXuatBan DESC")
    List<NewsArticleEntity> findFeaturedNews(Pageable pageable);

    /**
     * Tin mới nhất đã published
     */
    @Query("SELECT n FROM NewsArticleEntity n WHERE n.trangThai = 'published' " +
           "ORDER BY n.ngayXuatBan DESC")
    List<NewsArticleEntity> findLatestNews(Pageable pageable);
    
    @Query("SELECT n FROM NewsArticleEntity n WHERE n.trangThai = 'published' " +
           "ORDER BY n.ngayXuatBan DESC")
    Page<NewsArticleEntity> findLatestNewsPage(Pageable pageable);

    /**
     * Tin theo danh mục đã published
     */
    @Query("SELECT n FROM NewsArticleEntity n WHERE n.danhMuc = :danhMuc " +
           "AND n.trangThai = 'published' " +
           "ORDER BY n.ngayXuatBan DESC")
    Page<NewsArticleEntity> findPublishedByCategory(@Param("danhMuc") String danhMuc, Pageable pageable);

    /**
     * Tin liên quan (cùng danh mục, khác ID)
     */
    @Query("SELECT n FROM NewsArticleEntity n WHERE n.danhMuc = :danhMuc " +
           "AND n.id != :excludeId AND n.trangThai = 'published' " +
           "ORDER BY n.ngayXuatBan DESC")
    List<NewsArticleEntity> findRelatedNews(@Param("danhMuc") String danhMuc, 
                                            @Param("excludeId") Integer excludeId, 
                                            Pageable pageable);

    /**
     * Tin phổ biến nhất (nhiều lượt xem)
     */
    @Query("SELECT n FROM NewsArticleEntity n WHERE n.trangThai = 'published' " +
           "ORDER BY n.luotXem DESC")
    List<NewsArticleEntity> findPopularNews(Pageable pageable);

    /**
     * Search tin tức theo keyword
     */
    @Query("SELECT n FROM NewsArticleEntity n WHERE n.trangThai = 'published' " +
           "AND (LOWER(n.tieuDe) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(n.tomTat) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<NewsArticleEntity> searchNews(@Param("keyword") String keyword, Pageable pageable);

    // ========== STATISTICS QUERIES ==========

    /**
     * Đếm số bài viết đã published
     */
    @Query("SELECT COUNT(n) FROM NewsArticleEntity n WHERE n.trangThai = 'published'")
    long countPublishedArticles();

    /**
     * Đếm bài viết theo danh mục
     */
    @Query("SELECT COUNT(n) FROM NewsArticleEntity n WHERE n.danhMuc = :danhMuc " +
           "AND n.trangThai = 'published'")
    long countByCategory(@Param("danhMuc") String danhMuc);

    /**
     * Đếm bài viết trong tuần này
     */
    @Query("SELECT COUNT(n) FROM NewsArticleEntity n WHERE n.trangThai = 'published' " +
           "AND n.ngayXuatBan >= :startOfWeek")
    long countThisWeekArticles(@Param("startOfWeek") LocalDateTime startOfWeek);

    // ========== UPDATE QUERIES ==========

    /**
     * Tăng lượt xem
     */
    @Modifying
    @Transactional
    @Query("UPDATE NewsArticleEntity n SET n.luotXem = n.luotXem + 1 WHERE n.id = :id")
    void incrementViewCount(@Param("id") Integer id);

    /**
     * Đặt/bỏ nổi bật
     */
    @Modifying
    @Transactional
    @Query("UPDATE NewsArticleEntity n SET n.noiBat = :noiBat WHERE n.id = :id")
    void setFeatured(@Param("id") Integer id, @Param("noiBat") Boolean noiBat);
}
