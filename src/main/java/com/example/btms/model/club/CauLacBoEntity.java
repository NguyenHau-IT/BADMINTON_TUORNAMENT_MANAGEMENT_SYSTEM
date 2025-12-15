package com.example.btms.model.club;

import jakarta.persistence.*;

/**
 * JPA Entity cho bảng CAU_LAC_BO
 * Dùng cho Web Platform - Rankings module
 * 
 * @author BTMS Team
 * @version 2.0 - Web Platform
 */
@Entity
@Table(name = "CAU_LAC_BO")
public class CauLacBoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @Column(name = "TEN_CLB", nullable = false, length = 255)
    private String tenClb;

    @Column(name = "TEN_NGAN", length = 50)
    private String tenNgan; // Tên viết tắt

    // ========== Transient fields (tính toán) ==========
    
    @Transient
    private Integer soThanhVien; // Số thành viên CLB
    
    @Transient
    private Integer tongDanhHieu; // Tổng danh hiệu
    
    @Transient
    private Integer diemXepHang; // Điểm xếp hạng
    
    @Transient
    private Integer xepHang; // Thứ hạng

    // ========== Constructors ==========
    
    public CauLacBoEntity() {
    }

    public CauLacBoEntity(String tenClb, String tenNgan) {
        this.tenClb = tenClb;
        this.tenNgan = tenNgan;
    }

    // ========== Getters and Setters ==========

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTenClb() {
        return tenClb;
    }

    public void setTenClb(String tenClb) {
        this.tenClb = tenClb;
    }

    public String getTenNgan() {
        return tenNgan;
    }

    public void setTenNgan(String tenNgan) {
        this.tenNgan = tenNgan;
    }

    public Integer getSoThanhVien() {
        return soThanhVien;
    }

    public void setSoThanhVien(Integer soThanhVien) {
        this.soThanhVien = soThanhVien;
    }

    public Integer getTongDanhHieu() {
        return tongDanhHieu;
    }

    public void setTongDanhHieu(Integer tongDanhHieu) {
        this.tongDanhHieu = tongDanhHieu;
    }

    public Integer getDiemXepHang() {
        return diemXepHang;
    }

    public void setDiemXepHang(Integer diemXepHang) {
        this.diemXepHang = diemXepHang;
    }

    public Integer getXepHang() {
        return xepHang;
    }

    public void setXepHang(Integer xepHang) {
        this.xepHang = xepHang;
    }

    @Override
    public String toString() {
        return "CauLacBoEntity{" +
                "id=" + id +
                ", tenClb='" + tenClb + '\'' +
                ", tenNgan='" + tenNgan + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CauLacBoEntity that = (CauLacBoEntity) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
