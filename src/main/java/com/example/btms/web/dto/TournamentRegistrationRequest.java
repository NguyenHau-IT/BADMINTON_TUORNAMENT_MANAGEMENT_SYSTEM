package com.example.btms.web.dto;

/**
 * DTO for Tournament Registration Request (Enhanced Version)
 * Used when users register for a tournament via API
 * 
 * @author BTMS Team
 * @version 1.0
 */
public class TournamentRegistrationRequest {
    
    // Participant info
    private String hoTen;
    private String email;
    private String dienThoai;
    private String ngaySinh; // Changed to String for JSON parsing
    private String gioiTinh;
    private String cccd;
    
    // Tournament info
    private Integer tournamentId;
    private String noiDung; // singles, doubles, mixed
    private String trinhDo; // skill level
    private String cauLacBo; // club name
    private String tenHlv; // coach name
    
    // Partner info (for doubles)
    private String tenDongDoi;
    private String emailDongDoi;
    private String sdtDongDoi;
    
    // Payment
    private String phuongThucThanhToan;
    
    // Terms
    private Boolean dongYDieuKhoan;
    private Boolean xacNhanSucKhoe;
    
    // Additional
    private String ghiChu;
    
    // Metadata
    private String ipAddress;
    private String userAgent;
    
    // Constructors
    public TournamentRegistrationRequest() {
    }
    
    // Getters and Setters
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
    
    public String getNgaySinh() {
        return ngaySinh;
    }
    
    public void setNgaySinh(String ngaySinh) {
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
    
    public String getSdtDongDoi() {
        return sdtDongDoi;
    }
    
    public void setSdtDongDoi(String sdtDongDoi) {
        this.sdtDongDoi = sdtDongDoi;
    }
    
    public Boolean getXacNhanSucKhoe() {
        return xacNhanSucKhoe;
    }
    
    public void setXacNhanSucKhoe(Boolean xacNhanSucKhoe) {
        this.xacNhanSucKhoe = xacNhanSucKhoe;
    }
    
    public Integer getTournamentId() {
        return tournamentId;
    }
    
    public void setTournamentId(Integer tournamentId) {
        this.tournamentId = tournamentId;
    }
    
    public String getNoiDung() {
        return noiDung;
    }
    
    public void setNoiDung(String noiDung) {
        this.noiDung = noiDung;
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
    
    public String getGhiChu() {
        return ghiChu;
    }
    
    public void setGhiChu(String ghiChu) {
        this.ghiChu = ghiChu;
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
}
