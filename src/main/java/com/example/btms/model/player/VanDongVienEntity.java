package com.example.btms.model.player;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * JPA Entity cho bảng VAN_DONG_VIEN
 * Dùng cho Web Platform - Rankings, Players modules
 * 
 * @author BTMS Team
 * @version 2.0 - Web Platform
 */
@Entity
@Table(name = "VAN_DONG_VIEN")
public class VanDongVienEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @Column(name = "HO_TEN", nullable = false, length = 255)
    private String hoTen;

    @Column(name = "NGAY_SINH")
    private LocalDate ngaySinh;

    @Column(name = "ID_CLB")
    private Integer idClb;

    @Column(name = "GIOI_TINH", length = 1)
    private String gioiTinh; // "M" = Nam, "F" = Nữ

    @Column(name = "ANH_DAI_DIEN", length = 500)
    private String anhDaiDien;

    @Column(name = "TIEU_SU", columnDefinition = "NVARCHAR(MAX)")
    private String tieuSu;

    @Column(name = "CHIEU_CAO")
    private Integer chieuCao; // cm

    @Column(name = "CAN_NANG", precision = 5, scale = 2)
    private BigDecimal canNang; // kg

    @Column(name = "THOI_GIAN_TAO")
    private LocalDateTime thoiGianTao;

    @Column(name = "THOI_GIAN_CAP_NHAT")
    private LocalDateTime thoiGianCapNhat;

    @Column(name = "XEP_HANG_QUOC_GIA")
    private Integer xepHangQuocGia;

    @Column(name = "XEP_HANG_THE_GIOI")
    private Integer xepHangTheGioi;

    @Column(name = "TONG_TRAN_THANG")
    private Integer tongTranThang;

    @Column(name = "TONG_TRAN_THUA")
    private Integer tongTranThua;

    @Column(name = "TI_LE_THANG", precision = 5, scale = 2)
    private BigDecimal tiLeThang;

    @Column(name = "DIEN_THOAI", length = 20)
    private String dienThoai;

    @Column(name = "EMAIL", length = 100)
    private String email;

    @Column(name = "TRANG_THAI", length = 20)
    private String trangThai; // active, inactive, retired

    // ========== Transient fields ==========
    @Transient
    private String tenClb; // Tên CLB (join từ CAU_LAC_BO)

    @Transient
    private Integer diemXepHang; // Điểm xếp hạng tính toán

    // ========== Constructors ==========
    
    public VanDongVienEntity() {
    }

    public VanDongVienEntity(String hoTen, LocalDate ngaySinh, Integer idClb, String gioiTinh) {
        this.hoTen = hoTen;
        this.ngaySinh = ngaySinh;
        this.idClb = idClb;
        this.gioiTinh = gioiTinh;
        this.thoiGianTao = LocalDateTime.now();
        this.thoiGianCapNhat = LocalDateTime.now();
        this.trangThai = "active";
    }

    // ========== Lifecycle callbacks ==========
    
    @PrePersist
    protected void onCreate() {
        this.thoiGianTao = LocalDateTime.now();
        this.thoiGianCapNhat = LocalDateTime.now();
        if (this.trangThai == null) {
            this.trangThai = "active";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.thoiGianCapNhat = LocalDateTime.now();
    }

    // ========== Utility methods ==========
    
    /**
     * Tính tuổi VĐV
     */
    public Integer getTuoi() {
        if (ngaySinh == null) return null;
        return LocalDate.now().getYear() - ngaySinh.getYear();
    }

    /**
     * Lấy tên giới tính đầy đủ
     */
    public String getGioiTinhText() {
        if ("M".equals(gioiTinh)) return "Nam";
        if ("F".equals(gioiTinh)) return "Nữ";
        return "Khác";
    }

    /**
     * Tính tổng số trận đã đấu
     */
    public Integer getTongSoTran() {
        int thang = tongTranThang != null ? tongTranThang : 0;
        int thua = tongTranThua != null ? tongTranThua : 0;
        return thang + thua;
    }

    /**
     * Tính tỉ lệ thắng %
     */
    public BigDecimal getTiLeThangPercent() {
        if (tiLeThang != null) return tiLeThang;
        int total = getTongSoTran();
        if (total == 0) return BigDecimal.ZERO;
        int thang = tongTranThang != null ? tongTranThang : 0;
        return BigDecimal.valueOf(thang * 100.0 / total);
    }

    // ========== Getters and Setters ==========

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

    public LocalDate getNgaySinh() {
        return ngaySinh;
    }

    public void setNgaySinh(LocalDate ngaySinh) {
        this.ngaySinh = ngaySinh;
    }

    public Integer getIdClb() {
        return idClb;
    }

    public void setIdClb(Integer idClb) {
        this.idClb = idClb;
    }

    public String getGioiTinh() {
        return gioiTinh;
    }

    public void setGioiTinh(String gioiTinh) {
        this.gioiTinh = gioiTinh;
    }

    public String getAnhDaiDien() {
        return anhDaiDien;
    }

    public void setAnhDaiDien(String anhDaiDien) {
        this.anhDaiDien = anhDaiDien;
    }

    public String getTieuSu() {
        return tieuSu;
    }

    public void setTieuSu(String tieuSu) {
        this.tieuSu = tieuSu;
    }

    public Integer getChieuCao() {
        return chieuCao;
    }

    public void setChieuCao(Integer chieuCao) {
        this.chieuCao = chieuCao;
    }

    public BigDecimal getCanNang() {
        return canNang;
    }

    public void setCanNang(BigDecimal canNang) {
        this.canNang = canNang;
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

    public Integer getXepHangQuocGia() {
        return xepHangQuocGia;
    }

    public void setXepHangQuocGia(Integer xepHangQuocGia) {
        this.xepHangQuocGia = xepHangQuocGia;
    }

    public Integer getXepHangTheGioi() {
        return xepHangTheGioi;
    }

    public void setXepHangTheGioi(Integer xepHangTheGioi) {
        this.xepHangTheGioi = xepHangTheGioi;
    }

    public Integer getTongTranThang() {
        return tongTranThang;
    }

    public void setTongTranThang(Integer tongTranThang) {
        this.tongTranThang = tongTranThang;
    }

    public Integer getTongTranThua() {
        return tongTranThua;
    }

    public void setTongTranThua(Integer tongTranThua) {
        this.tongTranThua = tongTranThua;
    }

    public BigDecimal getTiLeThang() {
        return tiLeThang;
    }

    public void setTiLeThang(BigDecimal tiLeThang) {
        this.tiLeThang = tiLeThang;
    }

    public String getDienThoai() {
        return dienThoai;
    }

    public void setDienThoai(String dienThoai) {
        this.dienThoai = dienThoai;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(String trangThai) {
        this.trangThai = trangThai;
    }

    public String getTenClb() {
        return tenClb;
    }

    public void setTenClb(String tenClb) {
        this.tenClb = tenClb;
    }

    public Integer getDiemXepHang() {
        return diemXepHang;
    }

    public void setDiemXepHang(Integer diemXepHang) {
        this.diemXepHang = diemXepHang;
    }

    @Override
    public String toString() {
        return "VanDongVienEntity{" +
                "id=" + id +
                ", hoTen='" + hoTen + '\'' +
                ", xepHangQuocGia=" + xepHangQuocGia +
                ", xepHangTheGioi=" + xepHangTheGioi +
                ", gioiTinh='" + gioiTinh + '\'' +
                ", trangThai='" + trangThai + '\'' +
                '}';
    }
}
