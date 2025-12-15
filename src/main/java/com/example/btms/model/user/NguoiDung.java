package com.example.btms.model.user;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * POJO cho bảng NGUOI_DUNG (Users)
 * Quản lý thông tin người dùng cho Web Platform
 * 
 * Database Table: NGUOI_DUNG
 * - 4 original fields từ Desktop App (ID, HO_TEN, MAT_KHAU, THOI_GIAN_TAO)
 * - 10 new fields cho Web Platform (EMAIL, DIEN_THOAI, VAI_TRO, TRANG_THAI,
 * ...)
 * 
 * @author BTMS Team
 * @version 2.0 - Web Platform Enhancement
 */
public class NguoiDung {

    // ==================== ORIGINAL FIELDS (Desktop App) ====================

    private Integer id;
    private String hoTen;
    private String matKhau;
    private LocalDateTime thoiGianTao;

    // ==================== NEW FIELDS (Web Platform) ====================

    private String email;
    private String dienThoai;
    private String vaiTro; // ADMIN, ORGANIZER, PLAYER, CLIENT
    private String trangThai; // active, suspended, deleted
    private LocalDateTime lanDangNhapCuoi;
    private String anhDaiDien;
    private Boolean xacThucEmail;
    private String maXacThuc;
    private String maDatLaiMk;
    private LocalDateTime ngayHetHanToken;

    // ==================== CONSTRUCTORS ====================

    /**
     * Default constructor for JPA
     */
    public NguoiDung() {
        this.thoiGianTao = LocalDateTime.now();
        this.vaiTro = "CLIENT";
        this.trangThai = "active";
        this.xacThucEmail = false;
    }

    /**
     * Constructor for Desktop App compatibility (original 3 fields)
     */
    public NguoiDung(String hoTen, String matKhau) {
        this();
        this.hoTen = hoTen;
        this.matKhau = matKhau;
    }

    /**
     * Full constructor for Web Platform with all fields
     */
    public NguoiDung(String hoTen, String matKhau, String email, String dienThoai, String vaiTro) {
        this();
        this.hoTen = hoTen;
        this.matKhau = matKhau;
        this.email = email;
        this.dienThoai = dienThoai;
        this.vaiTro = vaiTro;
    }

    // ==================== GETTERS & SETTERS ====================

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getHoTen() {
        return hoTen;
    }

    public void setHoTen(String hoTen) {
        this.hoTen = hoTen;
    }

    public String getMatKhau() {
        return matKhau;
    }

    public void setMatKhau(String matKhau) {
        this.matKhau = matKhau;
    }

    public LocalDateTime getThoiGianTao() {
        return thoiGianTao;
    }

    public void setThoiGianTao(LocalDateTime thoiGianTao) {
        this.thoiGianTao = thoiGianTao;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDienThoai() {
        return dienThoai;
    }

    public void setDienThoai(String dienThoai) {
        this.dienThoai = dienThoai;
    }

    public String getVaiTro() {
        return vaiTro;
    }

    public void setVaiTro(String vaiTro) {
        this.vaiTro = vaiTro;
    }

    public String getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(String trangThai) {
        this.trangThai = trangThai;
    }

    public LocalDateTime getLanDangNhapCuoi() {
        return lanDangNhapCuoi;
    }

    public void setLanDangNhapCuoi(LocalDateTime lanDangNhapCuoi) {
        this.lanDangNhapCuoi = lanDangNhapCuoi;
    }

    public String getAnhDaiDien() {
        return anhDaiDien;
    }

    public void setAnhDaiDien(String anhDaiDien) {
        this.anhDaiDien = anhDaiDien;
    }

    public Boolean getXacThucEmail() {
        return xacThucEmail;
    }

    public void setXacThucEmail(Boolean xacThucEmail) {
        this.xacThucEmail = xacThucEmail;
    }

    public String getMaXacThuc() {
        return maXacThuc;
    }

    public void setMaXacThuc(String maXacThuc) {
        this.maXacThuc = maXacThuc;
    }

    public String getMaDatLaiMk() {
        return maDatLaiMk;
    }

    public void setMaDatLaiMk(String maDatLaiMk) {
        this.maDatLaiMk = maDatLaiMk;
    }

    public LocalDateTime getNgayHetHanToken() {
        return ngayHetHanToken;
    }

    public void setNgayHetHanToken(LocalDateTime ngayHetHanToken) {
        this.ngayHetHanToken = ngayHetHanToken;
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Check if user account is active
     */
    public boolean isActive() {
        return "active".equalsIgnoreCase(this.trangThai);
    }

    /**
     * Check if user account is suspended
     */
    public boolean isSuspended() {
        return "suspended".equalsIgnoreCase(this.trangThai);
    }

    /**
     * Check if user has verified email
     */
    public boolean hasVerifiedEmail() {
        return Boolean.TRUE.equals(this.xacThucEmail);
    }

    /**
     * Check if user is admin
     */
    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(this.vaiTro);
    }

    /**
     * Check if user is organizer
     */
    public boolean isOrganizer() {
        return "ORGANIZER".equalsIgnoreCase(this.vaiTro);
    }

    /**
     * Check if reset password token is still valid
     */
    public boolean isResetTokenValid() {
        return this.ngayHetHanToken != null &&
                this.ngayHetHanToken.isAfter(LocalDateTime.now());
    }

    /**
     * Update last login timestamp
     */
    public void updateLastLogin() {
        this.lanDangNhapCuoi = LocalDateTime.now();
    }


    // ==================== EQUALS & HASHCODE ====================

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        NguoiDung nguoiDung = (NguoiDung) o;
        return Objects.equals(id, nguoiDung.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "NguoiDung{" +
                "id=" + id +
                ", hoTen='" + hoTen + '\'' +
                ", email='" + email + '\'' +
                ", vaiTro='" + vaiTro + '\'' +
                ", trangThai='" + trangThai + '\'' +
                '}';
    }
}
