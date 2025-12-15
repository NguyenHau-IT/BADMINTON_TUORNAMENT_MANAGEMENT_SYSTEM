# 📡 SSE Implementation trong BTMS

## Hệ thống Server-Sent Events thực tế

---

## 📋 Mục lục

1. [SSE Endpoints trong BTMS](#1-sse-endpoints-trong-btms)
2. [Event Messages & Data Format](#2-event-messages--data-format)
3. [Connection Management](#3-connection-management)
4. [Threading với UUID v7](#4-threading-với-uuid-v7)
5. [UDP Multicast Broadcasting](#5-udp-multicast-broadcasting)
6. [Monitoring & Stats](#6-monitoring--stats)

---

## 1. SSE Endpoints trong BTMS

### 1.1 ScoreboardController - SSE chính

**File**: `ScoreboardController.java`
**Endpoint**: `GET /api/scoreboard/stream`
**Function**: Stream điểm số badminton real-time
**Timeout**: Không giới hạn (0L)
**Init Event**: Gửi event "init" với match.snapshot() khi kết nối

### 1.2 ScoreboardPinController - SSE dựa trên PIN

**File**: `ScoreboardPinController.java`
**Endpoint**: `GET /api/court/{pin}/stream`
**Function**: Stream điểm số theo PIN code sân
**Parameter**: {pin} - Mã PIN sân đấu
**Validation**: Kiểm tra PIN hợp lệ, trả về 404 nếu không tồn tại
**Init Event**: Gửi event "init" với courtSession.match.snapshot()
**Additional Endpoint**: `GET /api/court/connections` - Xem danh sách tất cả court SSE connections

### 1.3 SseController - Endpoints tổng quát

**File**: `SseController.java`
**Base Path**: `/api/sse`

**Endpoints**:
- `GET /tournaments/{id}/subscribe` - Đăng ký nhận events của giải đấu
- `GET /matches/{id}/subscribe` - Đăng ký nhận events của trận đấu
- `GET /stats` - Thống kê kết nối SSE
- `GET /connections` - Xem danh sách chi tiết các kết nối SSE

**Function**: Quản lý subscription cho tournaments và matches

---

## 2. Event Messages & Data Format

### 2.1 Event Types thực tế

| Event Name | Description | Data Format |
|------------|-------------|-------------|
| `init` | Dữ liệu khởi tạo khi connect | BadmintonMatch.Snapshot |
| `update` | Cập nhật điểm số real-time | BadmintonMatch.Snapshot |
| `ping` | Heartbeat keep-alive | Empty hoặc timestamp |
| `connected` | Xác nhận kết nối thành công | Connection info |

### 2.2 BadmintonMatch.Snapshot Format

```json
{
  "score": [21, 18],
  "games": [1, 0], 
  "gameNumber": 2,
  "bestOf": 3,
  "gameScores": [[21, 19], [0, 0], [0, 0]],
  "server": 0,
  "names": ["Nguyễn A", "Trần B"],
  "clubs": ["CLB ABC", "CLB XYZ"],
  "finished": false,
  "winner": null
}
```

### 2.3 SSE Stats Response

```json
{
  "tournamentConnections": 5,
  "matchConnections": 12,
  "totalConnections": 17,
  "tournamentChannels": 3,
  "matchChannels": 8
}
```

### 2.4 SSE Connections List Response

**Endpoint**: `GET /api/sse/connections`

```json
{
  "summary": {
    "totalConnections": 17,
    "tournamentConnections": 5,
    "matchConnections": 12,
    "tournamentChannels": 3,
    "matchChannels": 8,
    "heartbeatInterval": 15000,
    "sseTimeout": 1800000,
    "maxConnectionsPerResource": 1000,
    "timestamp": 1703234567890
  },
  "tournaments": {
    "tournament-1": {
      "count": 3,
      "maxConnections": 1000,
      "type": "tournament",
      "resourceId": "1",
      "endpoint": "/api/sse/tournaments/1/subscribe",
      "status": "active",
      "lastUpdate": 1703234567890
    },
    "tournament-2": {
      "count": 2,
      "maxConnections": 1000,
      "type": "tournament", 
      "resourceId": "2",
      "endpoint": "/api/sse/tournaments/2/subscribe",
      "status": "active",
      "lastUpdate": 1703234567890
    }
  },
  "matches": {
    "match-101": {
      "count": 7,
      "maxConnections": 1000,
      "type": "match",
      "resourceId": "101",
      "endpoint": "/api/sse/matches/101/subscribe",
      "status": "active",
      "lastUpdate": 1703234567890
    },
    "match-102": {
      "count": 5,
      "maxConnections": 1000,
      "type": "match",
      "resourceId": "102", 
      "endpoint": "/api/sse/matches/102/subscribe",
      "status": "active",
      "lastUpdate": 1703234567890
    }
  }
}
```

### 2.5 Court SSE Connections List Response

**Endpoint**: `GET /api/court/connections`

```json
{
  "summary": {
    "totalConnections": 8,
    "activeCourts": 3,
    "totalCourts": 5,
    "timestamp": 1703234567890
  },
  "courts": {
    "court-001": {
      "pin": "1234",
      "courtId": "court-001",
      "header": "Nam đơn - Bán kết",
      "endpoint": "/api/court/1234/stream",
      "connections": 3,
      "status": "active",
      "match": {
        "score": [21, 18],
        "games": [1, 0],
        "gameNumber": 2,
        "finished": false,
        "names": ["Nguyễn A", "Trần B"]
      },
      "lastActivity": 1703234567890
    },
    "court-002": {
      "pin": "5678", 
      "courtId": "court-002",
      "header": "Nữ đôi - Chung kết",
      "endpoint": "/api/court/5678/stream",
      "connections": 5,
      "status": "active",
      "match": {
        "score": [11, 15],
        "games": [0, 1],
        "gameNumber": 2,
        "finished": false,
        "names": ["Đội A", "Đội B"]
      },
      "lastActivity": 1703234567890
    },
    "court-003": {
      "pin": "9999",
      "courtId": "court-003", 
      "header": "Chưa có trận đấu",
      "endpoint": "/api/court/9999/stream",
      "connections": 0,
      "status": "inactive",
      "match": null,
      "lastActivity": 1703234567890
    }
  }
}

### 2.6 Frontend SSE Client

**File**: `scoreboard.js`
**Function**: Kết nối SSE từ frontend
**Logic**: 
- Detect PIN từ URL để chọn endpoint
- Endpoint với PIN: `/api/court/{pin}/stream`
- Endpoint mặc định: `/api/scoreboard/stream`
- Throttle update events: 100ms
- Handle events: init, update, ping

---

## 3. Connection Management

### 3.1 SseEmitterManager - Connection Pooling

**File**: `SseEmitterManager.java`
**Function**: Quản lý pool kết nối SSE
**Storage**: 
- Tournament connections: Map<Integer, Set<SseEmitter>>
- Match connections: Map<Integer, Set<SseEmitter>>
**Features**: 
- Auto cleanup khi connection đóng
- Statistics tracking
- Thread-safe với ConcurrentHashMap

### 3.2 Heartbeat Mechanism

**Service**: `SseHeartbeatService`
**Interval**: 30 giây
**Event**: "ping" với data "heartbeat"
**Function**: Maintain kết nối SSE, detect client disconnect

---

## 4. Threading với UUID v7

### 4.1 ThreadLocal UUID Configuration

**File**: `ThreadUuidManager.java`
**Function**: Quản lý UUID v7 cho từng thread
**Features**:
- ThreadLocal storage cho UUID v7
- getCurrentThreadUuid() - Lấy UUID của thread hiện tại
- resetCurrentThreadUuid() - Reset và generate UUID mới
- getCurrentThreadInfo() - Lấy thông tin đầy đủ thread

### 4.2 Enhanced SSE Broadcasting với Thread UUID

**Function**: Async broadcast events với Thread UUID tracking
**Features**:
- Async execution với sseTaskExecutor
- Thread UUID embedding trong mỗi event
- Event ID generation based on Thread UUID
- Timestamp tracking cho mỗi broadcast

---

## 5. UDP Multicast Broadcasting

### 5.1 ScoreboardBroadcaster - UDP Multicast

**File**: `ScoreboardBroadcaster.java`
**Multicast**: `239.255.50.50:50505`
**Function**: Broadcast thông tin sân đấu qua UDP
**Session ID**: UUID v7 generated
**Broadcast Data**:
- Operation: "UPSERT" hoặc "DELETE"
- Session info: sid, client, host, courtId
- Match info: header, nameA, nameB
- Score data: scoreA, scoreB, gamesA, gamesB, game
- SSE info: webConnections, sseStatus, sseEndpoint
- Tracking: threadUuid, timestamp

### 5.2 MonitorTab - UDP Message Receiver

**File**: `MonitorTab.java`
**Function**: Nhận và hiển thị UDP messages
**Listen**: `239.255.50.50:50505`
**Operations**:
- "UPSERT": Cập nhật hoặc thêm mới court card
- "DELETE": Xóa court card
**UI Update**: Real-time update court cards với SSE connection info

### 5.3 UDP Message Format

```json
{
  "op": "UPSERT",
  "sid": "018c8c47-1234-7abc-9def-123456789abc",
  "courtId": "court-001", 
  "client": "admin-001",
  "header": "Nam đơn",
  "nameA": "Nguyễn A",
  "nameB": "Trần B",
  "scoreA": "21",
  "scoreB": "18",
  "gamesA": "1",
  "gamesB": "0",
  "game": "2",
  "webConnections": "3",
  "sseStatus": "active",
  "sseEndpoint": "/api/court/1234/stream",
  "threadUuid": "018c8c47-5678-7def-9abc-fedcba987654",
  "ts": "1703234567890"
}
```

---

## 6. Monitoring & Stats

### 6.1 Connection Statistics

**Endpoint**: `GET /api/sse/stats`

```json
{
  "tournamentConnections": 5,
  "matchConnections": 12, 
  "totalConnections": 17,
  "tournamentChannels": 3,
  "matchChannels": 8,
  "uptime": 3600000,
  "lastBroadcast": 1703234567890
}
```

### 6.3 MonitorTab UI Display

```
┌─────────────────────────────────────────────────────────────────┐
│ 🏟️ SÂN 1 (court-001) - Session: 018c8c47-1234-7abc-9def...    │
│ ├── 👥 Nguyễn A (21) vs Trần B (18)                            │  
│ ├── 🌐 Web: 3 SSE connections active                           │
│ ├── 📡 SSE: ✅ active                                          │
│ ├── 🔗 /api/court/1234/stream                                  │
│ ├── 🆔 Thread: 018c8c47-5678-7def...                          │
│ └── 👤 Client: admin-001                                       │
└─────────────────────────────────────────────────────────────────┘
```

### 6.4 DeviceMonitorPanel - Thiết bị kết nối

**File**: `DeviceMonitorPanel.java`

```
┌─────────────────────────────────────────────────────────────────┐
│ 📱 THIẾT BỊ ĐANG KẾT NỐI SSE                                   │
├─────────────────────────────────────────────────────────────────┤
│ Session ID    │ Trọng Tài │ Thiết bị    │ IP         │ Trạng thái │
│ abc123-...    │ REF001    │ iPhone 13   │ 192.168... │ ✅ Active  │
│ def456-...    │ REF002    │ Samsung S21 │ 192.168... │ ✅ Active  │
│ xyz789-...    │ REF003    │ Chrome PC   │ 192.168... │ ⚠️ Pending │
└─────────────────────────────────────────────────────────────────┘
```

---

## 📝 Tóm tắt Implementation

### SSE Endpoints thực tế:
- `/api/scoreboard/stream` - Bảng điểm chính
- `/api/court/{pin}/stream` - SSE dựa trên PIN
- `/api/sse/tournaments/{id}/subscribe` - Tournament events
- `/api/sse/matches/{id}/subscribe` - Match events
- `/api/sse/stats` - Connection statistics
- `/api/sse/connections` - Danh sách chi tiết các kết nối SSE
- `/api/court/connections` - Danh sách SSE connections của các court

### Event Messages:
- `init` - Dữ liệu khởi tạo với BadmintonMatch.Snapshot
- `update` - Cập nhật real-time điểm số
- `ping` - Heartbeat keep-alive

### Features đã implement:
✅ Connection pooling với SseEmitterManager
✅ Thread UUID v7 tracking
✅ UDP Multicast broadcasting (239.255.50.50:50505) 
✅ MonitorTab real-time display
✅ Heartbeat mechanism
✅ Statistics & monitoring endpoints
✅ Device session management

### Data Flow:
```
BadmintonMatch → PropertyChangeSupport → ScoreboardController → SSE Broadcast
                                      ↓
                              UDP Multicast → MonitorTab UI Update
                                      ↓
                              Thread UUID v7 → Tracking & Monitoring
```

---

**Cập nhật lần cuối**: 12 tháng 12, 2025  
**Version**: Implementation-focused v1.0  
**Tác giả**: BTMS Development Team