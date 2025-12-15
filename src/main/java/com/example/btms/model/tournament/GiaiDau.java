package com.example.btms.model.tournament;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * POJO class cho bảng GIAI_DAU
 * Enhanced với Web Platform fields
 * Database: SQL Server
 * 
 * @author BTMS Team
 * @version 2.0 - Web Platform Integration
 * @since 2025-11-19
 */
public class GiaiDau {

    // ===== ORIGINAL FIELDS (Desktop App) =====

    private Integer id;
    private String tenGiai;
    private LocalDate ngayBd;
    private LocalDate ngayKt;
    private LocalDateTime ngayTao;
    private LocalDateTime ngayCapNhat;
    private Integer idUser;

    /**
     * Default constructor
     */
    public GiaiDau() {
    }

    /**
     * Constructor for Desktop App (backward compatibility)
     */
    public GiaiDau(String tenGiai, LocalDate ngayBd, LocalDate ngayKt, Integer idUser) {
        this.tenGiai = tenGiai;
        this.ngayBd = ngayBd;
        this.ngayKt = ngayKt;
        this.idUser = idUser;
        this.ngayTao = LocalDateTime.now();
        this.ngayCapNhat = LocalDateTime.now();
    }

    /**
     * Full constructor for Web Platform
     */
    public GiaiDau(String tenGiai, LocalDate ngayBd, LocalDate ngayKt, Integer idUser,
            String moTa, String diaDiem, String tinhThanh) {
        this(tenGiai, ngayBd, ngayKt, idUser);
    }

    // ===== GETTERS AND SETTERS - ORIGINAL FIELDS =====

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTenGiai() {
        return tenGiai;
    }

    public void setTenGiai(String tenGiai) {
        this.tenGiai = tenGiai;
    }

    public LocalDate getNgayBd() {
        return ngayBd;
    }

    public void setNgayBd(LocalDate ngayBd) {
        this.ngayBd = ngayBd;
    }

    public LocalDate getNgayKt() {
        return ngayKt;
    }

    public void setNgayKt(LocalDate ngayKt) {
        this.ngayKt = ngayKt;
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

    public Integer getIdUser() {
        return idUser;
    }

    public void setIdUser(Integer idUser) {
        this.idUser = idUser;
    }

    /**
     * Check if tournament is currently active/ongoing
     */
    public boolean isActive() {
        LocalDate now = LocalDate.now();
        return ngayBd != null && ngayKt != null &&
                (now.isEqual(ngayBd) || now.isAfter(ngayBd)) &&
                (now.isEqual(ngayKt) || now.isBefore(ngayKt));
    }

    /**
     * Check if tournament is upcoming
     */
    public boolean isUpcoming() {
        LocalDate now = LocalDate.now();
        return ngayBd != null && now.isBefore(ngayBd);
    }

    /**
     * Check if tournament is finished
     */
    public boolean isFinished() {
        LocalDate now = LocalDate.now();
        return ngayKt != null && now.isAfter(ngayKt);
    }

    // ===== OBJECT METHODS =====

    @Override
    public String toString() {
        return "GiaiDau{" +
                "id=" + id +
                ", tenGiai='" + tenGiai + '\'' +
                ", ngayBd=" + ngayBd +
                ", ngayKt=" + ngayKt +
                ", ngayTao=" + ngayTao +
                ", ngayCapNhat=" + ngayCapNhat +
                ", idUser=" + idUser +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        GiaiDau giaiDau = (GiaiDau) o;
        return id != null && id.equals(giaiDau.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
