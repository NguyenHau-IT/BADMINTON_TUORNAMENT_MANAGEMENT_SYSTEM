package com.example.btms.model.referee;

import java.util.Objects;

/**
 * POJO class representing referee assignment (Phân công trọng tài)
 * Maps to the PHAN_CONG_TRONG_TAI table in the database
 */
public class PhanCongTrongTai {

    private String maPhanCong;
    private String maTrongTai;
    private String maTranDau;
    private String vaiTro;
    private String ghiChu;

    // Constructors
    public PhanCongTrongTai() {
    }

    public PhanCongTrongTai(String maPhanCong, String maTrongTai, String maTranDau) {
        this.maPhanCong = maPhanCong;
        this.maTrongTai = maTrongTai;
        this.maTranDau = maTranDau;
    }

    public PhanCongTrongTai(String maPhanCong, String maTrongTai, String maTranDau, String vaiTro) {
        this.maPhanCong = maPhanCong;
        this.maTrongTai = maTrongTai;
        this.maTranDau = maTranDau;
        this.vaiTro = vaiTro;
    }

    // Getters and Setters
    public String getMaPhanCong() {
        return maPhanCong;
    }

    public void setMaPhanCong(String maPhanCong) {
        this.maPhanCong = maPhanCong;
    }

    public String getMaTrongTai() {
        return maTrongTai;
    }

    public void setMaTrongTai(String maTrongTai) {
        this.maTrongTai = maTrongTai;
    }

    public String getMaTranDau() {
        return maTranDau;
    }

    public void setMaTranDau(String maTranDau) {
        this.maTranDau = maTranDau;
    }

    public String getVaiTro() {
        return vaiTro;
    }

    public void setVaiTro(String vaiTro) {
        this.vaiTro = vaiTro;
    }

    public String getGhiChu() {
        return ghiChu;
    }

    public void setGhiChu(String ghiChu) {
        this.ghiChu = ghiChu;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        PhanCongTrongTai that = (PhanCongTrongTai) o;
        return Objects.equals(maPhanCong, that.maPhanCong);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maPhanCong);
    }

    @Override
    public String toString() {
        return "PhanCongTrongTai{" +
                "maPhanCong='" + maPhanCong + '\'' +
                ", maTrongTai='" + maTrongTai + '\'' +
                ", maTranDau='" + maTranDau + '\'' +
                ", vaiTro='" + vaiTro + '\'' +
                ", ghiChu='" + ghiChu + '\'' +
                '}';
    }
}
