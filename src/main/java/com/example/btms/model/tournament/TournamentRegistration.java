package com.example.btms.model.tournament;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "DANG_KY_THI_DAU")
public class TournamentRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @Column(name = "ID_GIAI_DAU", nullable = false)
    private Integer tournamentId;

    @Column(name = "MA_DANG_KY", nullable = false, unique = true, length = 100)
    private String registrationCode;

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

    @Column(name = "NOI_DUNG", nullable = false, length = 50)
    private String noiDung;

    @Column(name = "TRINH_DO", length = 50)
    private String trinhDo;

    @Column(name = "CAU_LAC_BO", length = 200)
    private String cauLacBo;

    @Column(name = "TEN_DONG_DOI", length = 200)
    private String tenDongDoi;

    @Column(name = "EMAIL_DONG_DOI", length = 200)
    private String emailDongDoi;

    @Column(name = "PHUONG_THUC_THANH_TOAN", nullable = false, length = 50)
    private String phuongThucThanhToan;

    @Column(name = "DONG_Y_DIEU_KHOAN", nullable = false)
    private Boolean dongYDieuKhoan;

    @Column(name = "XAC_NHAN_SUC_KHOE", nullable = false)
    private Boolean xacNhanSucKhoe;

    @Column(name = "GHI_CHU", columnDefinition = "NVARCHAR(MAX)")
    private String ghiChu;

    @Column(name = "TRANG_THAI", nullable = false, length = 50)
    private String trangThai; // pending, approved, rejected, cancelled

    @Column(name = "IP_ADDRESS", length = 50)
    private String ipAddress;

    @Column(name = "USER_AGENT", length = 500)
    private String userAgent;

    @Column(name = "NGAY_TAO", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "NGAY_CAP_NHAT", nullable = false)
    private LocalDateTime updatedAt;

    // Constructors
    public TournamentRegistration() {
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getTournamentId() {
        return tournamentId;
    }

    public void setTournamentId(Integer tournamentId) {
        this.tournamentId = tournamentId;
    }

    public String getRegistrationCode() {
        return registrationCode;
    }

    public void setRegistrationCode(String registrationCode) {
        this.registrationCode = registrationCode;
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

    public String getPhuongThucThanhToan() {
        return phuongThucThanhToan;
    }

    public void setPhuongThucThanhToan(String phuongThucThanhToan) {
        this.phuongThucThanhToan = phuongThucThanhToan;
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

    public String getGhiChu() {
        return ghiChu;
    }

    public void setGhiChu(String ghiChu) {
        this.ghiChu = ghiChu;
    }

    public String getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(String trangThai) {
        this.trangThai = trangThai;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now; // Set initial value for updatedAt
        if (trangThai == null) {
            trangThai = "pending";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
