package com.example.btms.model.news;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * JPA Entity cho bảng NEWS_ARTICLES
 * Dùng cho Web Platform - News module
 * 
 * @author BTMS Team
 * @version 2.0 - Web Platform
 */
@Entity
@Table(name = "NEWS_ARTICLES")
public class NewsArticleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @Column(name = "TIEU_DE", nullable = false, length = 500)
    private String tieuDe; // Tiêu đề bài viết

    @Column(name = "NOI_DUNG", columnDefinition = "NVARCHAR(MAX)")
    private String noiDung; // Nội dung HTML

    @Column(name = "TOM_TAT", length = 1000)
    private String tomTat; // Tóm tắt/excerpt

    @Column(name = "ANH_DAI_DIEN", length = 500)
    private String anhDaiDien; // URL ảnh đại diện

    @Column(name = "ID_TAC_GIA")
    private Integer idTacGia; // FK tới NGUOI_DUNG

    @Column(name = "DANH_MUC", length = 50)
    private String danhMuc; // events, results, interviews, tips, equipment

    @Column(name = "ID_GIAI_DAU")
    private Integer idGiaiDau; // FK tới GIAI_DAU (nếu tin liên quan giải đấu)

    @Column(name = "TRANG_THAI", length = 20)
    private String trangThai; // draft, published, archived

    @Column(name = "LUOT_XEM")
    private Integer luotXem;

    @Column(name = "NOI_BAT")
    private Boolean noiBat; // Tin nổi bật

    @Column(name = "NGAY_XUAT_BAN")
    private LocalDateTime ngayXuatBan;

    @Column(name = "THOI_GIAN_TAO")
    private LocalDateTime thoiGianTao;

    @Column(name = "THOI_GIAN_CAP_NHAT")
    private LocalDateTime thoiGianCapNhat;

    // ========== Transient fields ==========
    
    @Transient
    private String tenTacGia; // Tên tác giả (join)
    
    @Transient
    private String tenGiaiDau; // Tên giải đấu (join)

    // ========== Constructors ==========
    
    public NewsArticleEntity() {
    }

    public NewsArticleEntity(String tieuDe, String noiDung, String danhMuc, Integer idTacGia) {
        this.tieuDe = tieuDe;
        this.noiDung = noiDung;
        this.danhMuc = danhMuc;
        this.idTacGia = idTacGia;
        this.thoiGianTao = LocalDateTime.now();
        this.thoiGianCapNhat = LocalDateTime.now();
        this.trangThai = "draft";
        this.luotXem = 0;
        this.noiBat = false;
    }

    // ========== Lifecycle callbacks ==========
    
    @PrePersist
    protected void onCreate() {
        this.thoiGianTao = LocalDateTime.now();
        this.thoiGianCapNhat = LocalDateTime.now();
        if (this.trangThai == null) {
            this.trangThai = "draft";
        }
        if (this.luotXem == null) {
            this.luotXem = 0;
        }
        if (this.noiBat == null) {
            this.noiBat = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.thoiGianCapNhat = LocalDateTime.now();
    }

    // ========== Utility methods ==========
    
    /**
     * Tăng lượt xem
     */
    public void incrementViewCount() {
        if (this.luotXem == null) {
            this.luotXem = 0;
        }
        this.luotXem++;
    }

    /**
     * Publish bài viết
     */
    public void publish() {
        this.trangThai = "published";
        this.ngayXuatBan = LocalDateTime.now();
    }

    /**
     * Lấy tên danh mục đầy đủ
     */
    public String getDanhMucText() {
        if (danhMuc == null) return "Tin tức";
        return switch (danhMuc) {
            case "events" -> "Sự kiện";
            case "results" -> "Kết quả";
            case "interviews" -> "Phỏng vấn";
            case "tips" -> "Mẹo & Kỹ thuật";
            case "equipment" -> "Dụng cụ";
            default -> "Tin tức";
        };
    }

    /**
     * Check bài viết đã published
     */
    public boolean isPublished() {
        return "published".equals(trangThai);
    }

    /**
     * Check bài viết nổi bật
     */
    public boolean isFeatured() {
        return Boolean.TRUE.equals(noiBat);
    }

    // ========== Getters and Setters ==========

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTieuDe() {
        return tieuDe;
    }

    public void setTieuDe(String tieuDe) {
        this.tieuDe = tieuDe;
    }

    public String getNoiDung() {
        return noiDung;
    }

    public void setNoiDung(String noiDung) {
        this.noiDung = noiDung;
    }

    public String getTomTat() {
        return tomTat;
    }

    public void setTomTat(String tomTat) {
        this.tomTat = tomTat;
    }

    public String getAnhDaiDien() {
        return anhDaiDien;
    }

    public void setAnhDaiDien(String anhDaiDien) {
        this.anhDaiDien = anhDaiDien;
    }

    public Integer getIdTacGia() {
        return idTacGia;
    }

    public void setIdTacGia(Integer idTacGia) {
        this.idTacGia = idTacGia;
    }

    public String getDanhMuc() {
        return danhMuc;
    }

    public void setDanhMuc(String danhMuc) {
        this.danhMuc = danhMuc;
    }

    public Integer getIdGiaiDau() {
        return idGiaiDau;
    }

    public void setIdGiaiDau(Integer idGiaiDau) {
        this.idGiaiDau = idGiaiDau;
    }

    public String getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(String trangThai) {
        this.trangThai = trangThai;
    }

    public Integer getLuotXem() {
        return luotXem;
    }

    public void setLuotXem(Integer luotXem) {
        this.luotXem = luotXem;
    }

    public Boolean getNoiBat() {
        return noiBat;
    }

    public void setNoiBat(Boolean noiBat) {
        this.noiBat = noiBat;
    }

    public LocalDateTime getNgayXuatBan() {
        return ngayXuatBan;
    }

    public void setNgayXuatBan(LocalDateTime ngayXuatBan) {
        this.ngayXuatBan = ngayXuatBan;
    }

    public LocalDateTime getThoiGianTao() {
        return thoiGianTao;
    }

    public void setThoiGianTao(LocalDateTime thoiGianTao) {
        this.thoiGianTao = thoiGianTao;
    }

    public LocalDateTime getThoiGianCapNhat() {
        return thoiGianCapNhat;
    }

    public void setThoiGianCapNhat(LocalDateTime thoiGianCapNhat) {
        this.thoiGianCapNhat = thoiGianCapNhat;
    }

    public String getTenTacGia() {
        return tenTacGia;
    }

    public void setTenTacGia(String tenTacGia) {
        this.tenTacGia = tenTacGia;
    }

    public String getTenGiaiDau() {
        return tenGiaiDau;
    }

    public void setTenGiaiDau(String tenGiaiDau) {
        this.tenGiaiDau = tenGiaiDau;
    }

    @Override
    public String toString() {
        return "NewsArticleEntity{" +
                "id=" + id +
                ", tieuDe='" + tieuDe + '\'' +
                ", danhMuc='" + danhMuc + '\'' +
                ", trangThai='" + trangThai + '\'' +
                ", luotXem=" + luotXem +
                ", noiBat=" + noiBat +
                '}';
    }
}
