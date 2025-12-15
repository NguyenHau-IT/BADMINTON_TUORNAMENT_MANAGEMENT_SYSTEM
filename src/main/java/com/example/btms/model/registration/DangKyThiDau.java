package com.example.btms.model.registration;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity class cho bảng DANG_KY_THI_DAU
 * Quản lý đăng ký tham gia giải đấu từ Web Platform
 * Database: SQL Server
 * 
 * @author BTMS Team
 * @version 1.0
 * @since 2025-11-22
 */
@Entity
@Table(name = "DANG_KY_THI_DAU", 
       indexes = {
           @Index(name = "IDX_DANG_KY_GIAI_DAU", columnList = "ID_GIAI_DAU"),
           @Index(name = "IDX_DANG_KY_EMAIL", columnList = "EMAIL"),
           @Index(name = "IDX_DANG_KY_MA", columnList = "MA_DANG_KY"),
           @Index(name = "IDX_DANG_KY_TRANG_THAI", columnList = "TRANG_THAI")
       })
public class DangKyThiDau {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;
    
    // Tournament Reference
    @Column(name = "ID_GIAI_DAU", nullable = false)
    private Integer idGiaiDau;
    
    // Personal Information
    @Column(name = "HO_TEN", nullable = false, length = 200)
    private String hoTen;
    
    @Column(name = "EMAIL", nullable = false, length = 200)
    private String email;
    
    @Column(name = "DIEN_THOAI", nullable = false, length = 20)
    private String dienThoai;
    
    @Column(name = "NGAY_SINH", nullable = false)
    private LocalDate ngaySinh;
    
    @Column(name = "GIOI_TINH", nullable = false, length = 10)
    private String gioiTinh;
    
    @Column(name = "CCCD", nullable = false, length = 20)
    private String cccd;
    
    // Registration Details
    @Column(name = "NOI_DUNG", nullable = false, length = 50)
    private String noiDung;
    
    @Column(name = "TRINH_DO", length = 50)
    private String trinhDo;
    
    @Column(name = "CAU_LAC_BO", length = 200)
    private String cauLacBo;
    
    @Column(name = "TEN_HLV", length = 200)
    private String tenHlv;
    
    // Partner Information (for doubles)
    @Column(name = "TEN_DONG_DOI", length = 200)
    private String tenDongDoi;
    
    @Column(name = "EMAIL_DONG_DOI", length = 200)
    private String emailDongDoi;
    
    @Column(name = "SDT_DONG_DOI", length = 20)
    private String sdtDongDoi;
    
    // Payment & Notes
    @Column(name = "PHUONG_THUC_THANH_TOAN", nullable = false, length = 50)
    private String phuongThucThanhToan;
    
    @Column(name = "GHI_CHU", columnDefinition = "NVARCHAR(MAX)")
    private String ghiChu;
    
    // Agreement Flags
    @Column(name = "DONG_Y_DIEU_KHOAN", nullable = false)
    private Boolean dongYDieuKhoan = false;
    
    @Column(name = "XAC_NHAN_SUC_KHOE", nullable = false)
    private Boolean xacNhanSucKhoe = false;
    
    // Registration Status
    @Column(name = "TRANG_THAI", nullable = false, length = 50)
    private String trangThai = "pending";
    
    @Column(name = "MA_DANG_KY", nullable = false, unique = true, length = 100)
    private String maDangKy;
    
    // Payment Status
    @Column(name = "TRANG_THAI_THANH_TOAN", nullable = false, length = 50)
    private String trangThaiThanhToan = "unpaid";
    
    @Column(name = "NGAY_THANH_TOAN")
    private LocalDateTime ngayThanhToan;
    
    // Metadata
    @Column(name = "IP_ADDRESS", length = 50)
    private String ipAddress;
    
    @Column(name = "USER_AGENT", length = 500)
    private String userAgent;
    
    // Timestamps
    @Column(name = "NGAY_TAO", nullable = false)
    private LocalDateTime ngayTao;
    
    @Column(name = "NGAY_CAP_NHAT", nullable = false)
    private LocalDateTime ngayCapNhat;
    
    // Default constructor
    public DangKyThiDau() {
        this.ngayTao = LocalDateTime.now();
        this.ngayCapNhat = LocalDateTime.now();
        this.trangThai = "pending";
        this.trangThaiThanhToan = "unpaid";
        this.dongYDieuKhoan = false;
        this.xacNhanSucKhoe = false;
    }
    
    // PrePersist
    @PrePersist
    protected void onCreate() {
        if (this.ngayTao == null) {
            this.ngayTao = LocalDateTime.now();
        }
        if (this.ngayCapNhat == null) {
            this.ngayCapNhat = LocalDateTime.now();
        }
    }
    
    // PreUpdate
    @PreUpdate
    protected void onUpdate() {
        this.ngayCapNhat = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }
    
    public Integer getIdGiaiDau() {
        return idGiaiDau;
    }
    
    public void setIdGiaiDau(Integer idGiaiDau) {
        this.idGiaiDau = idGiaiDau;
    }
    
    public String getHoTen() {
        return hoTen;
    }
    
    public void setHoTen(String hoTen) {
        this.hoTen = hoTen;
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
    
    public LocalDate getNgaySinh() {
        return ngaySinh;
    }
    
    public void setNgaySinh(LocalDate ngaySinh) {
        this.ngaySinh = ngaySinh;
    }
    
    public String getGioiTinh() {
        return gioiTinh;
    }
    
    public void setGioiTinh(String gioiTinh) {
        this.gioiTinh = gioiTinh;
    }
    
    public String getCccd() {
        return cccd;
    }
    
    public void setCccd(String cccd) {
        this.cccd = cccd;
    }
    
    public String getNoiDung() {
        return noiDung;
    }
    
    public void setNoiDung(String noiDung) {
        this.noiDung = noiDung;
    }
    
    public String getTrinhDo() {
        return trinhDo;
    }
    
    public void setTrinhDo(String trinhDo) {
        this.trinhDo = trinhDo;
    }
    
    public String getCauLacBo() {
        return cauLacBo;
    }
    
    public void setCauLacBo(String cauLacBo) {
        this.cauLacBo = cauLacBo;
    }
    
    public String getTenHlv() {
        return tenHlv;
    }
    
    public void setTenHlv(String tenHlv) {
        this.tenHlv = tenHlv;
    }
    
    public String getTenDongDoi() {
        return tenDongDoi;
    }
    
    public void setTenDongDoi(String tenDongDoi) {
        this.tenDongDoi = tenDongDoi;
    }
    
    public String getEmailDongDoi() {
        return emailDongDoi;
    }
    
    public void setEmailDongDoi(String emailDongDoi) {
        this.emailDongDoi = emailDongDoi;
    }
    
    public String getSdtDongDoi() {
        return sdtDongDoi;
    }
    
    public void setSdtDongDoi(String sdtDongDoi) {
        this.sdtDongDoi = sdtDongDoi;
    }
    
    public String getPhuongThucThanhToan() {
        return phuongThucThanhToan;
    }
    
    public void setPhuongThucThanhToan(String phuongThucThanhToan) {
        this.phuongThucThanhToan = phuongThucThanhToan;
    }
    
    public String getGhiChu() {
        return ghiChu;
    }
    
    public void setGhiChu(String ghiChu) {
        this.ghiChu = ghiChu;
    }
    
    public Boolean getDongYDieuKhoan() {
        return dongYDieuKhoan;
    }
    
    public void setDongYDieuKhoan(Boolean dongYDieuKhoan) {
        this.dongYDieuKhoan = dongYDieuKhoan;
    }
    
    public Boolean getXacNhanSucKhoe() {
        return xacNhanSucKhoe;
    }
    
    public void setXacNhanSucKhoe(Boolean xacNhanSucKhoe) {
        this.xacNhanSucKhoe = xacNhanSucKhoe;
    }
    
    public String getTrangThai() {
        return trangThai;
    }
    
    public void setTrangThai(String trangThai) {
        this.trangThai = trangThai;
    }
    
    public String getMaDangKy() {
        return maDangKy;
    }
    
    public void setMaDangKy(String maDangKy) {
        this.maDangKy = maDangKy;
    }
    
    public String getTrangThaiThanhToan() {
        return trangThaiThanhToan;
    }
    
    public void setTrangThaiThanhToan(String trangThaiThanhToan) {
        this.trangThaiThanhToan = trangThaiThanhToan;
    }
    
    public LocalDateTime getNgayThanhToan() {
        return ngayThanhToan;
    }
    
    public void setNgayThanhToan(LocalDateTime ngayThanhToan) {
        this.ngayThanhToan = ngayThanhToan;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    public String getUserAgent() {
        return userAgent;
    }
    
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
    
    public LocalDateTime getNgayTao() {
        return ngayTao;
    }
    
    public void setNgayTao(LocalDateTime ngayTao) {
        this.ngayTao = ngayTao;
    }
    
    public LocalDateTime getNgayCapNhat() {
        return ngayCapNhat;
    }
    
    public void setNgayCapNhat(LocalDateTime ngayCapNhat) {
        this.ngayCapNhat = ngayCapNhat;
    }
    
    @Override
    public String toString() {
        return "DangKyThiDau{" +
                "id=" + id +
                ", idGiaiDau=" + idGiaiDau +
                ", hoTen='" + hoTen + '\'' +
                ", email='" + email + '\'' +
                ", noiDung='" + noiDung + '\'' +
                ", maDangKy='" + maDangKy + '\'' +
                ", trangThai='" + trangThai + '\'' +
                ", ngayTao=" + ngayTao +
                '}';
    }
}
