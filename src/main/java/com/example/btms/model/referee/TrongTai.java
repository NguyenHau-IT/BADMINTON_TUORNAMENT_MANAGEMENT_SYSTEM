package com.example.btms.model.referee;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * Entity class representing a referee (Trọng tài)
 * Maps to the TRONG_TAI table in the database
 */
@Entity
@Table(name = "TRONG_TAI")
public class TrongTai {

    @Id
    @Column(name = "MA_TRONG_TAI", length = 30, nullable = false)
    private String maTrongTai;

    @Column(name = "HO_TEN", nullable = false, length = 100, columnDefinition = "NVARCHAR(100)")
    private String hoTen;

    @Column(name = "NGAY_SINH")
    private LocalDate ngaySinh;

    @Column(name = "GIOI_TINH")
    private Boolean gioiTinh; // true = Nam, false = Nữ (BIT)

    @Column(name = "SO_DIEN_THOAI", length = 20)
    private String soDienThoai;

    @Column(name = "EMAIL", length = 100, columnDefinition = "NVARCHAR(100)")
    private String email;

    @Column(name = "MAT_KHAU", nullable = false, length = 255, columnDefinition = "NVARCHAR(255)")
    private String matKhau;

    @Column(name = "IDCLB")
    private Integer idClb;

    @Column(name = "GHI_CHU", columnDefinition = "NVARCHAR(MAX)")
    private String ghiChu;

    // Constructors
    public TrongTai() {
        this.gioiTinh = true; // Default = Nam
    }

    public TrongTai(String maTrongTai, String hoTen, String matKhau) {
        this.maTrongTai = maTrongTai;
        this.hoTen = hoTen;
        this.matKhau = matKhau;
        this.gioiTinh = true;
    }

    // Getters and Setters
    public String getMaTrongTai() {
        return maTrongTai;
    }

    public void setMaTrongTai(String maTrongTai) {
        this.maTrongTai = maTrongTai;
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

    public Boolean getGioiTinh() {
        return gioiTinh;
    }

    public void setGioiTinh(Boolean gioiTinh) {
        this.gioiTinh = gioiTinh;
    }

    public String getSoDienThoai() {
        return soDienThoai;
    }

    public void setSoDienThoai(String soDienThoai) {
        this.soDienThoai = soDienThoai;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMatKhau() {
        return matKhau;
    }

    public void setMatKhau(String matKhau) {
        this.matKhau = matKhau;
    }

    public Integer getIdClb() {
        return idClb;
    }

    public void setIdClb(Integer idClb) {
        this.idClb = idClb;
    }

    public String getGhiChu() {
        return ghiChu;
    }

    public void setGhiChu(String ghiChu) {
        this.ghiChu = ghiChu;
    }

    @Override
    public String toString() {
        return "TrongTai{" +
                "maTrongTai='" + maTrongTai + '\'' +
                ", hoTen='" + hoTen + '\'' +
                ", ngaySinh=" + ngaySinh +
                ", gioiTinh=" + gioiTinh +
                ", soDienThoai='" + soDienThoai + '\'' +
                ", email='" + email + '\'' +
                ", idClb=" + idClb +
                '}';
    }
}
