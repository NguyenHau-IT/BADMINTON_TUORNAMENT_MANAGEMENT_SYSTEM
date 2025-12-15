package com.example.btms.service.device;

import com.example.btms.model.device.DeviceSession;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service quản lý các thiết bị đang truy cập web bấm điểm
 */
@Service
public class DeviceSessionService {

    // Map lưu sessionId -> DeviceSession
    private final Map<String, DeviceSession> activeSessions = new ConcurrentHashMap<>();

    // Set lưu các sessionId bị block
    private final Map<String, Boolean> blockedSessions = new ConcurrentHashMap<>();

    /**
     * Đăng ký session mới khi trọng tài login
     */
    public void registerSession(String sessionId, String maTrongTai, String tenTrongTai,
            String deviceName, String ipAddress) {
        DeviceSession session = new DeviceSession(sessionId, maTrongTai, tenTrongTai,
                deviceName, ipAddress);
        activeSessions.put(sessionId, session);
    }

    /**
     * Đăng ký session với thông tin thiết bị chi tiết
     */
    public void registerSession(String sessionId, String maTrongTai, String tenTrongTai,
            String deviceName, String deviceId, String deviceModel, String ipAddress) {
        DeviceSession session = new DeviceSession(sessionId, maTrongTai, tenTrongTai,
                deviceName, deviceId, deviceModel, ipAddress);
        activeSessions.put(sessionId, session);
    }

    /**
     * Cập nhật hoạt động của session
     */
    public void updateActivity(String sessionId) {
        DeviceSession session = activeSessions.get(sessionId);
        if (session != null) {
            session.updateActivity();
        }
    }

    /**
     * Xóa session khi logout hoặc timeout
     */
    public void removeSession(String sessionId) {
        activeSessions.remove(sessionId);
        blockedSessions.remove(sessionId);
    }

    /**
     * Block một session - không cho truy cập nữa
     */
    public void blockSession(String sessionId) {
        DeviceSession session = activeSessions.get(sessionId);
        if (session != null) {
            session.setBlocked(true);
            blockedSessions.put(sessionId, true);
        }
    }

    /**
     * Unblock một session
     */
    public void unblockSession(String sessionId) {
        DeviceSession session = activeSessions.get(sessionId);
        if (session != null) {
            session.setBlocked(false);
            blockedSessions.remove(sessionId);
        }
    }

    /**
     * Kiểm tra xem session có bị block không
     */
    public boolean isBlocked(String sessionId) {
        return blockedSessions.containsKey(sessionId);
    }

    /**
     * Kiểm tra xem session có tồn tại trong hệ thống không
     */
    public boolean sessionExists(String sessionId) {
        return activeSessions.containsKey(sessionId);
    }

    /**
     * Xác thực/duyệt một session
     */
    public void verifySession(String sessionId) {
        DeviceSession session = activeSessions.get(sessionId);
        if (session != null) {
            session.setVerified(true);
        }
    }

    /**
     * Hủy xác thực một session
     */
    public void unverifySession(String sessionId) {
        DeviceSession session = activeSessions.get(sessionId);
        if (session != null) {
            session.setVerified(false);
        }
    }

    /**
     * Kiểm tra xem session đã được xác thực chưa
     */
    public boolean isVerified(String sessionId) {
        DeviceSession session = activeSessions.get(sessionId);
        return session != null && session.isVerified();
    }

    /**
     * Lấy tất cả sessions đang active
     */
    public List<DeviceSession> getAllSessions() {
        return new ArrayList<>(activeSessions.values());
    }

    /**
     * Lấy session theo sessionId
     */
    public DeviceSession getSession(String sessionId) {
        return activeSessions.get(sessionId);
    }

    /**
     * Lấy tất cả sessions của một trọng tài
     */
    public List<DeviceSession> getSessionsByReferee(String maTrongTai) {
        return activeSessions.values().stream()
                .filter(s -> s.getMaTrongTai().equals(maTrongTai))
                .collect(Collectors.toList());
    }

    /**
     * Đếm số session đang active
     */
    public int getActiveSessionCount() {
        return activeSessions.size();
    }

    /**
     * Xóa tất cả sessions (dùng khi restart hệ thống)
     */
    public void clearAllSessions() {
        activeSessions.clear();
        blockedSessions.clear();
    }
}
