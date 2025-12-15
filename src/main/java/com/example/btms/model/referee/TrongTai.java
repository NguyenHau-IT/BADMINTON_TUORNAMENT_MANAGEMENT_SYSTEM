package com.example.btms.model.referee;

import java.time.LocalDate;

/**
 * POJO class representing a referee (Trọng tài)
 * Maps to the TRONG_TAI table in the database
 */
public class TrongTai {

    private String maTrongTai;
    private String hoTen;
    private LocalDate ngaySinh;
    private Boolean gioiTinh; // true = Nam, false = Nữ (BIT)
    private String soDienThoai;
    private String email;
    private String matKhau;
    private Integer idClb;
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
