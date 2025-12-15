# 📡 Tài liệu Server-Sent Events (SSE)

## Hệ thống Quản lý Giải đấu Cầu lông (BTMS)

---

## 📋 Mục lục

1. [Tổng quan về SSE](#1-tổng-quan-về-sse)
2. [Kiến trúc SSE trong BTMS](#2-kiến-trúc-sse-trong-btms)
3. [Triển khai Backend](#3-triển-khai-backend)
4. [Triển khai Frontend](#4-triển-khai-frontend)
5. [Điểm cuối SSE](#5-điểm-cuối-sse)
6. [Các loại sự kiện](#6-các-loại-sự-kiện)
7. [Cấu trúc dữ liệu SSE](#7-cấu-trúc-dữ-liệu-sse)
8. [Quản lý kết nối](#8-quản-lý-kết-nối)
9. [Hiệu suất & Tối ưu hóa](#9-hiệu-suất--tối-ưu-hóa)
10. [Xử lý lỗi](#10-xử-lý-lỗi)
11. [Bảo mật](#11-bảo-mật)
12. [Khắc phục sự cố](#12-khắc-phục-sự-cố)

---

## 1. Tổng quan về SSE

### 1.1 Server-Sent Events là gì?

Server-Sent Events (SSE) là công nghệ cho phép máy chủ đẩy dữ liệu thời gian thực đến ứng dụng web khách hàng thông qua một kết nối HTTP duy nhất. Khác với WebSocket (hai chiều), SSE chỉ hoạt động một chiều (máy chủ → khách hàng).

### 1.2 Tại sao BTMS sử dụng SSE?

- **Cập nhật điểm số thời gian thực**: Cập nhật điểm số ngay lập tức
- **Đồng bộ đa thiết bị**: Đồng bộ hóa giữa nhiều thiết bị
- **Tự động kết nối lại**: Tự động kết nối lại khi mất kết nối
- **Độ trễ thấp**: Độ trễ thấp cho cập nhật trực tiếp
- **Hiệu quả băng thông**: Chỉ gửi khi có thay đổi

### 1.3 Các trường hợp sử dụng trong BTMS

- 🏸 **Bảng điểm**: Cập nhật điểm số thời gian thực
- 📱 **Hệ thống PIN**: Đồng bộ giữa thiết bị quản lý và hiển thị
- 🏆 **Giải đấu**: Cập nhật trực tiếp cho giải đấu
- 📊 **Trạng thái trận đấu**: Thay đổi trạng thái trận đấu

---

## 2. Kiến trúc SSE trong BTMS

### 2.1 Tổng quan kiến trúc

```
┌─────────────────┐    SSE Stream     ┌─────────────────┐
│   Web Client    │◄──────────────────│   SSE Server    │
│  (Scoreboard)   │                   │ (Spring Boot)   │
└─────────────────┘                   └─────────────────┘
         │                                       │
         │ HTTP POST (Actions)                   │
         └─────────────────────────────────────► │
                                                 │
┌─────────────────┐    SSE Stream     ┌─────────────────┤
│ Mobile Client   │◄──────────────────│ SseEmitterMgr   │
│  (PIN View)     │                   │ (Connection Mgmt)│
└─────────────────┘                   └─────────────────┘
```

### 2.2 Kiến trúc thành phần

```
📦 Hệ thống SSE
├── 🎯 Controllers
│   ├── ScoreboardController.java          # SSE bảng điểm chính
│   ├── ScoreboardPinController.java       # SSE dựa trên PIN
│   ├── SseController.java                 # Điểm cuối SSE tổng quát
│   └── MatchApiController.java            # SSE cụ thể cho trận đấu
├── 🔧 Services
│   ├── SseEmitterManager.java             # Quản lý pool kết nối
│   └── BackgroundTaskManager.java         # Xử lý tác vụ không đồng bộ
├── 🌐 Frontend
│   ├── scoreboard.js                      # SSE client chính
│   └── tournament-realtime-scores.js      # SSE client giải đấu
└── 📋 Models
    └── BadmintonMatch.java                # Model trạng thái trận đấu
```

---

## 3. Triển khai Backend

### 3.1 ScoreboardController (Điểm cuối SSE chính)

**File**: `ScoreboardController.java`

```java
@GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter stream() {
    SseEmitter em = new SseEmitter(0L); // Không có timeout
    clients.add(em);

    try {
        // Gửi dữ liệu ban đầu
        em.send(SseEmitter.event()
            .name("init")
            .data(om.writeValueAsString(match.snapshot())));
    } catch (IOException ignore) {}

    // Thiết lập callbacks
    em.onCompletion(() -> clients.remove(em));
    em.onTimeout(() -> clients.remove(em));
    em.onError(e -> clients.remove(em));

    return em;
}
```

**Tính năng**:

- ✅ Không có timeout (kết nối dài hạn)
- ✅ Tự động dọn dẹp khi ngắt kết nối
- ✅ Ảnh chụp ban đầu khi kết nối
- ✅ Danh sách client an toàn luồng (CopyOnWriteArrayList)

### 3.2 ScoreboardPinController (SSE dựa trên PIN)

**File**: `ScoreboardPinController.java`

```java
@GetMapping(value = "/{pin}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter streamWithPin(@PathVariable String pin) {
    SseEmitter em = new SseEmitter(300000L); // Timeout 5 phút

    // Thêm vào danh sách client cụ thể theo PIN
    pinClients.computeIfAbsent(pin, k -> new CopyOnWriteArrayList<>()).add(em);

    try {
        BadmintonMatch match = getOrCreateMatch(pin);
        em.send(SseEmitter.event()
            .name("init")
            .data(om.writeValueAsString(match.snapshot())));
    } catch (IOException ignore) {
        // Xóa client bị lỗi ngay lập tức
        List<SseEmitter> clients = pinClients.get(pin);
        if (clients != null) clients.remove(em);
    }

    return em;
}
```

**Tính năng nâng cao**:

- 🔒 Phân lập dựa trên PIN
- ⏰ Timeout ngắn hơn (5 phút)
- 📊 Tối ưu hóa hiệu suất với throttling
- 🧹 Tác vụ dọn dẹp nền
- 💾 Bộ nhớ cache payload JSON

### 3.3 SseEmitterManager (Pool kết nối)

**File**: `SseEmitterManager.java`

```java
@Service
public class SseEmitterManager {
    // Pool kết nối theo loại tài nguyên
    private final Map<String, Set<SseEmitter>> tournamentEmitters = new ConcurrentHashMap<>();
    private final Map<String, Set<SseEmitter>> matchEmitters = new ConcurrentHashMap<>();

    // Cấu hình
    private static final long SSE_TIMEOUT = 30 * 60 * 1000L; // 30 phút
    private static final long HEARTBEAT_INTERVAL = 15000L;   // 15 giây
    private static final int MAX_CONNECTIONS_PER_RESOURCE = 1000;
}
```

**Tính năng**:

- 📊 Pool kết nối theo tài nguyên
- 💓 Cơ chế heartbeat (khoảng 15 giây)
- 🔄 Tự động dọn dẹp kết nối chết
- 🚫 Giới hạn kết nối (1000/tài nguyên)
- 📈 Theo dõi thống kê

### 3.4 Hệ thống phát sóng

```java
private void broadcastSnapshot() {
    String payload;
    try {
        payload = om.writeValueAsString(match.snapshot());
    } catch (JsonProcessingException e) {
        return; // Lỗi serialization
    }

    for (SseEmitter client : clients) {
        try {
            client.send(SseEmitter.event()
                .name("update")
                .data(payload));
        } catch (IOException | IllegalStateException ex) {
            clients.remove(client);
            try {
                client.complete();
            } catch (IllegalStateException ignore) {}
        }
    }
}
```

**Tính năng tối ưu hóa**:

- 🚀 Throttling (tối thiểu 50ms giữa các lần phát sóng)
- 💾 Bộ nhớ cache payload JSON
- 🔄 Phát sóng không đồng bộ với BackgroundTaskManager
- 🧹 Dọn dẹp client chết

---

## 4. Triển khai Frontend

### 4.1 SSE Client chính (scoreboard.js)

```javascript
// ======= SSE với fallback =======
let esRef = null;
function startSSE() {
  if (!window.EventSource) return false;

  try {
    const pin = getPinCodeFromUrl();
    const endpoint = pin
      ? `/api/court/${pin}/stream`
      : "/api/scoreboard/stream";
    const es = new EventSource(API_BASE + endpoint);
    esRef = es;

    usingSSE = true;
    $("#liveBadge").removeClass("d-none");

    // Event listeners
    es.addEventListener("init", (e) => {
      renderScores(JSON.parse(e.data));
    });

    es.addEventListener("update", (e) => {
      const now = performance.now();
      if (now - last < minGap) return; // Throttling
      last = now;
      renderScores(JSON.parse(e.data));
    });

    es.onerror = () => {
      usingSSE = false;
      $("#liveBadge").addClass("d-none");
      setupAutoRefresh(true); // Fallback to polling
      es.close();
    };

    return true;
  } catch {
    return false;
  }
}
```

**Tính năng**:

- ✅ Tự động fallback sang polling
- ✅ Throttling phía client (tối thiểu 80ms)
- ✅ Chỉ báo kết nối trực quan
- ✅ Xử lý lỗi và kết nối lại

### 4.2 SSE Client giải đấu (tournament-realtime-scores.js)

```javascript
class RealtimeScoreUpdater {
  constructor(matchId) {
    this.matchId = matchId;
    this.eventSource = null;
    this.reconnectAttempts = 0;
    this.maxReconnectAttempts = 5;

    this.init();
  }

  connect() {
    const url = `/api/matches/${this.matchId}/score-stream`;
    this.eventSource = new EventSource(url);

    // Xử lý sự kiện
    this.eventSource.addEventListener("score-update", (event) => {
      const data = JSON.parse(event.data);
      this.updateScore(data);
    });

    this.eventSource.addEventListener("set-update", (event) => {
      const data = JSON.parse(event.data);
      this.updateSet(data);
    });

    this.eventSource.addEventListener("heartbeat", () => {
      console.log("💓 Nhận được heartbeat");
    });

    this.eventSource.addEventListener("error", (error) => {
      this.isConnected = false;
      this.eventSource.close();
      this.attemptReconnect();
    });
  }

  attemptReconnect() {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      this.useFallback = true;
      this.startPolling();
      return;
    }

    this.reconnectAttempts++;
    setTimeout(() => this.connect(), 3000);
  }
}
```

**Tính năng nâng cao**:

- 🔄 Kết nối lại thông minh (tối đa 5 lần thử)
- 📊 Nhiều loại sự kiện
- 💫 Hoạt ảnh mượt mà
- 📱 Trạng thái kết nối đáp ứng
- 🔄 Fallback sang polling

---

## 5. Điểm cuối SSE

### 5.1 Điểm cuối bảng điểm

| Điểm cuối                 | Phương thức | Mô tả               | Timeout       |
| ------------------------- | ----------- | ------------------- | ------------- |
| `/api/scoreboard/stream`  | GET         | SSE bảng điểm chính | Không timeout |
| `/api/court/{pin}/stream` | GET         | SSE cụ thể theo PIN | 5 phút        |

### 5.2 Điểm cuối giải đấu & trận đấu

| Điểm cuối                             | Phương thức | Mô tả                        | Tính năng          |
| ------------------------------------- | ----------- | ---------------------------- | ------------------ |
| `/api/sse/tournaments/{id}/subscribe` | GET         | Cập nhật giải đấu            | Heartbeat, pooling |
| `/api/sse/matches/{id}/subscribe`     | GET         | Cập nhật cụ thể cho trận đấu | Tự động dọn dẹp    |
| `/api/matches/{matchId}/score-stream` | GET         | Điểm số thời gian thực       | Hỗ trợ fallback    |

### 5.3 Điểm cuối phát sóng

| Điểm cuối                            | Phương thức | Mô tả              | Bảo mật        |
| ------------------------------------ | ----------- | ------------------ | -------------- |
| `/api/sse/matches/{id}/score-update` | POST        | Phát sóng điểm số  | Sử dụng nội bộ |
| `/api/sse/tournaments/{id}/update`   | POST        | Phát sóng giải đấu | Chỉ admin      |

### 5.4 Điểm cuối quản lý

| Điểm cuối        | Phương thức | Mô tả            | Phản hồi   |
| ---------------- | ----------- | ---------------- | ---------- |
| `/api/sse/stats` | GET         | Thống kê kết nối | JSON stats |

---

## 6. Các loại sự kiện

### 6.1 Sự kiện bảng điểm

| Tên sự kiện | Mô tả                    | Định dạng dữ liệu        | Tần suất        |
| ----------- | ------------------------ | ------------------------ | --------------- |
| `init`      | Kết nối ban đầu          | Ảnh chụp trận đấu đầy đủ | Một lần         |
| `update`    | Thay đổi điểm/trạng thái | Ảnh chụp trận đấu đầy đủ | Khi có thay đổi |
| `ping`      | Duy trì kết nối          | Chuỗi rỗng               | Khoảng 30 giây  |

**Ví dụ dữ liệu**:

```json
{
  "score": [21, 18],
  "games": [1, 0],
  "gameNumber": 2,
  "server": 0,
  "names": ["Đội A", "Đội B"],
  "bestOf": 3,
  "gameScores": [
    [21, 19],
    [18, 21]
  ]
}
```

### 6.2 Sự kiện giải đấu

| Tên sự kiện           | Mô tả                | Kích hoạt                | Dữ liệu           |
| --------------------- | -------------------- | ------------------------ | ----------------- |
| `connected`           | Client đã kết nối    | Thiết lập kết nối        | Thông tin kết nối |
| `tournament-update`   | Cập nhật tổng quát   | Hành động admin          | Dữ liệu giải đấu  |
| `match-update`        | Thay đổi trận đấu    | Thay đổi điểm/trạng thái | Dữ liệu trận đấu  |
| `registration-update` | Đăng ký mới          | Người chơi đăng ký       | Dữ liệu đăng ký   |
| `heartbeat`           | Ping duy trì kết nối | Theo lịch                | Timestamp         |

### 6.3 Sự kiện trận đấu

| Tên sự kiện      | Mô tả               | Khi nào           | Cấu trúc dữ liệu                       |
| ---------------- | ------------------- | ----------------- | -------------------------------------- | ---------- | ---------- |
| `score-update`   | Điểm số trực tiếp   | Ghi điểm          | `{team1Score, team2Score, currentSet}` |
| `set-complete`   | Ván hoàn thành      | Ván kết thúc      | `{setNumber, scores, winner}`          |
| `match-complete` | Trận đấu kết thúc   | Trận đấu kết thúc | `{finalScore, winner, duration}`       |
| `status-change`  | Cập nhật trạng thái | Thay đổi admin    | `{status: 'live'                       | 'finished' | 'paused'}` |

---

## 7. Cấu trúc dữ liệu SSE

### 7.1 Định dạng SSE Event

Mỗi sự kiện SSE được gửi theo định dạng chuẩn:

```
event: [event-name]
data: [json-data]

```

**Ví dụ thực tế**:
```
event: init
data: {"score":[0,0],"games":[0,0],"gameNumber":1,"server":0}

event: update 
data: {"score":[21,18],"games":[1,0],"gameNumber":2,"server":1}

event: ping
data: 

```

### 7.2 Cấu trúc BadmintonMatch.Snapshot

**Cấu trúc chính** được sử dụng trong tất cả sự kiện update:

```json
{
  "score": [21, 18],              // Điểm số hiện tại [Đội A, Đội B]
  "games": [2, 1],                // Số ván đã thắng [Đội A, Đội B]
  "gameNumber": 3,                // Ván đang thi đấu (1, 2, 3...)
  "server": 1,                    // Đội đang giao cầu (0=A, 1=B)
  "names": ["Nguyễn Văn A", "Trần Văn B"], // Tên đội/VĐV
  "bestOf": 3,                    // Tổng số ván (best of 3, 5...)
  "gameScores": [                 // Điểm các ván đã hoàn thành
    [21, 19],                     // Ván 1: A thắng 21-19
    [18, 21],                     // Ván 2: B thắng 21-18
    [15, 12]                      // Ván 3: đang thi đấu
  ],
  "matchStatus": "live",          // Trạng thái: "scheduled"|"live"|"finished"|"paused"
  "timestamp": 1702234567890      // Thời gian cập nhật (milliseconds)
}
```

### 7.3 Cấu trúc dữ liệu theo từng loại sự kiện

#### 7.3.1 Sự kiện `init` (Kết nối ban đầu)

```json
{
  "event": "init",
  "data": {
    "score": [0, 0],
    "games": [0, 0], 
    "gameNumber": 1,
    "server": 0,
    "names": ["Đội A", "Đội B"],
    "bestOf": 3,
    "gameScores": [],
    "matchStatus": "scheduled",
    "pinCode": "1234",           // Chỉ có khi dùng PIN
    "courtId": "court-001",      // ID sân đấu
    "timestamp": 1702234567890
  }
}
```

#### 7.3.2 Sự kiện `update` (Cập nhật điểm số)

```json
{
  "event": "update", 
  "data": {
    "score": [21, 18],
    "games": [1, 0],
    "gameNumber": 2,
    "server": 1,
    "names": ["Nguyễn Văn A", "Trần Văn B"],
    "bestOf": 3,
    "gameScores": [[21, 19]],    // Ván 1 đã hoàn thành
    "matchStatus": "live",
    "lastAction": "pointA",      // Hành động cuối: "pointA"|"pointB"|"nextGame"|"swap"
    "timestamp": 1702234567890
  }
}
```

#### 7.3.3 Sự kiện `score-update` (Cập nhật điểm cụ thể)

```json
{
  "event": "score-update",
  "data": {
    "matchId": "match-123",
    "team1Score": 21,
    "team2Score": 18, 
    "currentSet": 2,
    "team1Name": "Nguyễn Văn A",
    "team2Name": "Trần Văn B",
    "server": 1,
    "timestamp": 1702234567890
  }
}
```

#### 7.3.4 Sự kiện `set-complete` (Hoàn thành ván)

```json
{
  "event": "set-complete",
  "data": {
    "matchId": "match-123",
    "setNumber": 1,
    "scores": {
      "team1": 21,
      "team2": 19
    },
    "winner": 0,                 // 0=Team1, 1=Team2
    "duration": 1800000,         // Thời gian ván (ms)
    "totalGames": [1, 0],        // Tổng ván sau khi hoàn thành
    "nextSetServer": 1,          // Đội giao cầu đầu ván tiếp
    "timestamp": 1702234567890
  }
}
```

#### 7.3.5 Sự kiện `match-complete` (Hoàn thành trận đấu)

```json
{
  "event": "match-complete", 
  "data": {
    "matchId": "match-123",
    "finalScore": {
      "games": [2, 1],           // Kết quả cuối: 2-1
      "sets": [
        [21, 19],
        [18, 21], 
        [21, 17]
      ]
    },
    "winner": 0,                 // Đội thắng chung cuộc
    "duration": 5400000,         // Tổng thời gian trận (ms)
    "matchStatus": "finished",
    "winnerName": "Nguyễn Văn A",
    "timestamp": 1702234567890
  }
}
```

#### 7.3.6 Sự kiện `heartbeat` (Ping duy trì kết nối)

```json
{
  "event": "heartbeat",
  "data": {
    "timestamp": 1702234567890,
    "serverTime": "2025-12-11T10:30:00Z",
    "activeConnections": 45
  }
}
```

### 7.4 Cấu trúc dữ liệu Tournament SSE

#### 7.4.1 Tournament Update

```json
{
  "event": "tournament-update",
  "data": {
    "tournamentId": 123,
    "eventType": "bracket-update", // "registration"|"bracket-update"|"schedule-change"
    "timestamp": 1702234567890,
    "changes": {
      "matchId": "match-456",
      "status": "completed",
      "winner": "Nguyễn Văn A",
      "nextMatch": "match-789"
    }
  }
}
```

#### 7.4.2 Registration Update

```json
{
  "event": "registration-update",
  "data": {
    "tournamentId": 123,
    "playerId": "player-456", 
    "playerName": "Lê Văn C",
    "action": "registered",      // "registered"|"withdrawn"|"confirmed"
    "category": "MS",            // "MS"|"WS"|"MD"|"WD"|"XD"
    "totalRegistrations": 32,
    "timestamp": 1702234567890
  }
}
```

### 7.5 Cấu trúc lỗi SSE

```json
{
  "event": "error",
  "data": {
    "errorCode": "MATCH_NOT_FOUND",
    "message": "Không tìm thấy trận đấu",
    "details": "Match ID match-123 does not exist",
    "timestamp": 1702234567890,
    "retry": true               // Client có thể thử lại
  }
}
```

### 7.6 Schema Validation

**JSON Schema cho BadmintonMatch.Snapshot**:

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "required": ["score", "games", "gameNumber", "server"],
  "properties": {
    "score": {
      "type": "array",
      "items": {"type": "integer", "minimum": 0},
      "minItems": 2,
      "maxItems": 2
    },
    "games": {
      "type": "array", 
      "items": {"type": "integer", "minimum": 0},
      "minItems": 2,
      "maxItems": 2
    },
    "gameNumber": {
      "type": "integer",
      "minimum": 1
    },
    "server": {
      "type": "integer",
      "enum": [0, 1]
    },
    "names": {
      "type": "array",
      "items": {"type": "string"},
      "minItems": 2,
      "maxItems": 2
    },
    "bestOf": {
      "type": "integer",
      "enum": [1, 3, 5]
    },
    "matchStatus": {
      "type": "string", 
      "enum": ["scheduled", "live", "finished", "paused"]
    }
  }
}
```

---

## 8. Quản lý kết nối

### 8.1 Vòng đời kết nối

```
1. Yêu cầu Client
   ├── Kiểm tra hỗ trợ trình duyệt
   ├── Xác thực PIN (nếu có)
   └── Tạo SseEmitter

2. Thiết lập kết nối
   ├── Thêm vào pool client
   ├── Gửi sự kiện init
   ├── Thiết lập callbacks
   └── Bắt đầu heartbeat

3. Giai đoạn hoạt động
   ├── Nhận phát sóng
   ├── Xử lý heartbeats
   ├── Giám sát kết nối
   └── Tự động dọn dẹp client chết

4. Ngắt kết nối
   ├── Client đóng
   ├── Timeout xảy ra
   ├── Lỗi xảy ra
   └── Xóa khỏi pool
```

### 7.2 Quản lý pool client

**ScoreboardController**:

```java
// Clients toàn cục (không có PIN)
private final List<SseEmitter> clients = new CopyOnWriteArrayList<>();
```

**ScoreboardPinController**:

```java
// Clients cụ thể theo PIN
private final Map<String, List<SseEmitter>> pinClients = new ConcurrentHashMap<>();
```

**SseEmitterManager**:

```java
// Tổ chức theo loại tài nguyên
private final Map<String, Set<SseEmitter>> tournamentEmitters = new ConcurrentHashMap<>();
private final Map<String, Set<SseEmitter>> matchEmitters = new ConcurrentHashMap<>();
```

### 7.3 Chiến lược dọn dẹp

**Dọn dẹp tự động**:

- ✅ onCompletion callback
- ✅ onTimeout callback
- ✅ onError callback
- ✅ Tác vụ dọn dẹp nền (khoảng 30 giây)
- ✅ Phát hiện kết nối chết qua ping

**Dọn dẹp thủ công**:

- ✅ Điểm cuối admin để đóng kết nối
- ✅ Dọn dẹp dựa trên tài nguyên
- ✅ Thực thi giới hạn kết nối

---

## 9. Hiệu suất & Tối ưu hóa

### 8.1 Cơ chế throttling

**Throttling phía máy chủ**:

```java
// Tối thiểu 50ms giữa các lần phát sóng
private static final long MIN_BROADCAST_INTERVAL_MS = 50;

private void broadcastSnapshotToPin(String pinCode) {
    long currentTime = System.currentTimeMillis();
    AtomicLong lastTime = lastBroadcastTime.computeIfAbsent(pinCode, k -> new AtomicLong(0));

    if (currentTime - lastTime.get() < MIN_BROADCAST_INTERVAL_MS) {
        return; // Bỏ qua phát sóng
    }
    lastTime.set(currentTime);
    // ... tiến hành phát sóng
}
```

**Throttling phía client**:

```javascript
let last = 0;
const minGap = 80; // Tối thiểu 80ms

es.addEventListener("update", (e) => {
  const now = performance.now();
  if (now - last < minGap) return;
  last = now;
  renderScores(JSON.parse(e.data));
});
```

### 8.2 Chiến lược bộ nhớ cache

**Cache payload JSON**:

```java
// Cache để tránh serialization lặp lại
private final Map<String, String> jsonPayloadCache = new ConcurrentHashMap<>();

String payload = jsonPayloadCache.computeIfAbsent(pinCode + "_" + currentTime, k -> {
    try {
        return om.writeValueAsString(match.snapshot());
    } catch (Exception e) {
        return "{}";
    }
});
```

**Dọn dẹp cache**:

```java
// Dọn dẹp định kỳ để ngăn memory leak
if (jsonPayloadCache.size() > 100) {
    jsonPayloadCache.clear();
}
```

### 8.3 Xử lý không đồng bộ

**BackgroundTaskManager**:

```java
@Autowired
private BackgroundTaskManager taskManager;

// Phát sóng SSE không đồng bộ
taskManager.executeSseBroadcast(() -> {
    // Logic phát sóng ở đây
});
```

**Lợi ích**:

- 🚀 Phát sóng không chặn
- 📈 Throughput tốt hơn
- 🔄 Cải thiện concurrency
- ⚡ Giảm thời gian phản hồi

### 8.4 Quản lý bộ nhớ

**Giới hạn kết nối**:

```java
private static final int MAX_CONNECTIONS_PER_RESOURCE = 1000;

if (emitters.size() >= MAX_CONNECTIONS_PER_RESOURCE) {
    throw new IllegalStateException("Quá nhiều kết nối hoạt động");
}
```

**Dọn dẹp tài nguyên**:

- 🧹 Xóa client chết
- 💾 Giới hạn kích thước cache
- 🔄 Tác vụ dọn dẹp định kỳ
- 📊 Giám sát sử dụng bộ nhớ

---

## 10. Xử lý lỗi

### 9.1 Lỗi kết nối

**Xử lý lỗi phía máy chủ**:

```java
try {
    client.send(SseEmitter.event().name("update").data(payload));
} catch (IOException ex) {
    // Client đã ngắt kết nối - hành vi bình thường
    clients.remove(client);
    try {
        client.complete();
    } catch (Exception ignore) {}
} catch (Exception ex) {
    // Lỗi không mong muốn
    log.warn("Lỗi SSE không mong muốn: {}", ex.getMessage());
    clients.remove(client);
}
```

**Xử lý lỗi phía client**:

```javascript
es.onerror = () => {
  console.error("Lỗi kết nối SSE");
  usingSSE = false;
  $("#liveBadge").addClass("d-none");
  setupAutoRefresh(true); // Fallback
  es.close();
};
```

### 9.2 Cơ chế fallback

**Tự động fallback sang polling**:

```javascript
function startSSE() {
  if (!window.EventSource) return false;

  try {
    // Thiết lập SSE
    return true;
  } catch {
    return false;
  }
}

// Khởi tạo chính
const ok = startSSE();
if (!ok) setupAutoRefresh(true); // Fallback sang polling
```

**Degradation mềm mại**:

- ✅ Kiểm tra tương thích trình duyệt
- ✅ Xử lý lỗi mạng
- ✅ Khôi phục lỗi máy chủ
- ✅ Fallback trong suốt

### 9.3 Chiến lược kết nối lại

**Kết nối lại thông minh**:

```javascript
attemptReconnect() {
    if (this.reconnectAttempts >= CONFIG.maxReconnectAttempts) {
        this.useFallback = true;
        this.startPolling();
        return;
    }

    this.reconnectAttempts++;
    this.updateConnectionStatus('reconnecting');

    setTimeout(() => {
        this.connect();
    }, CONFIG.reconnectDelay);
}
```

**Chiến lược backoff**:

- 📈 Exponential backoff
- 🔄 Giới hạn thử lại tối đa
- ⏰ Delay có thể cấu hình
- 🛡️ Pattern circuit breaker

---

## 11. Bảo mật

### 10.1 Xác thực & Ủy quyền

**Bảo mật dựa trên PIN**:

```java
@GetMapping("/{pin}/status")
public ResponseEntity<Map<String, Object>> validatePin(@PathVariable String pin) {
    // Kiểm tra PIN có tồn tại trong CourtManagerService
    Map<String, CourtManagerService.CourtStatus> allCourts = courtManager.getAllCourtStatus();
    boolean pinExists = allCourts.values().stream()
            .anyMatch(court -> pin.equals(court.pinCode));

    if (pinExists) {
        return ResponseEntity.ok(response);
    } else {
        return ResponseEntity.notFound().build();
    }
}
```

**Xác minh thiết bị**:

```java
private ResponseEntity<Map<String, String>> checkVerifiedStatus(HttpSession session) {
    String sessionId = session.getId();

    if (!deviceSessionService.sessionExists(sessionId)) {
        return ResponseEntity.status(401).body(Map.of(
            "error", "Phiên đăng nhập không hợp lệ"));
    }

    if (deviceSessionService.isBlocked(sessionId)) {
        return ResponseEntity.status(403).body(Map.of(
            "error", "Thiết bị bị chặn"));
    }

    if (!deviceSessionService.isVerified(sessionId)) {
        return ResponseEntity.status(403).body(Map.of(
            "error", "Chưa được duyệt"));
    }

    return null; // OK
}
```

### 10.2 Giới hạn tốc độ

**Giới hạn kết nối**:

```java
private static final int MAX_CONNECTIONS_PER_RESOURCE = 1000;

if (emitters.size() >= MAX_CONNECTIONS_PER_RESOURCE) {
    logger.warn("Đạt giới hạn kết nối tối đa cho {}", resourceId);
    throw new IllegalStateException("Quá nhiều kết nối hoạt động");
}
```

**Throttling phát sóng**:

```java
private static final long MIN_BROADCAST_INTERVAL_MS = 50;
// Ngăn DoS qua cập nhật nhanh
```

### 10.3 Xác thực dữ liệu

**Làm sạch đầu vào**:

```java
@PathVariable String pin // Xác thực định dạng PIN
@RequestBody Map<String, Object> scoreData // Xác thực cấu trúc dữ liệu
```

**Lọc đầu ra**:

- 🔒 Loại bỏ dữ liệu nhạy cảm
- ✅ Xác thực cấu trúc JSON
- 🛡️ Ngăn XSS
- 📝 Audit logging

### 10.4 Cấu hình CORS

```java
@CrossOrigin(origins = "*", maxAge = 3600)
```

**Cân nhắc bảo mật**:

- ⚠️ Hiện tại: Cho phép tất cả origins (cho phát triển)
- 🔒 Production: Hạn chế domain cụ thể
- ⏰ Cache phản hồi preflight
- 🔐 Cân nhắc authentication headers

---

## 12. Khắc phục sự cố

### 11.1 Vấn đề thường gặp

**Vấn đề kết nối**:

```
Vấn đề: Kết nối SSE bị ngắt thường xuyên
Giải pháp:
- Kiểm tra tính ổn định mạng
- Xác minh cấu hình timeout
- Giám sát tài nguyên máy chủ
- Kiểm tra tương thích trình duyệt
```

**Vấn đề hiệu suất**:

```
Vấn đề: Sử dụng CPU cao trong quá trình phát sóng
Giải pháp:
- Bật throttling
- Triển khai caching
- Sử dụng xử lý không đồng bộ
- Giới hạn số lượng kết nối
```

**Memory leaks**:

```
Vấn đề: Sử dụng bộ nhớ tăng theo thời gian
Giải pháp:
- Bật dọn dẹp tự động
- Giám sát connection pools
- Kiểm tra emitters mồ côi
- Triển khai dọn dẹp định kỳ
```

### 11.2 Công cụ debug

**Debug phía máy chủ**:

```java
// Bật logging chi tiết
logging.level.com.example.btms.web.controller = DEBUG
logging.level.com.example.btms.service.sse = DEBUG
```

**Debug phía client**:

```javascript
// Giám sát console trình duyệt
console.log("Trạng thái SSE:", {
  connected: esRef?.readyState === EventSource.OPEN,
  url: esRef?.url,
  fallback: usingSSE,
});
```

**Thống kê kết nối**:

```bash
# Giám sát kết nối hoạt động
GET /api/sse/stats

Phản hồi:
{
  "tournamentConnections": 45,
  "matchConnections": 12,
  "totalConnections": 57,
  "tournamentChannels": 5,
  "matchChannels": 3
}
```

### 11.3 Giám sát sức khỏe

**Điểm cuối kiểm tra sức khỏe**:

```java
@GetMapping("/health")
public ResponseEntity<String> health() {
    return ResponseEntity.ok("ScoreboardPinController đang chạy!");
}
```

**Metrics hiệu suất**:

- 📊 Số lượng kết nối
- ⏱️ Thời gian phản hồi
- 💾 Sử dụng bộ nhớ
- 🔄 Tỷ lệ lỗi
- 📈 Throughput

### 11.4 Thông báo lỗi thường gặp

| Lỗi                           | Nguyên nhân                    | Giải pháp                         |
| ----------------------------- | ------------------------------ | --------------------------------- |
| "Quá nhiều kết nối hoạt động" | Đạt giới hạn kết nối           | Triển khai cleanup, tăng giới hạn |
| "SSE không được hỗ trợ"       | Trình duyệt cũ                 | Bật polling fallback              |
| "Connection timeout"          | Vấn đề mạng                    | Điều chỉnh timeout, kiểm tra mạng |
| "JSON parse error"            | Định dạng dữ liệu không hợp lệ | Xác thực cấu trúc dữ liệu         |
| "PIN không tìm thấy"          | Mã PIN không hợp lệ            | Xác minh PIN trong court manager  |

---

## 📝 Kết luận

SSE trong BTMS được thiết kế để cung cấp:

✅ **Hiệu suất thời gian thực**: Độ trễ thấp, cập nhật ngay lập tức  
✅ **Độ tin cậy**: Tự động reconnection, cơ chế fallback  
✅ **Khả năng mở rộng**: Connection pooling, quản lý tài nguyên  
✅ **Bảo mật**: Truy cập dựa trên PIN, xác minh thiết bị  
✅ **Khả năng bảo trì**: Kiến trúc rõ ràng, logging toàn diện

Hệ thống SSE của BTMS đảm bảo trải nghiệm người dùng mượt mà và đáng tin cậy cho việc theo dõi tỉ số trực tiếp và quản lý giải đấu.

---

## 📞 Hỗ trợ

Để được hỗ trợ về triển khai SSE:

- 📧 Email: btms-support@example.com
- 📖 Tài liệu: `/docs/`
- 🐛 Báo cáo lỗi: GitHub Issues
- 💬 Thảo luận: Team Chat

**Cập nhật lần cuối**: 11 tháng 12, 2025  
**Phiên bản**: 1.0  
**Tác giả**: BTMS Team
