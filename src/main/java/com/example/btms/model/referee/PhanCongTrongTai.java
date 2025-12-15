package com.example.btms.model.referee;

import jakarta.persistence.*;
import java.util.Objects;

/**
 * Entity class representing referee assignment (Phân công trọng tài)
 * Maps to the PHAN_CONG_TRONG_TAI table in the database
 */
@Entity
@Table(name = "PHAN_CONG_TRONG_TAI")
public class PhanCongTrongTai {

    @Id
    @Column(name = "MA_PHAN_CONG", length = 30, nullable = false)
    private String maPhanCong;

    @Column(name = "MA_TRONG_TAI", length = 30, nullable = false)
    private String maTrongTai;

    @Column(name = "MA_TRAN_DAU", length = 36, columnDefinition = "CHAR(36)")
    private String maTranDau;

    @Column(name = "VAI_TRO", length = 100, columnDefinition = "NVARCHAR(100)")
    private String vaiTro;

    @Column(name = "GHI_CHU", columnDefinition = "NVARCHAR(MAX)")
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
