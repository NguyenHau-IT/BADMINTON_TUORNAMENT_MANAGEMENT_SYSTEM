package com.example.btms.web.dto;

/**
 * DTO cho thống kê chi tiết nội dung thi đấu
 */
public class ContentStatsDTO {

    private String tenNoiDung;
    private int soVDV;
    private int soTranDuKien;
    private int soTranDaThiDau;
    private String trangThai;

    public ContentStatsDTO(String tenNoiDung, int soVDV, int soTranDuKien, int soTranDaThiDau, String trangThai) {
        this.tenNoiDung = tenNoiDung;
        this.soVDV = soVDV;
        this.soTranDuKien = soTranDuKien;
        this.soTranDaThiDau = soTranDaThiDau;
        this.trangThai = trangThai;
    }

    // Getters and Setters
    public String getTenNoiDung() {
        return tenNoiDung;
    }

    public void setTenNoiDung(String tenNoiDung) {
        this.tenNoiDung = tenNoiDung;
    }

    public int getSoVDV() {
        return soVDV;
    }

    public void setSoVDV(int soVDV) {
        this.soVDV = soVDV;
    }

    public int getSoTranDuKien() {
        return soTranDuKien;
    }

    public void setSoTranDuKien(int soTranDuKien) {
        this.soTranDuKien = soTranDuKien;
    }

    public int getSoTranDaThiDau() {
        return soTranDaThiDau;
    }

    public void setSoTranDaThiDau(int soTranDaThiDau) {
        this.soTranDaThiDau = soTranDaThiDau;
    }

    public String getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(String trangThai) {
        this.trangThai = trangThai;
    }

    @Override
    public String toString() {
        return "ContentStatsDTO{" +
                "tenNoiDung='" + tenNoiDung + '\'' +
                ", soVDV=" + soVDV +
                ", soTranDuKien=" + soTranDuKien +
                ", soTranDaThiDau=" + soTranDaThiDau +
                ", trangThai='" + trangThai + '\'' +
                '}';
    }
}