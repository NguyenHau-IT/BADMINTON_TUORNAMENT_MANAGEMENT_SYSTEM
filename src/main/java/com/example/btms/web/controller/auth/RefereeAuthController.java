package com.example.btms.web.controller.auth;

import com.example.btms.service.referee.TrongTaiService;
import com.example.btms.service.device.DeviceSessionService;
import com.example.btms.model.referee.TrongTai;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Controller xử lý đăng nhập trọng tài cho trang /pin
 */
@RestController
@RequestMapping("/api/referee")
public class RefereeAuthController {

    @Autowired
    private TrongTaiService trongTaiService;

    @Autowired
    private DeviceSessionService deviceSessionService;

    /**
     * Kiểm tra trạng thái đăng nhập
     */
    @GetMapping("/check-auth")
    public ResponseEntity<Map<String, Object>> checkAuth(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        String sessionId = session.getId();

        // Kiểm tra session có bị block không
        if (deviceSessionService.isBlocked(sessionId)) {
            // Invalidate session khi bị block
            session.invalidate();
            response.put("isLoggedIn", false);
            response.put("blocked", true);
            response.put("message", "Thiết bị của bạn đã bị chặn bởi quản trị viên");
            return ResponseEntity.ok(response);
        }

        String maTrongTai = (String) session.getAttribute("loggedInReferee");

        if (maTrongTai != null) {
            // Kiểm tra session có còn tồn tại trong DeviceSessionService không
            if (!deviceSessionService.sessionExists(sessionId)) {
                // Session đã bị xóa (kicked), invalidate session
                session.invalidate();
                response.put("isLoggedIn", false);
                response.put("kicked", true);
                response.put("message", "Phiên đăng nhập của bạn đã bị đóng. Vui lòng đăng nhập lại.");
                return ResponseEntity.ok(response);
            }

            Optional<TrongTai> trongTai = trongTaiService.getTrongTaiById(maTrongTai);
            if (trongTai.isPresent()) {
                // Cập nhật hoạt động
                deviceSessionService.updateActivity(sessionId);

                // Kiểm tra trạng thái xác thực
                boolean verified = deviceSessionService.isVerified(sessionId);

                response.put("isLoggedIn", true);
                response.put("verified", verified);
                response.put("maTrongTai", maTrongTai);
                response.put("hoTen", trongTai.get().getHoTen());

                if (!verified) {
                    response.put("message", "Đang chờ quản trị viên duyệt...");
                }

                return ResponseEntity.ok(response);
            }
        }

        response.put("isLoggedIn", false);
        return ResponseEntity.ok(response);
    }

    /**
     * Đăng nhập trọng tài
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @RequestBody Map<String, String> credentials,
            HttpSession session,
            HttpServletRequest request) {

        Map<String, Object> response = new HashMap<>();
        String maTrongTai = credentials.get("maTrongTai");
        String matKhau = credentials.get("matKhau");

        if (maTrongTai == null || maTrongTai.trim().isEmpty() ||
                matKhau == null || matKhau.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "Vui lòng nhập đầy đủ mã trọng tài và mật khẩu");
            return ResponseEntity.badRequest().body(response);
        }

        Optional<TrongTai> trongTaiOpt = trongTaiService.getTrongTaiById(maTrongTai);

        if (trongTaiOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Mã trọng tài không tồn tại");
            return ResponseEntity.ok(response);
        }

        TrongTai trongTai = trongTaiOpt.get();

        // Kiểm tra mật khẩu
        if (!matKhau.equals(trongTai.getMatKhau())) {
            response.put("success", false);
            response.put("message", "Mật khẩu không đúng");
            return ResponseEntity.ok(response);
        }

        // Đăng nhập thành công
        session.setAttribute("loggedInReferee", maTrongTai);
        session.setAttribute("refereeHoTen", trongTai.getHoTen());

        // Lưu thông tin thiết bị
        String sessionId = session.getId();
        String deviceName = getDeviceName(request);
        String deviceId = credentials.get("deviceId"); // Lấy từ client
        String deviceModel = credentials.get("deviceModel"); // Lấy từ client
        String ipAddress = getClientIp(request);

        // Nếu có deviceId từ client thì dùng method mới, không thì dùng method cũ
        if (deviceId != null && !deviceId.isEmpty()) {
            deviceSessionService.registerSession(sessionId, maTrongTai,
                    trongTai.getHoTen(), deviceName, deviceId, deviceModel, ipAddress);
        } else {
            deviceSessionService.registerSession(sessionId, maTrongTai,
                    trongTai.getHoTen(), deviceName, ipAddress);
        }

        response.put("success", true);
        response.put("message", "Đăng nhập thành công");
        response.put("maTrongTai", maTrongTai);
        response.put("hoTen", trongTai.getHoTen());

        return ResponseEntity.ok(response);
    }

    /**
     * Đăng xuất trọng tài
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpSession session) {
        Map<String, Object> response = new HashMap<>();

        String sessionId = session.getId();
        deviceSessionService.removeSession(sessionId);

        session.removeAttribute("loggedInReferee");
        session.removeAttribute("refereeHoTen");

        response.put("success", true);
        response.put("message", "Đăng xuất thành công");

        return ResponseEntity.ok(response);
    }

    /**
     * Lấy tên thiết bị từ User-Agent
     */
    private String getDeviceName(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null || userAgent.isEmpty()) {
            return "Unknown Device";
        }

        // Parse User-Agent để lấy thông tin browser và OS
        String browser = "Unknown Browser";
        String os = "Unknown OS";

        // Detect OS
        if (userAgent.contains("Windows")) {
            os = "Windows";
        } else if (userAgent.contains("Mac")) {
            os = "MacOS";
        } else if (userAgent.contains("Linux")) {
            os = "Linux";
        } else if (userAgent.contains("Android")) {
            os = "Android";
        } else if (userAgent.contains("iOS") || userAgent.contains("iPhone") || userAgent.contains("iPad")) {
            os = "iOS";
        }

        // Detect Browser
        if (userAgent.contains("Edg")) {
            browser = "Edge";
        } else if (userAgent.contains("Chrome") && !userAgent.contains("Edg")) {
            browser = "Chrome";
        } else if (userAgent.contains("Firefox")) {
            browser = "Firefox";
        } else if (userAgent.contains("Safari") && !userAgent.contains("Chrome")) {
            browser = "Safari";
        } else if (userAgent.contains("Opera") || userAgent.contains("OPR")) {
            browser = "Opera";
        }

        return browser + " (" + os + ")";
    }

    /**
     * Lấy IP address của client
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // Nếu có nhiều IP (proxy chain), lấy IP đầu tiên
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip != null ? ip : "Unknown";
    }
}
