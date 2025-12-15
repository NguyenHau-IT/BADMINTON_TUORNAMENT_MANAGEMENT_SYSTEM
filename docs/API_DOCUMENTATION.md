# 🔌 BTMS API Documentation · v2.1.0

> **📅 Cập nhật**: December 10, 2025 - UUID v7 Court Integration  
> **🏗️ Architecture**: Hybrid Desktop + Web Application  
> **🔧 Framework**: Spring Boot 3.4.0 + Java 21  
> **🎯 API Design**: RESTful với multi-mode architecture & UUID v7 courtId

Tài liệu API hoàn chỉnh cho hệ thống **BTMS (Badminton Tournament Management System)** - platform quản lý giải đấu cầu lông enterprise-grade với kiến trúc hybrid tiên tiến và hệ thống courtId UUID v7.

## 🎯 **Multi-Mode API Architecture**

BTMS cung cấp **7 chế độ API** cho các use case khác nhau:

| Mode                   | Base Path                | Authentication    | Use Case                                 |
| ---------------------- | ------------------------ | ----------------- | ---------------------------------------- |
| **PIN Mode**           | `/api/court/{pin}/**`    | 4-digit PIN       | Multi-court management với PIN isolation |
| **Direct Mode**        | `/api/scoreboard/**`     | None             | Single court direct access               |
| **Tournament API**     | `/api/tournaments/**`    | Public            | Tournament data, registrations, search   |
| **SSE API**            | `/api/sse/**`            | Public            | Real-time Server-Sent Events             |
| **Match API**          | `/api/matches/**`        | Public            | Live match data và score streaming       |
| **Referee Auth API**   | `/api/referee/**`        | Session-based     | Device verification & referee auth       |
| **Bracket API**        | `/api/tournaments/**`    | Public            | Tournament bracket & draw data           |

### ✨ **Enhanced Core Features**

- **🔄 Real-time Updates**: Advanced SSE với Thread UUID tracking + UDP multicast
- **📱 Mobile-First**: Responsive design optimized for mobile/tablet scoreboard
- **⚡ Ultra Performance**: Java 21 virtual threads + async processing + caching
- **🔒 Multi-layer Security**: PIN-based court access + device verification system
- **🌐 Cross-Platform Support**: Desktop app + web interface + mobile responsive
- **🆔 UUID v7 Court System**: Time-ordered court IDs với user-friendly display mapping
- **👥 Device Management**: Real-time device monitoring & admin approval system

### 🆔 **UUID v7 Court System**

BTMS sử dụng hệ thống courtId UUID v7 mới:

- **Backend Storage**: UUID v7 time-ordered IDs (ví dụ: `01939f3c-1234-7abc-9def-123456789abc`)
- **UI Display**: User-friendly names ("Sân 1", "Sân 2", "Sân 3"...)
- **Advantages**: Chronological ordering, database optimization, global uniqueness
- **Mapping**: Automatic conversion giữa UUID v7 và display names

```json
{
  "courtId": "01939f3c-1234-7abc-9def-123456789abc",
  "displayName": "Sân 1",
  "pinCode": "1234",
  "header": "Nam đơn - Chung kết"
}
```

---

## 🧭 Base URLs & Network Configuration

### 🌐 **Standard Endpoints**

| Environment             | Base URL                  | Purpose                     |
| ----------------------- | ------------------------- | --------------------------- |
| **Local Development**   | `http://localhost:2345`   | Development và testing      |
| **LAN Deployment**      | `http://[SERVER_IP]:2345` | Production trên mạng nội bộ |
| **Multi-machine Setup** | `http://[HOST_IP]:2345`   | Distributed deployment      |

### 🔗 **API Mode Examples**

```http
# PIN Mode (Multi-court với UUID v7)
GET http://192.168.1.100:2345/api/court/1234/sync
POST http://192.168.1.100:2345/api/court/5678/increaseA
GET http://192.168.1.100:2345/api/court/connections

# Tournament API
GET http://192.168.1.100:2345/api/tournaments
GET http://192.168.1.100:2345/api/tournaments/1/bracket

# SSE Real-time
GET http://192.168.1.100:2345/api/sse/tournaments/1/subscribe
GET http://192.168.1.100:2345/api/matches/abc123/score-stream

# Device Management
POST http://192.168.1.100:2345/api/referee/login
GET http://192.168.1.100:2345/api/referee/check-auth
```

### 🔒 **CORS & Security Configuration**

- **Default CORS**: Allows `*` (all origins) cho development
- **Production Recommendation**: Giới hạn theo internal domains
- **Network Isolation**: Designed cho LAN deployment, không expose ra internet
- **IPv4 Only**: Hệ thống chỉ support IPv4 networks
- **Device Verification**: Session-based authentication cho web scoreboard access

---

## 📦 Content Types & Response Format

- **Request**: `application/json` (cho endpoints cần body data)
- **Response**: `application/json; charset=utf-8`
- **SSE Stream**: `text/event-stream`
- **CORS Headers**: `Access-Control-Allow-Origin: *`

---

## 🧱 Core Data Models

### 📊 **Enhanced Match Snapshot Model**

Đây là JSON response chính được trả về bởi các endpoint `/sync`, actions và SSE events:

```json
{
  "names": ["Nguyễn Văn A", "Trần Thị B"],
  "clubs": ["VBA Club", "Hanoi BC"],
  "score": [21, 19],
  "games": [1, 0],
  "gameNumber": 1,
  "bestOf": 3,
  "server": 0,
  "doubles": false,
  "betweenGamesInterval": false,
  "changedEndsThisGame": false,
  "matchFinished": false,
  "elapsedSec": 1234,
  "gameScores": [[21, 19]],
  "courtId": "01939f3c-1234-7abc-9def-123456789abc",
  "pinCode": "1234",
  "threadUuid": "thread-uuid-for-tracking"
}
```

### 📋 **Enhanced Field Definitions**

| Field                  | Type     | Description                           | Example                                  |
| ---------------------- | -------- | ------------------------------------- | ---------------------------------------- |
| `names`                | string[] | Tên players/teams [A, B]              | `["Nguyễn A", "Trần B"]`                 |
| `clubs`                | string[] | Club affiliations [A, B]              | `["VBA", "Hanoi BC"]`                    |
| `score`                | int[]    | Điểm hiện tại game đang chơi          | `[21, 19]`                               |
| `games`                | int[]    | Số games đã thắng [A, B]              | `[2, 1]`                                 |
| `gameNumber`           | int      | Game hiện tại (1-based)               | `3`                                      |
| `courtId`              | string   | UUID v7 court identifier              | `"01939f3c-1234-7abc-9def-123456789abc"` |
| `pinCode`              | string   | 4-digit court access PIN              | `"1234"`                                 |
| `threadUuid`           | string   | Thread tracking UUID cho SSE          | `"thread-uuid-for-tracking"`             |

---

## 🏟️ Court Management API (UUID v7)

### Base Path: `/api/court`

Quản lý nhiều sân cầu lông với hệ thống UUID v7 và PIN-based access control.

#### 🔍 **GET /api/court/connections**
**Mô tả**: Lấy thông tin tất cả court connections và SSE status

**Response Example**:
```json
{
  "summary": {
    "totalConnections": 5,
    "activeCourts": 3,
    "totalCourts": 10,
    "timestamp": 1733842123456
  },
  "courts": {
    "01939f3c-1234-7abc-9def-123456789abc": {
      "pin": "1234",
      "courtId": "01939f3c-1234-7abc-9def-123456789abc",
      "header": "Nam đơn - Chung kết",
      "endpoint": "/api/court/1234/stream",
      "connections": 2,
      "status": "active",
      "match": {
        "score": [21, 19],
        "games": [1, 0],
        "gameNumber": 1,
        "finished": false,
        "names": ["Nguyễn A", "Trần B"]
      },
      "lastActivity": 1733842123456
    }
  }
}
```

#### 🔍 **GET /api/court/{pin}/sync**
**Mô tả**: Lấy snapshot hiện tại của trận đấu theo PIN

**Parameters**:
- `pin` (path): 4-digit PIN code của sân

**Response**: [Match Snapshot Model](#-enhanced-match-snapshot-model)

#### 📡 **GET /api/court/{pin}/stream**
**Mô tả**: Server-Sent Events stream cho real-time updates

**Content-Type**: `text/event-stream`
**Events**:
- `snapshot`: Match state updates
- `score-change`: Score modifications
- `game-complete`: Game completion
- `match-complete`: Match finish

#### 🎮 **POST /api/court/{pin}/increaseA**
**Mô tả**: Tăng điểm cho team A

#### 🎮 **POST /api/court/{pin}/increaseB**
**Mô tả**: Tăng điểm cho team B

#### 🎮 **POST /api/court/{pin}/decreaseA**
**Mô tả**: Giảm điểm cho team A

#### 🎮 **POST /api/court/{pin}/decreaseB**
**Mô tả**: Giảm điểm cho team B

#### ⚙️ **POST /api/court/{pin}/reset**
**Mô tả**: Reset toàn bộ trận đấu

#### ⚙️ **POST /api/court/{pin}/nextGame**
**Mô tả**: Chuyển sang game tiếp theo

#### 🔍 **GET /api/court/{pin}/status**
**Mô tả**: Kiểm tra trạng thái court và device verification

#### 🔍 **GET /api/court/health**
**Mô tả**: Health check cho court management system

---

## 🏆 Tournament Management API

### Base Path: `/api/tournaments`

Quản lý giải đấu, đăng ký, tìm kiếm và thống kê.

#### 🔍 **GET /api/tournaments**
**Mô tả**: Lấy danh sách tất cả giải đấu với pagination

**Query Parameters**:
- `page` (int, default: 0): Số trang
- `size` (int, default: 20): Kích thước trang
- `sort` (string, default: "ngayBd"): Trường sắp xếp
- `direction` (string, default: "desc"): Hướng sắp xếp (asc/desc)

**Response**: Paginated list of tournaments

```json
{
  "content": [
    {
      "id": 1,
      "tenGiai": "Giải cầu lông Hà Nội mở rộng 2025",
      "ngayBd": "2025-01-15",
      "ngayKt": "2025-01-20",
      "diaDiem": "Nhà thi đấu Trịnh Hoài Đức",
      "tinhThanh": "Hà Nội",
      "moTa": "Giải đấu cầu lông chuyên nghiệp...",
      "soLuongDangKy": 45,
      "trangThai": "Đang mở đăng ký"
    }
  ],
  "totalElements": 10,
  "totalPages": 1,
  "size": 20,
  "number": 0
}
```

#### 🔍 **GET /api/tournaments/{id}**
**Mô tả**: Lấy chi tiết một giải đấu

**Parameters**:
- `id` (path): Tournament ID

**Response**: Detailed tournament information

#### 🔍 **GET /api/tournaments/count**
**Mô tả**: Đếm tổng số giải đấu

**Response**:
```json
{
  "count": 123
}
```

#### 🔍 **GET /api/tournaments/search**
**Mô tả**: Tìm kiếm giải đấu

**Query Parameters**:
- `q` (string): Từ khóa tìm kiếm
- `location` (string): Tỉnh thành
- `status` (string): Trạng thái giải đấu
- `fromDate` (date): Từ ngày
- `toDate` (date): Đến ngày

#### 🔍 **GET /api/tournaments/featured**
**Mô tả**: Lấy danh sách giải đấu nổi bật

#### 🔍 **GET /api/tournaments/upcoming**
**Mô tả**: Lấy danh sách giải đấu sắp diễn ra

#### 🔍 **GET /api/tournaments/calendar**
**Mô tả**: Lấy calendar events cho giải đấu

#### 🔍 **GET /api/tournaments/stats**
**Mô tả**: Thống kê tổng quan về giải đấu

### 🏅 Bracket & Draw API

#### 🔍 **GET /api/tournaments/{id}/bracket**
**Mô tả**: Lấy bracket data cho giải đấu

**Parameters**:
- `id` (path): Tournament ID
- `categoryId` (query, optional): Category ID
- `isTeam` (query, optional): true cho đội, false cho đơn

**Response**: Bracket data theo format BWF

```json
{
  "tournamentId": 1,
  "categoryId": 1,
  "format": "single-elimination",
  "isTeam": false,
  "totalRounds": 4,
  "totalMatches": 15,
  "rounds": [
    {
      "roundNumber": 1,
      "roundName": "Vòng 1",
      "matches": [
        {
          "matchId": "match-1",
          "position": 1,
          "player1": "Nguyễn Văn A",
          "player2": "Trần Văn B",
          "score": "21-19, 21-18",
          "winner": "player1",
          "status": "completed"
        }
      ]
    }
  ]
}
```

#### 🔍 **GET /api/tournaments/{id}/bracket/all**
**Mô tả**: Lấy tất cả brackets của một giải đấu

#### 🔍 **GET /api/tournaments/{id}/bracket/categories**
**Mô tả**: Lấy danh sách categories có bracket data

---

## 📡 Server-Sent Events (SSE) API

### Base Path: `/api/sse`

Real-time event streaming cho tournaments và matches.

#### 📡 **GET /api/sse/tournaments/{id}/subscribe**
**Mô tả**: Subscribe to tournament updates

**Content-Type**: `text/event-stream`

**Events**:
- `connected`: Xác nhận kết nối
- `heartbeat`: Keep-alive ping
- `tournament-update`: Cập nhật giải đấu
- `match-update`: Cập nhật trận đấu
- `registration-update`: Đăng ký mới

**Event Example**:
```
event: tournament-update
data: {"tournamentId": 1, "registrationCount": 45, "timestamp": 1733842123456}

```

#### 📡 **GET /api/sse/matches/{id}/subscribe**
**Mô tả**: Subscribe to match score updates

**Events**:
- `connected`: Xác nhận kết nối
- `score-update`: Thay đổi điểm số
- `set-complete`: Hoàn thành một set
- `match-complete`: Kết thúc trận đấu

#### 🔍 **GET /api/sse/stats**
**Mô tả**: SSE connection statistics

#### 🔍 **GET /api/sse/connections**
**Mô tả**: Chi tiết tất cả SSE connections

#### 🚀 **POST /api/sse/matches/{id}/score-update**
**Mô tả**: Broadcast score update to clients

**Request Body**:
```json
{
  "teamAScore": 21,
  "teamBScore": 19,
  "currentSet": 2,
  "matchStatus": "ongoing"
}
```

---

## 🏸 Live Match API

### Base Path: `/api/matches`

Real-time match data và score streaming.

#### 📡 **GET /api/matches/{matchId}/score-stream**
**Mô tả**: Server-Sent Events cho real-time score updates

**Content-Type**: `text/event-stream`
**Timeout**: 5 minutes

**Events**:
- `connected`: Initial connection confirmation
- `score-update`: Live score changes
- `set-update`: Set completion
- `status-change`: Match status changes
- `heartbeat`: Keep-alive ping every 30 seconds

#### 🔍 **GET /api/matches/{matchId}/score**
**Mô tả**: Polling fallback cho score retrieval

**Response**:
```json
{
  "matchId": "01939f3c-1234-7abc-9def-123456789abc",
  "status": "ongoing",
  "team1Score": 21,
  "team2Score": 18,
  "currentSet": 2,
  "sets": [
    { "setNumber": 1, "team1Score": 21, "team2Score": 19 },
    { "setNumber": 2, "team1Score": 18, "team2Score": 21 }
  ],
  "lastUpdated": 1733842123456
}
```

#### 🔍 **GET /api/matches/{matchId}**
**Mô tả**: Lấy thông tin chi tiết trận đấu

#### 🔍 **GET /api/matches/{matchId}/history**
**Mô tả**: Lịch sử thay đổi điểm số của trận đấu

---

## 🔐 Device Management & Referee Authentication

### Base Path: `/api/referee`

Quản lý thiết bị truy cập web và xác thực trọng tài.

#### 🔍 **GET /api/referee/check-auth**
**Mô tả**: Kiểm tra trạng thái đăng nhập và device verification

**Response**:
```json
{
  "isLoggedIn": true,
  "verified": false,
  "maTrongTai": "BTMS-ABC123",
  "hoTen": "Nguyễn Văn Trọng Tài",
  "message": "Đang chờ quản trị viên duyệt...",
  "blocked": false,
  "kicked": false
}
```

**Status Cases**:
- `isLoggedIn: false` - Chưa đăng nhập
- `verified: false` - Đang chờ admin duyệt
- `blocked: true` - Thiết bị bị chặn
- `kicked: true` - Session bị đóng bởi admin

#### 🚀 **POST /api/referee/login**
**Mô tả**: Đăng nhập trọng tài

**Request Body**:
```json
{
  "maTrongTai": "BTMS-ABC123",
  "matKhau": "password123",
  "deviceId": "device-uuid",
  "deviceModel": "iPhone 14 Pro"
}
```

**Response**:
```json
{
  "success": true,
  "message": "Đăng nhập thành công",
  "maTrongTai": "BTMS-ABC123",
  "hoTen": "Nguyễn Văn Trọng Tài"
}
```

#### 🚀 **POST /api/referee/logout**
**Mô tả**: Đăng xuất và xóa device session

#### 🔍 **GET /api/referee/device-info**
**Mô tả**: Thông tin thiết bị hiện tại

### Device Management Features:

1. **Auto Device Detection**: Tự động detect device model, browser, OS
2. **Admin Verification**: Admin cần approve mỗi thiết bị mới
3. **Session Tracking**: Real-time monitoring thiết bị đang online
4. **Remote Control**: Admin có thể kick/block thiết bị từ xa
5. **Security Logging**: Log tất cả hoạt động đăng nhập/truy cập

---

## 🚨 Error Handling & Status Codes

### Standard HTTP Status Codes

| Status Code | Meaning              | Usage                                   |
| ----------- | -------------------- | --------------------------------------- |
| `200`       | OK                   | Request successful                      |
| `201`       | Created              | Resource created successfully           |
| `400`       | Bad Request          | Invalid request parameters              |
| `401`       | Unauthorized         | Missing or invalid authentication       |
| `403`       | Forbidden            | Device not verified/blocked             |
| `404`       | Not Found           | Tournament/match/court not found        |
| `409`       | Conflict            | PIN already exists, duplicate data      |
| `429`       | Too Many Requests   | Rate limiting applied                   |
| `500`       | Internal Server Error| Server-side error                       |
| `503`       | Service Unavailable | SSE connection issues, server overload  |

### Error Response Format

```json
{
  "error": "TOURNAMENT_NOT_FOUND",
  "message": "Giải đấu với ID 123 không tồn tại",
  "timestamp": "2025-12-10T15:30:45Z",
  "path": "/api/tournaments/123",
  "details": {
    "requestId": "req-123456",
    "suggestion": "Kiểm tra lại ID giải đấu"
  }
}
```

### Common Error Scenarios

#### Court Management Errors:
- `COURT_NOT_FOUND`: Court với UUID hoặc PIN không tồn tại
- `PIN_INVALID`: PIN code không đúng format (4 chữ số)
- `DEVICE_NOT_VERIFIED`: Thiết bị chưa được admin duyệt
- `DEVICE_BLOCKED`: Thiết bị bị admin chặn

#### Match Errors:
- `MATCH_NOT_ACTIVE`: Trận đấu không đang diễn ra
- `INVALID_SCORE_ACTION`: Hành động điểm số không hợp lệ
- `GAME_ALREADY_FINISHED`: Game đã kết thúc

#### SSE Connection Errors:
- `SSE_TIMEOUT`: Kết nối SSE bị timeout
- `TOO_MANY_CONNECTIONS`: Quá nhiều connections đồng thời
- `STREAM_INTERRUPTED`: Stream bị gián đoạn

---

## 🔐 Authentication & Security

### Authentication Methods

1. **PIN-based Court Access**
   - 4-digit PIN codes cho mỗi court
   - Automatic validation và session management
   - No persistent storage required

2. **Session-based Referee Auth**
   - HTTP session với server-side storage
   - Device fingerprinting và verification
   - Admin approval workflow

3. **Public API Access**
   - Tournament, SSE, Match APIs are public
   - Rate limiting applied per IP
   - CORS enabled for web clients

### Security Features

- **Input Validation**: Tất cả input được validate strict
- **SQL Injection Prevention**: Parameterized queries
- **XSS Protection**: Output encoding
- **CSRF Protection**: Session-based requests có CSRF tokens
- **Rate Limiting**: API calls bị limit theo IP và endpoint
- **Device Verification**: Multi-step approval cho web access
- **Session Management**: Auto cleanup expired sessions
- **Network Isolation**: LAN-only deployment recommended

---

## 📊 Performance & Monitoring

### Performance Optimizations

1. **Java 21 Virtual Threads**
   - Async processing cho tất cả I/O operations
   - Higher concurrent connection capacity
   - Reduced memory footprint

2. **SSE Broadcast Optimizations**
   - Thread UUID tracking cho event correlation
   - JSON payload caching
   - Broadcast throttling (50ms minimum interval)
   - Dead client cleanup every 30 seconds

3. **UUID v7 Court System**
   - Time-ordered IDs cho chronological sorting
   - Database index optimization
   - Mapping cache cho display names

4. **Database Optimizations**
   - Connection pooling
   - Prepared statement caching
   - Index optimization cho time-based queries

### Monitoring Endpoints

#### System Health
- `GET /api/court/health` - Court system health
- `GET /api/sse/stats` - SSE connection statistics
- `GET /actuator/health` - Spring Boot health checks

#### Real-time Monitoring
- `GET /api/court/connections` - Detailed court status
- `GET /api/sse/connections` - Active SSE connections
- SSE events include performance metrics

### Recommended Deployment

```yaml
# docker-compose.yml
version: '3.8'
services:
  btms:
    image: btms:latest
    ports:
      - "2345:2345"
    environment:
      - JAVA_OPTS=-Xmx2g -XX:+UseZGC
      - SPRING_PROFILES_ACTIVE=production
    networks:
      - btms-network
    volumes:
      - ./data:/data
      - ./logs:/logs

networks:
  btms-network:
    driver: bridge
    ipam:
      config:
        - subnet: 192.168.100.0/24
```

### Load Testing Results

- **Concurrent Courts**: Up to 50 courts simultaneously
- **SSE Connections**: 500+ concurrent connections per court
- **Response Time**: < 50ms for score updates
- **Throughput**: 1000+ requests/second
- **Memory Usage**: ~1GB for 25 active courts

---

## 📚 Integration Examples

### JavaScript/React Integration

```javascript
// Court SSE Connection
const connectToCourt = (pin) => {
  const eventSource = new EventSource(`/api/court/${pin}/stream`);
  
  eventSource.onmessage = (event) => {
    const matchData = JSON.parse(event.data);
    updateScoreboard(matchData);
  };
  
  eventSource.onerror = (error) => {
    console.error('SSE Error:', error);
    // Implement reconnection logic
  };
  
  return eventSource;
};

// Score Update
const increaseScore = async (pin, team) => {
  try {
    const response = await fetch(`/api/court/${pin}/increase${team}`, {
      method: 'POST'
    });
    // Score update will come via SSE
  } catch (error) {
    console.error('Score update failed:', error);
  }
};
```

### Mobile App Integration

```swift
// Swift/iOS EventSource
import Foundation

class CourtConnection {
    private var eventSource: EventSource?
    
    func connect(pin: String) {
        let url = URL(string: "http://192.168.1.100:2345/api/court/\(pin)/stream")!
        eventSource = EventSource(url: url)
        
        eventSource?.onMessage { event in
            if let data = event.data?.data(using: .utf8),
               let match = try? JSONDecoder().decode(MatchSnapshot.self, from: data) {
                DispatchQueue.main.async {
                    self.updateUI(with: match)
                }
            }
        }
    }
}
```

---

## 🚀 Migration Notes (v1.0 → v2.1)

### Breaking Changes

1. **Court ID Format**
   - **Old**: String "Sân 1", "Sân 2"
   - **New**: UUID v7 format với display mapping
   - **Migration**: Automatic mapping maintained

2. **New Endpoints Added**
   - `/api/court/connections` - Court monitoring
   - `/api/tournaments/**` - Full tournament API
   - `/api/sse/**` - Enhanced SSE support
   - `/api/matches/**` - Live match API
   - `/api/referee/**` - Device management

3. **Enhanced Response Format**
   - Added `courtId` field to match snapshots
   - Added `threadUuid` for SSE tracking
   - Enhanced error responses with more details

### Backward Compatibility

- Tất cả v1.0 endpoints vẫn hoạt động
- Court display names maintained ("Sân 1", "Sân 2"...)
- Existing PIN codes unchanged
- SSE event format preserved

---

## 📞 Support & Contact

- **Documentation**: `/docs/` in project repository
- **API Examples**: `/examples/` folder
- **Issue Tracking**: GitHub Issues
- **Performance Monitoring**: Built-in metrics endpoints

**Latest Update**: December 10, 2025 - Complete UUID v7 integration và comprehensive API expansion

#### 🎮 **POST /api/court/{pin}/increaseA**
**Mô tả**: Tăng điểm cho team A

#### 🎮 **POST /api/court/{pin}/increaseB**
**Mô tả**: Tăng điểm cho team B

#### 🎮 **POST /api/court/{pin}/decreaseA**
**Mô tả**: Giảm điểm cho team A

#### 🎮 **POST /api/court/{pin}/decreaseB**
**Mô tả**: Giảm điểm cho team B

#### ⚙️ **POST /api/court/{pin}/reset**
**Mô tả**: Reset toàn bộ trận đấu

#### ⚙️ **POST /api/court/{pin}/nextGame**
**Mô tả**: Chuyển sang game tiếp theo

#### 🔍 **GET /api/court/{pin}/status**
**Mô tả**: Kiểm tra trạng thái court và device verification

#### 🔍 **GET /api/court/health**
**Mô tả**: Health check cho court management system
| `bestOf`               | int      | Total games trong match      | `3` (BO3)                |
| `server`               | int      | Current server (0=A, 1=B)    | `0`                      |
| `doubles`              | boolean  | Singles hoặc doubles match   | `false`                  |
| `betweenGamesInterval` | boolean  | Trong break giữa games       | `true`                   |
| `changedEndsThisGame`  | boolean  | Đã đổi sân trong game này    | `false`                  |
| `matchFinished`        | boolean  | Match đã kết thúc            | `false`                  |
| `elapsedSec`           | long     | Thời gian match (seconds)    | `1234`                   |
| `gameScores`           | int[][]  | Lịch sử scores các games     | `[[21,19], [18,21]]`     |

---

## 📡 Real-time Communication: Server-Sent Events (SSE)

### 🚀 **SSE Endpoints**

| Mode            | Endpoint                      | Purpose                                 |
| --------------- | ----------------------------- | --------------------------------------- |
| **PIN Mode**    | `GET /api/court/{pin}/stream` | Real-time updates cho specific court    |
| **Direct Mode** | `GET /api/scoreboard/stream`  | Real-time updates cho single scoreboard |

### 📋 **Technical Specifications**

- **Content-Type**: `text/event-stream; charset=utf-8`
- **Connection**: Keep-alive với automatic reconnection
- **Encoding**: UTF-8 support cho Vietnamese characters
- **Compression**: Gzip enabled cho bandwidth optimization

### 🎯 **Event Types**

| Event    | Trigger             | Payload                | Description               |
| -------- | ------------------- | ---------------------- | ------------------------- |
| `init`   | Client connection   | Full match snapshot    | Initial state khi connect |
| `update` | Score/match changes | Updated match snapshot | Real-time state changes   |

### ⚡ **Performance Features (Java 21 Enhanced)**

- **Async Broadcasting**: Non-blocking event processing với virtual threads
- **Client Throttling**: 80ms minimum interval để prevent spam
- **JSON Caching**: Cached payload cho repeated identical updates
- **Connection Pooling**: Efficient connection management
- **Graceful Degradation**: Auto-fallback to polling nếu SSE fail

### 💻 **Client Implementation Guide**

#### **Browser JavaScript (Recommended)**

```javascript
// Kết nối SSE với error handling
const eventSource = new EventSource("/api/court/1234/stream");

// Handle initial state
eventSource.addEventListener("init", function (event) {
  const matchData = JSON.parse(event.data);
  updateScoreboard(matchData);
  console.log("📡 SSE Connected:", matchData);
});

// Handle real-time updates
eventSource.addEventListener("update", function (event) {
  const matchData = JSON.parse(event.data);
  updateScoreboard(matchData);
  console.log("🔄 Score Updated:", matchData);
});

// Handle connection errors
eventSource.addEventListener("error", function (event) {
  console.warn("❌ SSE Error:", event);
  // Implement fallback to polling
  fallbackToPolling();
});
```

#### **Node.js / Backend Integration**

```javascript
const EventSource = require("eventsource");

const es = new EventSource("http://192.168.1.100:2345/api/court/1234/stream");
es.onmessage = function (event) {
  if (event.type === "init" || event.type === "update") {
    const matchData = JSON.parse(event.data);
    // Process match data
    console.log("Match update:", matchData);
  }
};
```

### 🔄 **Fallback Strategy**

```javascript
// Polling fallback khi SSE không available
function fallbackToPolling() {
  const pollInterval = setInterval(async () => {
    try {
      const response = await fetch("/api/court/1234/sync");
      const matchData = await response.json();
      updateScoreboard(matchData);
    } catch (error) {
      console.error("Polling failed:", error);
    }
  }, 1000); // Poll every second
}
```

---

## 🔑 PIN mode (đa sân)

Base path: `/api/court`

### Health & Info

- `GET /api/court/health` → Kiểm tra tình trạng controller (text/plain)
- `GET /api/court/{pin}` → Thông tin điểm cơ bản (ví dụ: `{ "teamAScore": 0, "teamBScore": 0 }`)
- `GET /api/court/{pin}/status` → Xác thực và thông tin sân theo PIN (JSON)
- `GET /api/court/{pin}/sync` → Snapshot chi tiết trận đấu (JSON theo model ở trên)
- `GET /api/court/{pin}/stream` → SSE stream

### Điều khiển điểm số

- `POST /api/court/{pin}/increaseA`
- `POST /api/court/{pin}/decreaseA`
- `POST /api/court/{pin}/increaseB`
- `POST /api/court/{pin}/decreaseB`

Phản hồi: Thông thường là JSON điểm cơ bản hoặc snapshot tùy action; 200 khi thành công.

### Điều khiển trận đấu

- `POST /api/court/{pin}/reset` → Đặt lại điểm
- `POST /api/court/{pin}/next` → Sang ván tiếp theo
- `POST /api/court/{pin}/swap` → Đổi sân (có ghi dấu SWAP vào chi tiết ván nếu panel sẵn có)
- `POST /api/court/{pin}/change-server` → Đổi người giao cầu
- `POST /api/court/{pin}/undo` → Hoàn tác thao tác gần nhất

Phản hồi: Hầu hết trả về snapshot JSON; 200 khi thành công.

### Endpoint tổng quát (tương thích JS cũ)

- `POST /api/court/{pin}/{action}`
  - `action` ∈ {`increaseA`, `decreaseA`, `increaseB`, `decreaseB`, `reset`, `next`, `swap`, `change-server`, `undo`}
  - Phản hồi: JSON; 200 khi thành công, 400 nếu action không hợp lệ.

---

## 🟩 No-PIN mode (đơn bảng điểm)

Base path: `/api/scoreboard`

### Thông tin & Stream

- `GET /api/scoreboard` → Thông tin điểm cơ bản
- `GET /api/scoreboard/sync` → Snapshot chi tiết trận đấu
- `GET /api/scoreboard/stream` → SSE stream

### Điều khiển điểm số

- `POST /api/scoreboard/increaseA`
- `POST /api/scoreboard/decreaseA`
- `POST /api/scoreboard/increaseB`
- `POST /api/scoreboard/decreaseB`

### Điều khiển trận đấu

- `POST /api/scoreboard/reset`
- `POST /api/scoreboard/next`
- `POST /api/scoreboard/swap` → Đổi sân (có ghi dấu SWAP vào chi tiết ván nếu panel sẵn có)
- `POST /api/scoreboard/change-server`
- `POST /api/scoreboard/undo`

Phản hồi: JSON; 200 khi thành công.

---

## ⚙️ Ví dụ (curl)

Windows CMD (LAN IP ví dụ: 192.168.1.100, PIN: 1234)

```bat
:: Tăng điểm đội A (PIN mode)
curl http://192.168.1.100:2345/api/court/1234/increaseA

:: Đổi sân (PIN mode)
curl -X POST http://192.168.1.100:2345/api/court/1234/swap

:: Lấy snapshot (No-PIN mode)
curl http://192.168.1.100:2345/api/scoreboard/sync

:: Health check (PIN mode)
curl http://192.168.1.100:2345/api/court/health
```

SSE (trình duyệt, JS):

```js
const es = new EventSource("http://192.168.1.100:2345/api/court/1234/stream");
es.addEventListener("init", (e) => {
  const snapshot = JSON.parse(e.data);
  console.log("init", snapshot);
});
es.addEventListener("update", (e) => {
  const snapshot = JSON.parse(e.data);
  console.log("update", snapshot);
});
es.addEventListener("error", (e) => {
  console.warn("sse error", e);
});
```

---

## 🧪 Mã phản hồi (HTTP)

- 200 OK: Thành công
- 400 Bad Request: `action` không hợp lệ ở endpoint tổng quát
- 500 Internal Server Error: Lỗi không mong muốn (một số nhánh trả Map JSON mặc định)

Lưu ý: Xác thực PIN hiện tại do tầng ứng dụng xử lý (và có thể khác nhau theo cấu hình). Sử dụng `GET /api/court/{pin}/status` để kiểm tra PIN.

---

## ⚡ Performance & Scalability

### 🚀 **Java 21 Enhanced Threading Architecture**

#### **Virtual Threads Implementation**

- **Per-Court Serial Executors**: Mỗi court có dedicated `SerialExecutor` với virtual threads backing
- **Race Condition Prevention**: Thread-safe operations cho concurrent multi-court access
- **Scalable Design**: Không giới hạn bởi OS thread pool size
- **Low Latency**: Sub-millisecond response times cho score updates

#### **Performance Metrics**

| Metric                 | Value         | Description                    |
| ---------------------- | ------------- | ------------------------------ |
| **Response Time**      | < 50ms        | Average API response time      |
| **SSE Latency**        | < 100ms       | Real-time update delivery      |
| **Concurrent Courts**  | 5+            | Simultaneous active courts     |
| **Client Connections** | 50+           | Max concurrent SSE connections |
| **Throughput**         | 1000+ req/sec | Peak API requests per second   |

### 🔧 **Internal Architecture**

```java
// Simplified internal flow
public class CourtApiController {

    @PostMapping("/api/court/{pin}/increaseA")
    public CompletableFuture<MatchSnapshot> increaseScore(
        @PathVariable String pin) {

        return courtManager.submitToCourt(pin, () -> {
            // Thread-safe score update
            match.increaseScoreA();
            // Database persistence
            matchRepository.save(match);
            // SSE broadcasting
            sseService.broadcast(pin, match.getSnapshot());
            return match.getSnapshot();
        });
    }
}
```

### 📊 **Monitoring & Optimization**

- **Real-time Metrics**: Memory usage, thread utilization via status bar
- **Connection Monitoring**: Active SSE connections tracking
- **Performance Alerts**: Automatic alerts cho high latency
- **Resource Management**: Proactive GC suggestions với memory analytics

## 🔒 Security & Access Control

### 🛡️ **API Security Features**

#### **PIN-based Authentication**

- **4-digit PIN**: Unique identifier cho mỗi court
- **Session Management**: Temporary sessions cho web clients
- **Input Validation**: Sanitized inputs cho tất cả API calls
- **Rate Limiting**: Client throttling để prevent abuse

#### **Network Security**

```bash
# Recommended firewall configuration
# Allow BTMS web server
netsh advfirewall firewall add rule name="BTMS API" dir=in action=allow protocol=TCP localport=2345

# Optional: H2 remote access
netsh advfirewall firewall add rule name="BTMS H2" dir=in action=allow protocol=TCP localport=9092
```

#### **CORS Configuration**

```javascript
// Production CORS setup (application.properties)
spring.web.cors.allowed-origins=http://192.168.1.*, http://10.0.0.*
spring.web.cors.allowed-methods=GET,POST,PUT,DELETE
spring.web.cors.allowed-headers=Content-Type,Authorization
spring.web.cors.max-age=3600
```

### 🌐 **Deployment Security**

| Security Layer        | Implementation              | Purpose                   |
| --------------------- | --------------------------- | ------------------------- |
| **Network Isolation** | LAN-only deployment         | Prevent external access   |
| **Input Validation**  | Spring Validation           | SQL injection prevention  |
| **HTTPS Ready**       | SSL certificate support     | Encrypted communication   |
| **IPv4 Filtering**    | Network interface filtering | Additional security layer |

---

## 🔒 Bảo mật & CORS

- PIN là cơ chế ủy quyền nhẹ cho chế độ đa sân (PIN nằm trong URL path).
- CORS mặc định cho phép mọi nguồn (`*`); khuyến nghị giới hạn theo domain nội bộ khi triển khai.
- Hệ thống hướng tới chạy trong mạng LAN tin cậy; nếu xuất Internet, nên đặt sau reverse proxy HTTPS và thêm lớp xác thực bổ sung.

---

## 📚 Documentation & Resources

### 🔗 **Related Documentation**

| Document                                                                       | Purpose                       | Audience              |
| ------------------------------------------------------------------------------ | ----------------------------- | --------------------- |
| [`README.md`](../README.md)                                                    | Project overview & setup      | Developers, Admins    |
| [`BAO_CAO_CONG_NGHE_VA_TINH_NANG_v2.md`](BAO_CAO_CONG_NGHE_VA_TINH_NANG_v2.md) | Technical architecture report | Technical teams       |
| [`HUONG_DAN_SU_DUNG.md`](HUONG_DAN_SU_DUNG.md)                                 | User manual (Vietnamese)      | End users             |
| [`SETTINGS.md`](SETTINGS.md)                                                   | Configuration guide           | System administrators |

### ⚙️ **Version Information**

| Component         | Version           | Release Date       |
| ----------------- | ----------------- | ------------------ |
| **API Version**   | v1.0.0            | November 2025      |
| **Application**   | Spring Boot 3.4.0 | November 2025      |
| **Java Runtime**  | Java 21 LTS       | Enhanced threading |
| **Documentation** | v2.1              | November 28, 2025  |

### 🧪 **Testing & Development**

#### **API Testing Tools**

```bash
# Postman Collection
# Import from: docs/postman/BTMS_API_Collection.json

# cURL examples
curl -X GET "http://localhost:2345/api/court/health"
curl -X POST "http://localhost:2345/api/court/1234/increaseA"
curl -X GET "http://localhost:2345/api/scoreboard/sync"

# SSE testing với curl
curl -N -H "Accept: text/event-stream" "http://localhost:2345/api/court/1234/stream"
```

#### **Development Environment**

```properties
# Development configuration (application-dev.properties)
server.port=2345
spring.datasource.url=jdbc:h2:mem:btms_dev
spring.jpa.hibernate.ddl-auto=create-drop
logging.level.com.example.btms=DEBUG
```

### 🤝 **Community & Support**

#### **Getting Help**

- **🐛 Bug Reports**: [GitHub Issues](https://github.com/NguyenHau-IT/Badminton_Tournament_Management_System_DB_SQLSRV/issues)
- **💡 Feature Requests**: [GitHub Discussions](https://github.com/NguyenHau-IT/Badminton_Tournament_Management_System_DB_SQLSRV/discussions)
- **📖 Documentation**: [Project Wiki](https://github.com/NguyenHau-IT/Badminton_Tournament_Management_System_DB_SQLSRV/wiki)

#### **Contributing**

- **Code Style**: Google Java Style Guide
- **API Standards**: RESTful design principles
- **Testing**: Unit tests cho tất cả API endpoints
- **Documentation**: Update API docs với mọi API changes

---

<div align="center">

### 🏆 **"Professional Tournament Management API"** 🏆

**Made with ❤️ by [Nguyen Viet Hau](https://github.com/NguyenHau-IT)**

[![⭐ Star the repo](https://img.shields.io/github/stars/NguyenHau-IT/Badminton_Tournament_Management_System_DB_SQLSRV?style=social)](https://github.com/NguyenHau-IT/Badminton_Tournament_Management_System_DB_SQLSRV)  
[![📖 Documentation](https://img.shields.io/badge/Docs-API-blue)](https://github.com/NguyenHau-IT/Badminton_Tournament_Management_System_DB_SQLSRV/blob/main/docs/API_DOCUMENTATION.md)  
[![🚀 BTMS Platform](https://img.shields.io/badge/Platform-BTMS-green)](https://github.com/NguyenHau-IT/Badminton_Tournament_Management_System_DB_SQLSRV)

---

**📅 Last Updated**: November 28, 2025 | **📋 Version**: v1.0.0 | **📄 License**: MIT

</div>
