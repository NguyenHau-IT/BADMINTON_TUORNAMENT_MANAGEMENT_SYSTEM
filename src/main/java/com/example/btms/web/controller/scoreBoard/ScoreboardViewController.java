package com.example.btms.web.controller.scoreBoard;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.example.btms.service.device.DeviceSessionService;
import com.example.btms.service.match.CourtManagerService;
import com.example.btms.service.scoreboard.ScoreboardRemote;

import jakarta.servlet.http.HttpSession;

/**
 * Controller xử lý web interface cho scoreboard
 * Route: /scoreboard/{pin} - hiển thị giao diện nhập điểm
 */
@Controller
public class ScoreboardViewController {

    @Autowired
    private DeviceSessionService deviceSessionService;

    /**
     * Hiển thị giao diện scoreboard với mã PIN
     */
    @GetMapping("/scoreboard/{pin}")
    public String showScoreboard(@PathVariable String pin, Model model, HttpSession session) {
        // 🔐 Kiểm tra authentication và verification
        String sessionId = session.getId();

        // Check if session exists
        if (!deviceSessionService.sessionExists(sessionId)) {
            model.addAttribute("error", "Vui lòng đăng nhập trước");
            model.addAttribute("message", "Bạn cần đăng nhập với tài khoản trọng tài để truy cập trang này.");
            model.addAttribute("redirectUrl", "/pin");
            return "exception/auth-required";
        }

        // Check if blocked
        if (deviceSessionService.isBlocked(sessionId)) {
            model.addAttribute("error", "Thiết bị bị chặn");
            model.addAttribute("message",
                    "Thiết bị của bạn đã bị chặn bởi quản trị viên. Vui lòng liên hệ để được hỗ trợ.");
            return "exception/blocked";
        }

        // Check if verified
        if (!deviceSessionService.isVerified(sessionId)) {
            model.addAttribute("error", "Chờ phê duyệt");
            model.addAttribute("message",
                    "Đang chờ quản trị viên duyệt thiết bị của bạn. Vui lòng chờ trong giây lát.");
            model.addAttribute("redirectUrl", "/pin");
            return "exception/waiting-approval";
        }
        // Kiểm tra PIN có hợp lệ không
        if (pin == null || pin.trim().isEmpty()) {
            model.addAttribute("error", "Mã PIN không hợp lệ");
            return "exception/error";
        }

        // Lấy thông tin sân từ CourtManagerService
        CourtManagerService courtManager = CourtManagerService.getInstance();
        java.util.Map<String, CourtManagerService.CourtStatus> allCourts = courtManager.getAllCourtStatus();

        // Tìm sân có mã PIN tương ứng
        String courtId = null;
        String header = null;
        for (CourtManagerService.CourtStatus court : allCourts.values()) {
            if (pin.equals(court.pinCode)) {
                courtId = court.courtId;
                header = court.header;
                break;
            }
        }

        // Lấy thông tin match hiện tại
        var match = ScoreboardRemote.get().match();
        var snapshot = match.snapshot();

        // Truyền dữ liệu vào model
        model.addAttribute("pinCode", pin); // Đổi tên để khớp với template
        model.addAttribute("courtInfo", courtId != null ? courtId : "Sân"); // Thông tin sân
        model.addAttribute("header", header); // Nội dung trận đấu
        model.addAttribute("pin", pin);
        model.addAttribute("match", snapshot);
        model.addAttribute("names", snapshot.names);
        model.addAttribute("score", snapshot.score);
        model.addAttribute("games", snapshot.games);
        model.addAttribute("gameNumber", snapshot.gameNumber);
        model.addAttribute("bestOf", snapshot.bestOf);
        model.addAttribute("doubles", snapshot.doubles);

        return "scoreboard/scoreboard";
    }

    /**
     * Hiển thị giao diện nhập PIN
     */
    @GetMapping("/pin")
    public String showPinEntry() {
        return "pin/pin-entry";
    }

    /**
     * Hiển thị trang kết quả trận đấu khi kết thúc
     */
    @GetMapping("/result/{pin}")
    public String showMatchResult(@PathVariable String pin, Model model, HttpSession session) {
        // 🔐 Kiểm tra authentication và verification
        String sessionId = session.getId();

        // Check if session exists
        if (!deviceSessionService.sessionExists(sessionId)) {
            model.addAttribute("error", "Vui lòng đăng nhập trước");
            model.addAttribute("message", "Bạn cần đăng nhập với tài khoản trọng tài để truy cập trang này.");
            model.addAttribute("redirectUrl", "/pin");
            return "exception/auth-required";
        }

        // Check if blocked
        if (deviceSessionService.isBlocked(sessionId)) {
            model.addAttribute("error", "Thiết bị bị chặn");
            model.addAttribute("message",
                    "Thiết bị của bạn đã bị chặn bởi quản trị viên. Vui lòng liên hệ để được hỗ trợ.");
            return "exception/blocked";
        }

        // Check if verified
        if (!deviceSessionService.isVerified(sessionId)) {
            model.addAttribute("error", "Chờ phê duyệt");
            model.addAttribute("message",
                    "Đang chờ quản trị viên duyệt thiết bị của bạn. Vui lòng chờ trong giây lát.");
            model.addAttribute("redirectUrl", "/pin");
            return "exception/waiting-approval";
        }

        // Kiểm tra PIN có hợp lệ không
        if (pin == null || pin.trim().isEmpty()) {
            model.addAttribute("error", "Mã PIN không hợp lệ");
            return "exception/error";
        }

        // Lấy thông tin sân từ CourtManagerService
        CourtManagerService courtManager = CourtManagerService.getInstance();
        java.util.Map<String, CourtManagerService.CourtStatus> allCourts = courtManager.getAllCourtStatus();

        // Tìm sân có mã PIN tương ứng
        String courtId = null;
        String header = null;
        for (CourtManagerService.CourtStatus court : allCourts.values()) {
            if (pin.equals(court.pinCode)) {
                courtId = court.courtId;
                header = court.header;
                break;
            }
        }

        // Lấy thông tin match từ ScoreboardPinController
        var matchData = com.example.btms.web.controller.scoreBoard.ScoreboardPinController.getMatchByPin(pin);
        if (matchData == null) {
            // Fallback to ScoreboardRemote if no PIN-specific match
            var match = ScoreboardRemote.get().match();
            matchData = match;
        }

        var snapshot = matchData.snapshot();

        // Truyền dữ liệu vào model
        model.addAttribute("pinCode", pin);
        model.addAttribute("courtInfo", courtId != null ? courtId : "Sân");
        model.addAttribute("header", header);
        model.addAttribute("pin", pin);
        model.addAttribute("match", snapshot);
        model.addAttribute("names", snapshot.names);
        model.addAttribute("score", snapshot.score);
        model.addAttribute("games", snapshot.games);
        model.addAttribute("gameNumber", snapshot.gameNumber);
        model.addAttribute("bestOf", snapshot.bestOf);
        model.addAttribute("doubles", snapshot.doubles);
        model.addAttribute("matchFinished", snapshot.matchFinished);
        model.addAttribute("gameScores", snapshot.gameScores);
        model.addAttribute("elapsedSec", snapshot.elapsedSec);

        // Tính toán kết quả
        String winner = "";
        if (snapshot.matchFinished) {
            if (snapshot.games[0] > snapshot.games[1]) {
                winner = snapshot.names[0] + " (" + snapshot.clubs[0] + ")";
            } else {
                winner = snapshot.names[1] + " (" + snapshot.clubs[1] + ")";
            }
        }
        model.addAttribute("winner", winner);

        return "scoreboard/result";
    }
}
