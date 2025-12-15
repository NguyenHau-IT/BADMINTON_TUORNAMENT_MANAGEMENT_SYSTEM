# 🏸 BTMS v1.1.0 - Tài Liệu Phiên Bản

## 📋 **Tổng Quan**

**Badminton Tournament Management System (BTMS) v1.1.0** là bản cập nhật quan trọng bổ sung hệ thống quản lý phân công trọng tài hoàn chỉnh với giao diện người dùng được cải thiện và các tính năng mới.

### 📅 **Thông Tin Phiên Bản**
- **Version:** 1.1.0
- **Release Date:** December 2024
- **Previous Version:** 1.0.0
- **Platform:** Java 21, Spring Boot 4.0.0
- **Architecture:** Desktop + Web Hybrid

---

## 🆕 **Tính Năng Mới Trong v1.1.0**

### 1. 🏆 **Hệ Thống Quản Lý Phân Công Trọng Tài**

#### **📋 Panel Lịch Sử Phân Công Trọng Tài**
- **Hiển thị toàn bộ lịch sử phân công** với bảng dữ liệu chi tiết
- **Tìm kiếm đa dạng:** Theo mã trọng tài, tên trọng tài, mã trận đấu
- **Lọc nâng cao:** Theo vai trò trọng tài với dropdown tiếng Việt
- **Thống kê tự động:** Số lượng phân công, phân bố vai trò, hiệu suất trọng tài

**Columns được hiển thị:**
```
┌─────────────────┬─────────────┬─────────────────┬──────────────┬─────────────────┬──────────┐
│ Mã phân công    │ Mã trọng tài│ Tên trọng tài   │ Mã trận đấu   │ Vai trò         │ Ghi chú  │
├─────────────────┼─────────────┼─────────────────┼──────────────┼─────────────────┼──────────┤
│ PC-001-2024     │ TT001       │ Nguyễn Văn An  │ TD-001-2024  │ Trọng tài chính │ Chính    │
│ PC-002-2024     │ TT002       │ Trần Thị Bình  │ TD-002-2024  │ Trọng tài biên  │ Phụ      │
└─────────────────┴─────────────┴─────────────────┴──────────────┴─────────────────┴──────────┘
```

#### **🎯 Dialog Chi Tiết Phân Công**
- **Form tạo mới phân công** với validation đầy đủ
- **Chọn trọng tài** từ dropdown với thông tin đầy đủ
- **Nhập mã trận đấu** với format UUID v7
- **Chọn vai trò** bằng tiếng Việt: Trọng tài chính, Trọng tài biên, Trọng tài giao cầu, Trọng tài tổng
- **Ghi chú tùy chọn** cho thông tin bổ sung

### 2. 🎨 **Cải Thiện Giao Diện Người Dùng**

#### **📂 Navigation Tree Mới**
```
🏸 [Tên Giải Đấu]
├── 📊 Tổng quan
├── 📋 Nội dung của giải
├── 👥 Đăng ký thi đấu
├── 🎲 Bốc thăm
├── 👨‍⚖️ Quản lý trọng tài          ← MỚI
│   ├── 👨‍⚖️ Trọng tài
│   └── 📋 Lịch sử phân công TT     ← MỚI
├── 👁️ Giám sát
└── 🏆 Kết quả
```

#### **🌐 Vai Trò Tiếng Việt**
- **UI hoàn toàn tiếng Việt** cho trải nghiệm người dùng tốt hơn
- **Mapping tự động** giữa tiếng Việt (UI) và tiếng Anh (Database)

| Hiển thị (Tiếng Việt) | Lưu trữ (Database) |
|----------------------|-------------------|
| Trọng tài chính      | CHIEF             |
| Trọng tài biên       | LINE              |
| Trọng tài giao cầu   | SERVICE           |
| Trọng tài tổng       | UMPIRE            |

### 3. ⚡ **Tích Hợp Tự Động**

#### **🔗 Web Scoring Integration**
- **Tự động tạo phân công** khi trận đấu hoàn thành
- **Gán trọng tài chính** cho mỗi trận đấu qua web scoring
- **Đồng bộ dữ liệu** giữa web scoring và desktop management

#### **🗂️ Quản Lý Phiên Làm Việc**
- **Service-based architecture** với Spring Boot integration
- **Database connection management** tự động
- **Error handling** và logging cải tiến

---

## 📖 **Hướng Dẫn Sử Dụng**

### 🚀 **Truy Cập Tính Năng Mới**

1. **Đăng nhập** với quyền ADMIN
2. **Mở rộng menu** "Quản lý trọng tài" trong Navigation Tree
3. **Nhấp "Lịch sử phân công TT"** để mở panel

### 📝 **Tạo Phân Công Mới**

1. **Nhấp nút "➕ Thêm"** trong panel lịch sử
2. **Chọn trọng tài** từ dropdown (hiển thị mã + tên)
3. **Nhập mã trận đấu** (format: UUID v7)
4. **Chọn vai trò** từ dropdown tiếng Việt
5. **Thêm ghi chú** (tùy chọn)
6. **Nhấp "💾 Lưu"** để hoàn tất

### 🔍 **Tìm Kiếm và Lọc**

#### **Tìm kiếm văn bản:**
- **Nhập từ khóa** vào ô tìm kiếm
- **Chọn loại tìm kiếm:** Tất cả, Mã trọng tài, Tên trọng tài, Mã trận đấu

#### **Lọc theo vai trò:**
- **Chọn vai trò** từ dropdown "Tất cả vai trò"
- **Kết hợp** với tìm kiếm văn bản để lọc chính xác hơn

### 📊 **Xem Thống Kê**

**Nhấp "📈 Thống kê"** để xem:
- 📋 **Tổng số phân công**
- 👨‍⚖️ **Số lượng trọng tài được phân công**
- 📊 **Phân bố theo vai trò** (biểu đồ cột)
- 🏆 **Top trọng tài** theo số lần phân công

---

## 🛠️ **Chi Tiết Kỹ Thuật**

### 📁 **Cấu Trúc File Mới**

```
src/main/java/com/example/btms/ui/referee/
├── PhanCongTrongTaiHistoryPanel.java     ← Panel chính
├── PhanCongTrongTaiDetailDialog.java     ← Dialog chi tiết  
├── RefereeManagementFrame.java           ← Demo frame
└── TrongTaiManagementPanel.java          ← Panel trọng tài

src/main/java/com/example/btms/service/referee/
├── PhanCongTrongTaiService.java          ← Service phân công
└── TrongTaiService.java                  ← Service trọng tài

src/main/java/com/example/btms/model/referee/
├── PhanCongTrongTai.java                 ← Model phân công
└── TrongTai.java                         ← Model trọng tài

src/main/java/com/example/btms/repository/referee/
├── PhanCongTrongTaiRepository.java       ← Repository phân công
└── TrongTaiRepository.java               ← Repository trọng tài
```

### 🗄️ **Database Schema**

#### **Bảng PhanCongTrongTai (Referee Assignments)**
```sql
CREATE TABLE PhanCongTrongTai (
    MaPhanCong NVARCHAR(50) PRIMARY KEY,     -- Assignment ID
    MaTrongTai NVARCHAR(50) NOT NULL,        -- Referee ID (FK)
    MaTranDau NVARCHAR(255) NOT NULL,        -- Match ID (UUID v7)
    VaiTro NVARCHAR(50) NOT NULL,            -- Role (CHIEF, LINE, SERVICE, UMPIRE)
    GhiChu NVARCHAR(500),                    -- Notes
    NgayTao DATETIME DEFAULT GETDATE(),      -- Created date
    NgayCapNhat DATETIME DEFAULT GETDATE(),  -- Updated date
    
    CONSTRAINT FK_PhanCong_TrongTai 
        FOREIGN KEY (MaTrongTai) REFERENCES TrongTai(MaTrongTai)
);
```

### 🔧 **API Endpoints Mới**

#### **REST API cho Web Integration**
```http
GET    /api/referee-assignments          # Lấy tất cả phân công
POST   /api/referee-assignments          # Tạo phân công mới
GET    /api/referee-assignments/{id}     # Lấy chi tiết phân công
PUT    /api/referee-assignments/{id}     # Cập nhật phân công
DELETE /api/referee-assignments/{id}     # Xóa phân công
```

### 🔄 **Service Layer Architecture**

```java
@Service
public class PhanCongTrongTaiService {
    // CRUD operations
    public List<PhanCongTrongTai> getAllAssignments()
    public Optional<PhanCongTrongTai> getAssignmentById(String id)
    public boolean createAssignment(PhanCongTrongTai assignment)
    public boolean updateAssignment(PhanCongTrongTai assignment)
    public boolean deleteAssignment(String id)
    
    // Business logic
    public List<PhanCongTrongTai> getAssignmentsByMatch(String matchId)
    public List<PhanCongTrongTai> getAssignmentsByReferee(String refereeId)
    public Map<String, Long> getAssignmentStatsByRole()
}
```

---

## 🚀 **Cài Đặt và Triển Khai**

### 📦 **Requirements**
- **Java 21+**
- **Spring Boot 4.0.0**
- **SQL Server** hoặc **H2 Database**
- **Maven 3.8+**
- **Windows 10/11** (recommended)

### 🛠️ **Build Instructions**

```bash
# Clone repository
git clone https://github.com/NguyenHau-IT/BADMINTON_TUORNAMENT_MANAGEMENT_SYSTEM.git
cd BADMINTON_TUORNAMENT_MANAGEMENT_SYSTEM

# Switch to dev branch for v1.1.0
git checkout dev

# Build project
mvn clean compile package

# Run application
java -jar target/btms-1.0.0.jar
```

### ⚙️ **Configuration**

#### **Database Configuration**
```properties
# application.properties
spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=BTMS
spring.datasource.username=your_username
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update
```

#### **UI Configuration**
```properties
# UI Settings
ui.fontScalePercent=100
ui.darkTheme=false
ui.alwaysOnTop=false
monitor.columns=2
```

---

## 🐛 **Bug Fixes trong v1.1.0**

1. **✅ Fixed:** Service constructor errors trong RefereeManagementFrame
2. **✅ Fixed:** UIManager.getSystemLookAndFeel() method calls
3. **✅ Fixed:** Database connection handling trong referee services
4. **✅ Fixed:** Null pointer exceptions trong referee assignment dialogs
5. **✅ Fixed:** Navigation tree integration cho admin role

---

## 🔮 **Roadmap v1.2.0**

### 📅 **Planned Features**
- **📧 Email notifications** cho phân công trọng tài
- **📱 Mobile app** cho trọng tài check-in
- **🔄 Real-time sync** giữa multiple clients
- **📊 Advanced analytics** với charts và reports
- **🔒 Role-based permissions** cho referee management
- **📤 Export/Import** referee assignments
- **🌐 Multi-language support** (English, Vietnamese)

### 🎯 **Performance Improvements**
- **⚡ Faster database queries** với indexed searches
- **🖼️ Lazy loading** cho large datasets
- **📱 Responsive UI** cho different screen sizes
- **🔧 Memory optimization** cho better performance

---

## 📞 **Hỗ Trợ và Liên Hệ**

### 🛠️ **Technical Support**
- **GitHub Issues:** [BTMS Issues](https://github.com/NguyenHau-IT/BADMINTON_TUORNAMENT_MANAGEMENT_SYSTEM/issues)
- **Email:** support@btms.com
- **Documentation:** [BTMS Docs](docs/)

### 👥 **Development Team**
- **Project Lead:** NguyenHau-IT
- **Version:** v1.1.0
- **Contributors:** BTMS Development Team

### 📚 **Additional Resources**
- **API Documentation:** [API_DOCUMENTATION.md](API_DOCUMENTATION.md)
- **User Guide:** [HUONG_DAN_SU_DUNG.md](HUONG_DAN_SU_DUNG.md)
- **Getting Started:** [GETTING_STARTED.md](GETTING_STARTED.md)
- **BWF Rules:** [LUAT_THI_DAU_CAU_LONG_BWF.md](LUAT_THI_DAU_CAU_LONG_BWF.md)

---

## 📄 **License**

**BTMS v1.1.0** được phát triển cho mục đích quản lý giải đấu cầu lông chuyên nghiệp.

**Copyright © 2024 BTMS Development Team. All rights reserved.**

---

*🏸 Badminton Tournament Management System v1.1.0 - Nâng cao trải nghiệm quản lý giải đấu với công nghệ hiện đại!*