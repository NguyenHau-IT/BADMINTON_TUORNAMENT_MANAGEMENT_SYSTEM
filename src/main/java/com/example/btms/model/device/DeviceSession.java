package com.example.btms.model.device;

import java.time.LocalDateTime;

/**
 * Model lưu thông tin thiết bị đang truy cập web bấm điểm
 */
public class DeviceSession {
    private String sessionId;
    private String maTrongTai;
    private String tenTrongTai;
    private String deviceName; // Tên thiết bị (browser + OS)
    private String deviceId; // ID thiết bị (CPH2083, SM-G998B, iPhone13,2...)
    private String deviceModel; // Model thiết bị chi tiết
    private String ipAddress;
    private LocalDateTime loginTime;
    private LocalDateTime lastActivity;
    private boolean blocked;
    private boolean verified; // Trạng thái xác thực - admin phải duyệt

    public DeviceSession() {
    }

    public DeviceSession(String sessionId, String maTrongTai, String tenTrongTai,
            String deviceName, String ipAddress) {
        this.sessionId = sessionId;
        this.maTrongTai = maTrongTai;
        this.tenTrongTai = tenTrongTai;
        this.deviceName = deviceName;
        this.ipAddress = ipAddress;
        this.loginTime = LocalDateTime.now();
        this.lastActivity = LocalDateTime.now();
        this.blocked = false;
        this.verified = false; // Mặc định chưa xác thực
    }

    public DeviceSession(String sessionId, String maTrongTai, String tenTrongTai,
            String deviceName, String deviceId, String deviceModel, String ipAddress) {
        this.sessionId = sessionId;
        this.maTrongTai = maTrongTai;
        this.tenTrongTai = tenTrongTai;
        this.deviceName = deviceName;
        this.deviceId = deviceId;
        this.deviceModel = deviceModel;
        this.ipAddress = ipAddress;
        this.loginTime = LocalDateTime.now();
        this.lastActivity = LocalDateTime.now();
        this.blocked = false;
        this.verified = false; // Mặc định chưa xác thực
    }

    // Getters and Setters
    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getMaTrongTai() {
        return maTrongTai;
    }

    public void setMaTrongTai(String maTrongTai) {
        this.maTrongTai = maTrongTai;
    }

    public String getTenTrongTai() {
        return tenTrongTai;
    }

    public void setTenTrongTai(String tenTrongTai) {
        this.tenTrongTai = tenTrongTai;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public LocalDateTime getLoginTime() {
        return loginTime;
    }

    public void setLoginTime(LocalDateTime loginTime) {
        this.loginTime = loginTime;
    }

    public LocalDateTime getLastActivity() {
        return lastActivity;
    }

    public void setLastActivity(LocalDateTime lastActivity) {
        this.lastActivity = lastActivity;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceModel() {
        return deviceModel;
    }

    public void setDeviceModel(String deviceModel) {
        this.deviceModel = deviceModel;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public void updateActivity() {
        this.lastActivity = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "DeviceSession{" +
                "sessionId='" + sessionId + '\'' +
                ", maTrongTai='" + maTrongTai + '\'' +
                ", tenTrongTai='" + tenTrongTai + '\'' +
                ", deviceName='" + deviceName + '\'' +
                ", deviceId='" + deviceId + '\'' +
                ", deviceModel='" + deviceModel + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", loginTime=" + loginTime +
                ", lastActivity=" + lastActivity +
                ", blocked=" + blocked +
                ", verified=" + verified +
                '}';
    }
}
