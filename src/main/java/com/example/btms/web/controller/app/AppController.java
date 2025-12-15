package com.example.btms.web.controller.app;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller xử lý các trang liên quan đến BTMS Desktop Application
 * Bao gồm: App homepage, Download, Features, Learn More
 * 
 * @author BTMS Team
 * @version 1.0
 * @since 2025-11-25
 */
@Controller
@RequestMapping("/app")
public class AppController {

        // ==================== APP HOME PAGE ====================

        /**
         * Hiển thị trang chủ App Hub
         * Giới thiệu tổng quan về BTMS Desktop Application
         * 
         * @param model Spring Model
         * @return Template path: app/btms-app
         */
        @GetMapping({ "", "/", "/home" })
        public String showAppHome(Model model) {
                // App version info
                model.addAttribute("appVersion", "1.0.0");
                model.addAttribute("releaseDate", "Tháng 11, 2025");
                model.addAttribute("lastUpdate", "25/11/2025");

                // Statistics (mock data - có thể lấy từ DB sau)
                model.addAttribute("downloadCount", 5247);
                model.addAttribute("activeUsers", 1823);
                model.addAttribute("tournamentsManaged", 342);
                model.addAttribute("averageRating", 4.7);

                // System requirements
                Map<String, String> systemReqs = new HashMap<>();
                systemReqs.put("os", "Windows 10/11 (64-bit)");
                systemReqs.put("processor", "Intel Core i3 hoặc tương đương");
                systemReqs.put("ram", "4 GB (khuyến nghị 8 GB)");
                systemReqs.put("storage", "500 MB");
                systemReqs.put("screen", "1280x720 trở lên");
                systemReqs.put("java", "Java 21 Runtime (tự động cài đặt)");
                model.addAttribute("systemRequirements", systemReqs);

                // Key features for homepage
                List<Map<String, String>> keyFeatures = new ArrayList<>();
                keyFeatures.add(createFeature(
                                "fa-layer-group",
                                "Quản lý Đa Sân",
                                "Điều khiển đồng thời 5 sân thi đấu với mã PIN độc lập"));
                keyFeatures.add(createFeature(
                                "fa-mobile-alt",
                                "Điều khiển Từ xa",
                                "Web interface responsive, truy cập qua mobile/tablet"));
                keyFeatures.add(createFeature(
                                "fa-sync-alt",
                                "Real-time Updates",
                                "Đồng bộ điểm số tức thì qua Server-Sent Events"));
                keyFeatures.add(createFeature(
                                "fa-database",
                                "Tích hợp Database",
                                "SQL Server với JPA/Hibernate, backup tự động"));
                keyFeatures.add(createFeature(
                                "fa-sitemap",
                                "Quản lý Bracket",
                                "Tự động tạo sơ đồ thi đấu Single/Double Elimination"));
                keyFeatures.add(createFeature(
                                "fa-qrcode",
                                "QR Code Access",
                                "Truy cập nhanh scoreboard qua QR code"));
                model.addAttribute("keyFeatures", keyFeatures);

                // Latest release notes (preview)
                List<String> releaseHighlights = new ArrayList<>();
                releaseHighlights.add("Giao diện FlatLaf hiện đại, theme Dark/Light");
                releaseHighlights.add("H2 TCP Server cho remote database access");
                releaseHighlights.add("IPv4 filtering và network interface selector");
                releaseHighlights.add("Screenshot capture và export kết quả");
                releaseHighlights.add("Tối ưu performance với connection pooling");
                model.addAttribute("releaseHighlights", releaseHighlights);

                // SEO metadata
                model.addAttribute("pageTitle", "BTMS Desktop App - Ứng dụng Quản lý Giải đấu Cầu lông Chuyên nghiệp");
                model.addAttribute("pageDescription",
                                "Tải xuống BTMS Desktop Application miễn phí. Quản lý giải đấu cầu lông với real-time scoreboard, điều khiển từ xa, và tích hợp database.");
                model.addAttribute("pageKeywords",
                                "btms, badminton, cầu lông, quản lý giải đấu, scoreboard, desktop app");

                return "app/btms-app";
        }

        // ==================== DOWNLOAD PAGE ====================

        /**
         * Hiển thị trang tải xuống ứng dụng
         * Tự động detect OS và hiển thị download phù hợp
         * 
         * @param model   Spring Model
         * @param request HTTP Request (để detect User-Agent)
         * @return Template path: app/download-app/download
         */
        @GetMapping("/download")
        public String showDownloadPage(Model model, HttpServletRequest request) {
                // Detect OS from User-Agent
                String userAgent = request.getHeader("User-Agent");
                String detectedOS = detectOperatingSystem(userAgent);
                model.addAttribute("detectedOS", detectedOS);
                model.addAttribute("recommendedDownload", getDownloadLink(detectedOS));

                // Download options
                List<Map<String, String>> downloadOptions = new ArrayList<>();

                downloadOptions.add(createDownloadOption(
                                "Windows 10/11 (64-bit)",
                                "windows",
                                "Badminton Tournament Management System (BTMS)-1.0.0.msi",
                                "144 MB",
                                "/downloads/Badminton Tournament Management System (BTMS)-1.0.0.msi",
                                "fa-windows",
                                "Installer tự động, bao gồm Java Runtime"));

                downloadOptions.add(createDownloadOption(
                                "Windows (Portable)",
                                "windows-portable",
                                "BTMS-Portable-1.0.0.zip",
                                "152 MB",
                                "/downloads/btms-portable.zip",
                                "fa-file-archive",
                                "Không cần cài đặt, giải nén và chạy"));

                downloadOptions.add(createDownloadOption(
                                "JAR File (All Platforms)",
                                "jar",
                                "btms-1.0.0.jar",
                                "85.3 MB",
                                "/downloads/btms.jar",
                                "fa-java",
                                "Yêu cầu Java 21+ đã cài đặt trước"));

                model.addAttribute("downloadOptions", downloadOptions);

                // Checksums for verification
                Map<String, String> checksums = new HashMap<>();
                checksums.put("windows-msi", "SHA256: a1b2c3d4e5f6...");
                checksums.put("portable-zip", "SHA256: f6e5d4c3b2a1...");
                checksums.put("jar-file", "SHA256: 1a2b3c4d5e6f...");
                model.addAttribute("checksums", checksums);

                // Installation steps
                List<Map<String, Object>> installSteps = new ArrayList<>();
                installSteps.add(Map.of("step", 1, "title", "Tải xuống file cài đặt", "description",
                                "Chọn phiên bản phù hợp với hệ điều hành"));
                installSteps.add(Map.of("step", 2, "title", "Chạy installer", "description",
                                "Double-click file .msi và làm theo hướng dẫn"));
                installSteps.add(
                                Map.of("step", 3, "title", "Chọn thư mục cài đặt", "description",
                                                "Mặc định: C:\\Program Files\\BTMS"));
                installSteps.add(Map.of("step", 4, "title", "Hoàn tất cài đặt", "description",
                                "Khởi động BTMS từ Start Menu hoặc Desktop"));
                model.addAttribute("installSteps", installSteps);

                // Version history (recent)
                List<Map<String, String>> versionHistory = new ArrayList<>();
                versionHistory.add(Map.of("version", "1.0.0", "date", "25/11/2025", "status", "Latest"));
                versionHistory.add(Map.of("version", "0.9.5-beta", "date", "15/11/2025", "status", "Beta"));
                versionHistory.add(Map.of("version", "0.9.0-beta", "date", "01/11/2025", "status", "Beta"));
                model.addAttribute("versionHistory", versionHistory);

                // SEO
                model.addAttribute("pageTitle", "Tải xuống BTMS - Ứng dụng Quản lý Giải đấu Cầu lông");
                model.addAttribute("pageDescription",
                                "Download BTMS Desktop Application miễn phí cho Windows. Hỗ trợ đầy đủ tính năng quản lý giải đấu, real-time scoreboard.");

                return "app/download-app/download";
        }

        // ==================== FEATURES PAGES ====================

        /**
         * Hiển thị danh sách tính năng của ứng dụng
         * 
         * @param model    Spring Model
         * @param category Optional category filter
         * @return Template path: app/features/features-list
         */
        @GetMapping("/features")
        public String showFeaturesList(Model model,
                        @RequestParam(required = false) String category) {
                // Feature categories
                List<String> categories = List.of("Tất cả", "Quản lý", "Scoreboard", "Database", "Network", "UI/UX");
                model.addAttribute("categories", categories);
                model.addAttribute("selectedCategory", category != null ? category : "Tất cả");

                // All features with details
                List<Map<String, Object>> features = getAllFeatures();

                // Filter by category if specified
                if (category != null && !category.equals("Tất cả")) {
                        features = features.stream()
                                        .filter(f -> category.equals(f.get("category")))
                                        .toList();
                }

                model.addAttribute("features", features);
                model.addAttribute("featuresCount", features.size());

                // SEO
                model.addAttribute("pageTitle", "Tính năng BTMS - Desktop Application");
                model.addAttribute("pageDescription",
                                "Khám phá đầy đủ tính năng của BTMS: Quản lý đa sân, Real-time scoring, Database integration, Remote control và nhiều hơn nữa.");

                return "app/features/features-list";
        }

        /**
         * Hiển thị chi tiết một tính năng
         * 
         * @param slug  Feature slug/identifier
         * @param model Spring Model
         * @return Template path: app/features/feature-detail
         */
        @GetMapping("/features/{slug}")
        public String showFeatureDetail(@PathVariable String slug, Model model) {
                // Find feature by slug
                Map<String, Object> feature = getAllFeatures().stream()
                                .filter(f -> slug.equals(f.get("slug")))
                                .findFirst()
                                .orElse(null);

                if (feature == null) {
                        return "redirect:/app/features";
                }

                model.addAttribute("feature", feature);

                // Related features (same category)
                String category = (String) feature.get("category");
                List<Map<String, Object>> relatedFeatures = getAllFeatures().stream()
                                .filter(f -> category.equals(f.get("category")) && !slug.equals(f.get("slug")))
                                .limit(3)
                                .toList();
                model.addAttribute("relatedFeatures", relatedFeatures);

                // SEO
                model.addAttribute("pageTitle", feature.get("title") + " - BTMS Feature");
                model.addAttribute("pageDescription", feature.get("description"));

                return "app/features/feature-detail";
        }

        // ==================== LEARN MORE PAGES ====================

        /**
         * Hiển thị trang Learn More hub
         * 
         * @param model Spring Model
         * @return Template path: app/learn-more-app/learn-more-home
         */
        @GetMapping("/learn-more")
        public String showLearnMoreHome(Model model) {
                // Learning resources
                List<Map<String, String>> resources = new ArrayList<>();

                resources.add(Map.of(
                                "type", "manual",
                                "icon", "fa-book",
                                "title", "Hướng dẫn Sử dụng",
                                "description", "User manual đầy đủ với screenshots và ví dụ",
                                "link", "/app/learn-more/manual",
                                "duration", "30 phút đọc"));

                resources.add(Map.of(
                                "type", "video",
                                "icon", "fa-video",
                                "title", "Video Tutorials",
                                "description", "Series video hướng dẫn từng tính năng chi tiết",
                                "link", "/app/learn-more/videos",
                                "duration", "15 videos"));

                resources.add(Map.of(
                                "type", "faq",
                                "icon", "fa-question-circle",
                                "title", "Câu hỏi Thường gặp",
                                "description", "Giải đáp các thắc mắc phổ biến về BTMS",
                                "link", "/app/learn-more/faq",
                                "duration", "50+ câu hỏi"));

                resources.add(Map.of(
                                "type", "troubleshooting",
                                "icon", "fa-tools",
                                "title", "Xử lý Sự cố",
                                "description", "Hướng dẫn khắc phục các lỗi thường gặp",
                                "link", "/app/learn-more/troubleshooting",
                                "duration", "10 phút đọc"));

                resources.add(Map.of(
                                "type", "api",
                                "icon", "fa-code",
                                "title", "API Documentation",
                                "description", "Tài liệu API cho developers muốn tích hợp",
                                "link", "/app/learn-more/api",
                                "duration", "Developer"));

                resources.add(Map.of(
                                "type", "community",
                                "icon", "fa-users",
                                "title", "Cộng đồng & Hỗ trợ",
                                "description", "Diễn đàn, Discord, và kênh hỗ trợ trực tuyến",
                                "link", "/app/learn-more/community",
                                "duration", "24/7"));

                model.addAttribute("resources", resources);

                // Quick start guide (preview)
                List<Map<String, String>> quickStart = new ArrayList<>();
                quickStart.add(Map.of("step", "1", "title", "Tải và cài đặt BTMS"));
                quickStart.add(Map.of("step", "2", "title", "Khởi động ứng dụng lần đầu"));
                quickStart.add(Map.of("step", "3", "title", "Tạo giải đấu mới"));
                quickStart.add(Map.of("step", "4", "title", "Thêm vận động viên và đội"));
                quickStart.add(Map.of("step", "5", "title", "Thiết lập sân thi đấu"));
                quickStart.add(Map.of("step", "6", "title", "Bắt đầu thi đấu và ghi điểm"));
                model.addAttribute("quickStart", quickStart);

                // SEO
                model.addAttribute("pageTitle", "Tìm hiểu thêm về BTMS - Hướng dẫn & Tài liệu");
                model.addAttribute("pageDescription",
                                "Tài liệu đầy đủ về BTMS: User manual, video tutorials, FAQ, troubleshooting, và API documentation.");

                return "app/learn-more-app/learn-more-home";
        }

        /**
         * Hiển thị User Manual
         * 
         * @param model   Spring Model
         * @param section Optional section to jump to
         * @return Template path: app/learn-more-app/user-manual
         */
        @GetMapping("/learn-more/manual")
        public String showUserManual(Model model,
                        @RequestParam(required = false) String section) {
                // Table of contents
                List<Map<String, Object>> tableOfContents = new ArrayList<>();
                tableOfContents.add(Map.of("id", "intro", "title", "1. Giới thiệu", "subsections",
                                List.of("Tổng quan", "Yêu cầu hệ thống", "Cài đặt")));
                tableOfContents.add(Map.of("id", "getting-started", "title", "2. Bắt đầu", "subsections",
                                List.of("Giao diện chính", "Tạo giải đấu", "Cấu hình")));
                tableOfContents.add(Map.of("id", "tournament", "title", "3. Quản lý Giải đấu", "subsections",
                                List.of("Tạo giải", "Thêm nội dung", "Đăng ký", "Bốc thăm")));
                tableOfContents.add(Map.of("id", "scoreboard", "title", "4. Scoreboard", "subsections",
                                List.of("Thiết lập sân", "Điều khiển", "Remote control", "QR code")));
                tableOfContents.add(Map.of("id", "database", "title", "5. Database", "subsections",
                                List.of("Kết nối SQL Server", "Backup", "Import/Export")));
                tableOfContents.add(Map.of("id", "advanced", "title", "6. Nâng cao", "subsections",
                                List.of("Network settings", "Performance", "Troubleshooting")));

                model.addAttribute("tableOfContents", tableOfContents);
                model.addAttribute("currentSection", section != null ? section : "intro");

                // SEO
                model.addAttribute("pageTitle", "User Manual - BTMS Desktop Application");
                model.addAttribute("pageDescription",
                                "Hướng dẫn sử dụng đầy đủ BTMS từ A-Z với screenshots và ví dụ cụ thể.");

                return "app/learn-more-app/user-manual";
        }

        // ==================== HELPER METHODS ====================

        /**
         * Helper: Tạo feature map
         */
        private Map<String, String> createFeature(String icon, String title, String description) {
                Map<String, String> feature = new HashMap<>();
                feature.put("icon", icon);
                feature.put("title", title);
                feature.put("description", description);
                return feature;
        }

        /**
         * Helper: Tạo download option map
         */
        private Map<String, String> createDownloadOption(String name, String platform, String filename,
                        String size, String link, String icon, String notes) {
                Map<String, String> option = new HashMap<>();
                option.put("name", name);
                option.put("platform", platform);
                option.put("filename", filename);
                option.put("size", size);
                option.put("link", link);
                option.put("icon", icon);
                option.put("notes", notes);
                return option;
        }

        /**
         * Detect OS from User-Agent string
         */
        private String detectOperatingSystem(String userAgent) {
                if (userAgent == null) {
                        return "unknown";
                }

                String ua = userAgent.toLowerCase();

                if (ua.contains("windows")) {
                        return "windows";
                } else if (ua.contains("mac") || ua.contains("darwin")) {
                        return "macos";
                } else if (ua.contains("linux")) {
                        return "linux";
                } else if (ua.contains("android")) {
                        return "android";
                } else if (ua.contains("iphone") || ua.contains("ipad")) {
                        return "ios";
                }

                return "unknown";
        }

        /**
         * Get recommended download link based on OS
         */
        private String getDownloadLink(String os) {
                return switch (os) {
                        case "windows" -> "/downloads/btms-windows-x64.exe";
                        case "macos" -> "/downloads/btms-macos.dmg";
                        case "linux" -> "/downloads/btms-linux.tar.gz";
                        default -> "/downloads/btms.jar";
                };
        }

        /**
         * Get all features with full details
         */
        private List<Map<String, Object>> getAllFeatures() {
                List<Map<String, Object>> features = new ArrayList<>();

                // QUẢN LÝ
                features.add(createFeatureDetail(
                                "multi-court",
                                "Quản lý Đa Sân",
                                "Điều khiển đồng thời 5 sân thi đấu với mã PIN độc lập",
                                "Quản lý",
                                "fa-layer-group",
                                "Hệ thống cho phép quản lý đồng thời nhiều sân thi đấu, mỗi sân có mã PIN 4 chữ số riêng biệt. Hỗ trợ cả đơn và đôi, tự động chuyển đổi layout dọc/ngang.",
                                List.of("5 sân đồng thời", "Mã PIN độc lập", "Singles/Doubles",
                                                "Vertical/Horizontal layout"),
                                "intermediate"));

                features.add(createFeatureDetail(
                                "tournament-management",
                                "Quản lý Giải đấu",
                                "Tạo và quản lý giải đấu với đầy đủ thông tin",
                                "Quản lý",
                                "fa-trophy",
                                "Module quản lý giải đấu toàn diện: tạo giải, thêm nội dung thi đấu, quản lý đăng ký, bốc thăm tự động, và theo dõi kết quả.",
                                List.of("Tạo giải nhanh", "Quản lý nội dung", "Đăng ký online", "Bốc thăm tự động"),
                                "beginner"));

                // SCOREBOARD
                features.add(createFeatureDetail(
                                "realtime-scoring",
                                "Real-time Scoring",
                                "Ghi điểm và cập nhật tức thì qua Server-Sent Events",
                                "Scoreboard",
                                "fa-sync-alt",
                                "Điểm số được cập nhật real-time qua SSE (Server-Sent Events), đảm bảo tất cả devices nhìn thấy điểm giống nhau tức thì. Fallback sang polling nếu cần.",
                                List.of("SSE protocol", "Auto-reconnect", "Fallback polling", "Zero latency"),
                                "advanced"));

                features.add(createFeatureDetail(
                                "remote-control",
                                "Điều khiển Từ xa",
                                "Web interface responsive, truy cập qua mobile/tablet",
                                "Scoreboard",
                                "fa-mobile-alt",
                                "Điều khiển scoreboard từ bất kỳ thiết bị nào qua browser. Responsive design tối ưu cho mobile/tablet. Truy cập qua mã PIN hoặc QR code.",
                                List.of("Mobile-first", "QR code access", "PIN security", "Offline support"),
                                "beginner"));

                // DATABASE
                features.add(createFeatureDetail(
                                "database-integration",
                                "Tích hợp SQL Server",
                                "Database enterprise với JPA/Hibernate ORM",
                                "Database",
                                "fa-database",
                                "Tích hợp SQL Server với Spring Data JPA. Connection pooling (HikariCP), transaction management, và automatic schema migration.",
                                List.of("SQL Server", "JPA/Hibernate", "Connection pooling", "Auto migration"),
                                "advanced"));

                features.add(createFeatureDetail(
                                "h2-tcp-server",
                                "H2 TCP Server",
                                "Remote database access trên port 9092",
                                "Database",
                                "fa-server",
                                "H2 embedded database với TCP server mode. Cho phép máy khác connect database từ xa, thuận tiện cho development và debugging.",
                                List.of("Port 9092", "Remote access", "Multi-client", "Read/Write support"),
                                "advanced"));

                // NETWORK
                features.add(createFeatureDetail(
                                "network-filtering",
                                "IPv4 Network Filtering",
                                "Chỉ chấp nhận IPv4, loại bỏ IPv6 tự động",
                                "Network",
                                "fa-network-wired",
                                "Tự động filter và chỉ hiển thị IPv4 addresses. Giải quyết vấn đề IPv6 gây confuse khi setup network.",
                                List.of("IPv4 only", "Auto-detect", "Interface selector", "Conflict resolution"),
                                "intermediate"));

                features.add(createFeatureDetail(
                                "udp-broadcast",
                                "UDP Multicast Broadcasting",
                                "Broadcasting data qua UDP multicast (239.255.50.50:50505)",
                                "Network",
                                "fa-broadcast-tower",
                                "UDP multicast cho monitoring và synchronization. Tất cả clients trong cùng subnet nhận updates đồng thời.",
                                List.of("Multicast address", "Auto-discovery", "Low latency", "LAN only"),
                                "advanced"));

                // UI/UX
                features.add(createFeatureDetail(
                                "flatlaf-theme",
                                "FlatLaf Modern UI",
                                "Giao diện hiện đại với Dark/Light themes",
                                "UI/UX",
                                "fa-palette",
                                "FlatLaf look-and-feel với Dark và Light themes. Giao diện đẹp, modern, và professional cho desktop application.",
                                List.of("Dark theme", "Light theme", "Modern design", "Custom colors"),
                                "beginner"));

                features.add(createFeatureDetail(
                                "screenshot-capture",
                                "Screenshot Capture",
                                "Chụp màn hình scoreboard và lưu local",
                                "UI/UX",
                                "fa-camera",
                                "Capture screenshot scoreboard và tự động lưu vào folder local. Tên file theo format timestamp, không upload qua network.",
                                List.of("Auto-save", "PNG format", "Timestamp naming", "Local storage"),
                                "beginner"));

                return features;
        }

        /**
         * Helper: Create feature detail map
         */
        private Map<String, Object> createFeatureDetail(String slug, String title, String shortDesc,
                        String category, String icon, String fullDesc,
                        List<String> highlights, String level) {
                Map<String, Object> feature = new HashMap<>();
                feature.put("slug", slug);
                feature.put("title", title);
                feature.put("description", shortDesc);
                feature.put("category", category);
                feature.put("icon", icon);
                feature.put("fullDescription", fullDesc);
                feature.put("highlights", highlights);
                feature.put("level", level); // beginner, intermediate, advanced
                return feature;
        }
}
