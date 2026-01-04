package com.example.btms.model.cateoftuornament;

public class ChiTietGiaiDau {
    private int idGiaiDau;
    private int idNoiDung;
    private int tuoiDuoi;
    private int tuoiTren;
    private int soDo;

    public ChiTietGiaiDau(int idGiaiDau, int idNoiDung, int tuoiDuoi, int tuoiTren, int soDo) {
        this.idGiaiDau = idGiaiDau;
        this.idNoiDung = idNoiDung;
        this.tuoiDuoi = tuoiDuoi;
        this.tuoiTren = tuoiTren;
        this.soDo = soDo;
    }

    public int getIdGiaiDau() {
        return idGiaiDau;
    }

    public void setIdGiaiDau(int idGiaiDau) {
        this.idGiaiDau = idGiaiDau;
    }

    public int getIdNoiDung() {
        return idNoiDung;
    }

    public void setIdNoiDung(int idNoiDung) {
        this.idNoiDung = idNoiDung;
    }

    public int getTuoiDuoi() {
        return tuoiDuoi;
    }

    public void setTuoiDuoi(int tuoiDuoi) {
        this.tuoiDuoi = tuoiDuoi;
    }

    public int getTuoiTren() {
        return tuoiTren;
    }

    public void setTuoiTren(int tuoiTren) {
        this.tuoiTren = tuoiTren;
    }

    public int getSoDo() {
        return soDo;
    }

    public void setSoDo(int soDo) {
        this.soDo = soDo;
    }
}
