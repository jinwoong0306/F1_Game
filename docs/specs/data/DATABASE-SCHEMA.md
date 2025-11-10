# DATABASE-SCHEMA.md

## Overview
SQLite 데이터베이스 스키마 정의 및 데이터 관리 전략을 명세합니다.

**Owner**: Database Lead  
**Status**: Draft  
**Last Updated**: 2025-01-15

---

## Dependencies
- **Technology**: SQLite 3.45+ with JDBC driver
- **Related Specs**: `USER-SESSION.md`, `SAVE-SYSTEM.md`
- **External Libraries**: `org.xerial:sqlite-jdbc:3.45.0.0`

---

## Database Location

### Development
```
data/game.db                # Main database file
data/game.db-wal           # Write-Ahead Log (auto-generated)
data/game.db-shm           # Shared memory file (auto-generated)
```

### Production
```
{USER_HOME}/.f1racing/game.db
```

---

## Schema Definition

### Table: users
사용자 계정 정보

```sql
CREATE TABLE users (
    user_id INTEGER PRIMARY KEY AUTOINCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP,
    total_playtime INTEGER DEFAULT 0,  -- 초 단위
    avatar_id INTEGER DEFAULT 1,
    is_guest BOOLEAN DEFAULT 0,
    
    CHECK (LENGTH(username) >= 3),
    CHECK (LENGTH(username) <= 50)
);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
```

**Example Data**:
```sql
INSERT INTO users (username, password_hash, email) VALUES
('Player1', '$2a$10$...', 'player1@example.com'),
('Guest_12345', NULL, NULL);
```

---

### Table: vehicles
차량 마스터 데이터

```sql
CREATE TABLE vehicles (
    vehicle_id INTEGER PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    description TEXT,
    
    -- 성능 스탯 (0.0 ~ 1.0 normalized)
    top_speed REAL DEFAULT 0.8,
    acceleration REAL DEFAULT 0.8,
    handling REAL DEFAULT 0.8,
    durability REAL DEFAULT 0.8,
    
    -- 잠금 조건
    unlock_level INTEGER DEFAULT 1,
    unlock_price INTEGER DEFAULT 0,
    
    -- 메타데이터
    sprite_path VARCHAR(255),
    is_unlocked_by_default BOOLEAN DEFAULT 0,
    
    CHECK (top_speed BETWEEN 0 AND 1),
    CHECK (acceleration BETWEEN 0 AND 1),
    CHECK (handling BETWEEN 0 AND 1),
    CHECK (durability BETWEEN 0 AND 1)
);

CREATE INDEX idx_vehicles_unlock_level ON vehicles(unlock_level);
```

**Example Data**:
```sql
INSERT INTO vehicles VALUES
(1, 'car_01', 'Red Bull RB20', 'Balanced performance', 0.85, 0.80, 0.90, 0.75, 1, 0, 'vehicles/car_01/body.png', 1),
(2, 'car_02', 'Ferrari SF-24', 'High acceleration', 0.80, 0.95, 0.75, 0.80, 5, 10000, 'vehicles/car_02/body.png', 0),
(3, 'car_03', 'Mercedes W15', 'High top speed', 0.95, 0.75, 0.85, 0.70, 10, 25000, 'vehicles/car_03/body.png', 0);
```

---

### Table: user_vehicles
사용자별 차량 소유 현황

```sql
CREATE TABLE user_vehicles (
    user_id INTEGER NOT NULL,
    vehicle_id INTEGER NOT NULL,
    is_unlocked BOOLEAN DEFAULT 0,
    purchase_date TIMESTAMP,
    total_distance REAL DEFAULT 0.0,  -- km
    total_races INTEGER DEFAULT 0,
    customization_data TEXT,  -- JSON: {"color": "#FF0000", "decals": [...]}
    
    PRIMARY KEY (user_id, vehicle_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(vehicle_id) ON DELETE CASCADE
);

CREATE INDEX idx_user_vehicles_user ON user_vehicles(user_id);
```

---

### Table: tracks
트랙 마스터 데이터

```sql
CREATE TABLE tracks (
    track_id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    description TEXT,
    
    -- 트랙 특성
    length_km REAL NOT NULL,
    default_laps INTEGER DEFAULT 5,
    difficulty INTEGER DEFAULT 3,  -- 1=Easy, 5=Hard
    
    -- 파일 경로
    tmx_path VARCHAR(255) NOT NULL,
    minimap_path VARCHAR(255),
    
    -- 잠금 조건
    unlock_level INTEGER DEFAULT 1,
    is_unlocked_by_default BOOLEAN DEFAULT 0,
    
    CHECK (difficulty BETWEEN 1 AND 5),
    CHECK (length_km > 0)
);

CREATE INDEX idx_tracks_difficulty ON tracks(difficulty);
```

**Example Data**:
```sql
INSERT INTO tracks VALUES
('monaco', 'track_01_monaco', 'Monaco Grand Prix', 'Tight street circuit', 3.337, 5, 5, 'tracks/track_01_monaco/track.tmx', 'tracks/track_01_monaco/minimap.png', 1, 1),
('monza', 'track_02_monza', 'Monza Circuit', 'High-speed temple', 5.793, 5, 3, 'tracks/track_02_monza/track.tmx', 'tracks/track_02_monza/minimap.png', 3, 0);
```

---

### Table: lap_times
싱글플레이어 랩 타임 기록

```sql
CREATE TABLE lap_times (
    lap_id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    track_id VARCHAR(50) NOT NULL,
    vehicle_id INTEGER NOT NULL,
    
    -- 타임 기록 (밀리초)
    lap_time_ms INTEGER NOT NULL,
    
    -- 레이스 컨텍스트
    tire_type VARCHAR(10),  -- 'soft', 'medium', 'hard'
    weather VARCHAR(20) DEFAULT 'clear',
    
    -- 메타데이터
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_valid BOOLEAN DEFAULT 1,  -- 치트 방지
    
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (track_id) REFERENCES tracks(track_id) ON DELETE CASCADE,
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(vehicle_id) ON DELETE CASCADE,
    
    CHECK (lap_time_ms > 0)
);

CREATE INDEX idx_lap_times_user ON lap_times(user_id);
CREATE INDEX idx_lap_times_track ON lap_times(track_id, lap_time_ms);
CREATE INDEX idx_lap_times_leaderboard ON lap_times(track_id, is_valid, lap_time_ms);
```

**Leaderboard Query**:
```sql
-- Top 10 lap times for Monaco
SELECT 
    u.username,
    v.display_name AS vehicle,
    lt.lap_time_ms,
    lt.created_at
FROM lap_times lt
JOIN users u ON lt.user_id = u.user_id
JOIN vehicles v ON lt.vehicle_id = v.vehicle_id
WHERE lt.track_id = 'monaco' AND lt.is_valid = 1
ORDER BY lt.lap_time_ms ASC
LIMIT 10;
```

---

### Table: multiplayer_matches
멀티플레이어 경기 기록

```sql
CREATE TABLE multiplayer_matches (
    match_id VARCHAR(36) PRIMARY KEY,  -- UUID
    track_id VARCHAR(50) NOT NULL,
    num_laps INTEGER DEFAULT 5,
    
    -- 타임스탬프
    started_at TIMESTAMP NOT NULL,
    ended_at TIMESTAMP,
    
    -- 메타데이터
    server_version VARCHAR(20),
    match_type VARCHAR(20) DEFAULT 'casual',  -- 'casual', 'ranked'
    
    FOREIGN KEY (track_id) REFERENCES tracks(track_id)
);

CREATE INDEX idx_multiplayer_matches_started ON multiplayer_matches(started_at);
```

---

### Table: multiplayer_results
멀티플레이어 경기 결과 (개인)

```sql
CREATE TABLE multiplayer_results (
    result_id INTEGER PRIMARY KEY AUTOINCREMENT,
    match_id VARCHAR(36) NOT NULL,
    user_id INTEGER NOT NULL,
    vehicle_id INTEGER NOT NULL,
    
    -- 결과
    final_position INTEGER NOT NULL,  -- 1st, 2nd, 3rd, 4th
    best_lap_ms INTEGER,
    total_time_ms INTEGER,
    
    -- 통계
    num_collisions INTEGER DEFAULT 0,
    num_pit_stops INTEGER DEFAULT 0,
    distance_covered_km REAL,
    
    FOREIGN KEY (match_id) REFERENCES multiplayer_matches(match_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(vehicle_id) ON DELETE CASCADE,
    
    CHECK (final_position BETWEEN 1 AND 4)
);

CREATE INDEX idx_multiplayer_results_match ON multiplayer_results(match_id);
CREATE INDEX idx_multiplayer_results_user ON multiplayer_results(user_id);
```

**User Stats Query**:
```sql
-- User's multiplayer statistics
SELECT 
    COUNT(*) AS total_races,
    SUM(CASE WHEN final_position = 1 THEN 1 ELSE 0 END) AS wins,
    SUM(CASE WHEN final_position <= 3 THEN 1 ELSE 0 END) AS podiums,
    ROUND(AVG(final_position), 2) AS avg_position
FROM multiplayer_results
WHERE user_id = ?;
```

---

### Table: user_preferences
사용자 설정

```sql
CREATE TABLE user_preferences (
    user_id INTEGER PRIMARY KEY,
    
    -- 오디오
    master_volume REAL DEFAULT 0.8,
    music_volume REAL DEFAULT 0.7,
    sfx_volume REAL DEFAULT 0.8,
    
    -- 그래픽
    graphics_quality VARCHAR(20) DEFAULT 'high',  -- 'low', 'medium', 'high'
    vsync_enabled BOOLEAN DEFAULT 1,
    fps_limit INTEGER DEFAULT 60,
    
    -- 컨트롤
    controls_preset VARCHAR(20) DEFAULT 'default',  -- 'default', 'custom'
    invert_steering BOOLEAN DEFAULT 0,
    
    -- UI
    minimap_enabled BOOLEAN DEFAULT 1,
    hud_scale REAL DEFAULT 1.0,
    
    -- 개인정보
    show_online_status BOOLEAN DEFAULT 1,
    allow_friend_requests BOOLEAN DEFAULT 1,
    
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    
    CHECK (master_volume BETWEEN 0 AND 1),
    CHECK (music_volume BETWEEN 0 AND 1),
    CHECK (sfx_volume BETWEEN 0 AND 1),
    CHECK (hud_scale BETWEEN 0.5 AND 2.0)
);
```

---

### Table: achievements
도전과제 마스터 데이터

```sql
CREATE TABLE achievements (
    achievement_id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    icon_path VARCHAR(255),
    points INTEGER DEFAULT 10,
    
    -- 잠금 조건 (JSON)
    unlock_criteria TEXT NOT NULL,  -- {"type": "lap_time", "track": "monaco", "time_ms": 90000}
    
    is_hidden BOOLEAN DEFAULT 0
);
```

**Example Data**:
```sql
INSERT INTO achievements VALUES
('first_win', 'First Victory', 'Win your first multiplayer race', 'icons/trophy.png', 50, '{"type":"multiplayer_win","count":1}', 0),
('speed_demon', 'Speed Demon', 'Reach 300 km/h', 'icons/speedometer.png', 20, '{"type":"max_speed","value":300}', 0),
('monaco_master', 'Monaco Master', 'Complete Monaco in under 1:30', 'icons/monaco.png', 100, '{"type":"lap_time","track":"monaco","time_ms":90000}', 0);
```

---

### Table: user_achievements
사용자별 도전과제 달성 현황

```sql
CREATE TABLE user_achievements (
    user_id INTEGER NOT NULL,
    achievement_id VARCHAR(50) NOT NULL,
    unlocked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    progress REAL DEFAULT 0.0,  -- 0.0 ~ 1.0
    
    PRIMARY KEY (user_id, achievement_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (achievement_id) REFERENCES achievements(achievement_id) ON DELETE CASCADE
);

CREATE INDEX idx_user_achievements_user ON user_achievements(user_id);
```

---

## Database Manager Implementation

### Java Class: DatabaseManager

```java
public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:data/game.db";
    private Connection connection;
    
    public DatabaseManager() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(DB_URL);
            connection.setAutoCommit(true);
            initializeTables();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }
    
    private void initializeTables() throws SQLException {
        // Execute schema creation scripts
        executeSQLFile("migrations/001_initial_schema.sql");
        executeSQLFile("migrations/002_seed_data.sql");
    }
    
    private void executeSQLFile(String path) throws SQLException {
        String sql = Gdx.files.internal(path).readString();
        String[] statements = sql.split(";");
        
        for (String statement : statements) {
            if (!statement.trim().isEmpty()) {
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute(statement);
                }
            }
        }
    }
    
    public void dispose() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            Gdx.app.error("DatabaseManager", "Error closing connection", e);
        }
    }
}
```

---

## Data Access Objects (DAOs)

### UserDAO

```java
public class UserDAO {
    private Connection connection;
    
    public User createUser(String username, String password, String email) throws SQLException {
        String hashedPassword = PasswordHasher.hash(password);
        
        String sql = "INSERT INTO users (username, password_hash, email) VALUES (?, ?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, username);
            pstmt.setString(2, hashedPassword);
            pstmt.setString(3, email);
            pstmt.executeUpdate();
            
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                int userId = rs.getInt(1);
                return getUserById(userId);
            }
        }
        
        throw new SQLException("Failed to create user");
    }
    
    public User authenticateUser(String username, String password) throws SQLException {
        String sql = "SELECT user_id, password_hash FROM users WHERE username = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                String storedHash = rs.getString("password_hash");
                if (PasswordHasher.verify(password, storedHash)) {
                    int userId = rs.getInt("user_id");
                    updateLastLogin(userId);
                    return getUserById(userId);
                }
            }
        }
        
        return null; // Authentication failed
    }
    
    public User getUserById(int userId) throws SQLException {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
        }
        
        return null;
    }
    
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getInt("user_id"));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        user.setCreatedAt(rs.getTimestamp("created_at"));
        user.setLastLogin(rs.getTimestamp("last_login"));
        user.setTotalPlaytime(rs.getInt("total_playtime"));
        user.setAvatarId(rs.getInt("avatar_id"));
        user.setGuest(rs.getBoolean("is_guest"));
        return user;
    }
    
    private void updateLastLogin(int userId) throws SQLException {
        String sql = "UPDATE users SET last_login = CURRENT_TIMESTAMP WHERE user_id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.executeUpdate();
        }
    }
}
```

### LapTimeDAO

```java
public class LapTimeDAO {
    private Connection connection;
    
    public void saveLapTime(int userId, String trackId, int vehicleId, 
                            int lapTimeMs, String tireType) throws SQLException {
        String sql = "INSERT INTO lap_times (user_id, track_id, vehicle_id, lap_time_ms, tire_type) " +
                     "VALUES (?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, trackId);
            pstmt.setInt(3, vehicleId);
            pstmt.setInt(4, lapTimeMs);
            pstmt.setString(5, tireType);
            pstmt.executeUpdate();
        }
    }
    
    public List<LapTime> getLeaderboard(String trackId, int limit) throws SQLException {
        String sql = "SELECT lt.*, u.username, v.display_name " +
                     "FROM lap_times lt " +
                     "JOIN users u ON lt.user_id = u.user_id " +
                     "JOIN vehicles v ON lt.vehicle_id = v.vehicle_id " +
                     "WHERE lt.track_id = ? AND lt.is_valid = 1 " +
                     "ORDER BY lt.lap_time_ms ASC " +
                     "LIMIT ?";
        
        List<LapTime> leaderboard = new ArrayList<>();
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, trackId);
            pstmt.setInt(2, limit);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                LapTime lapTime = new LapTime();
                lapTime.setLapId(rs.getInt("lap_id"));
                lapTime.setUsername(rs.getString("username"));
                lapTime.setVehicleName(rs.getString("display_name"));
                lapTime.setLapTimeMs(rs.getInt("lap_time_ms"));
                lapTime.setCreatedAt(rs.getTimestamp("created_at"));
                leaderboard.add(lapTime);
            }
        }
        
        return leaderboard;
    }
    
    public Integer getUserBestTime(int userId, String trackId) throws SQLException {
        String sql = "SELECT MIN(lap_time_ms) as best_time " +
                     "FROM lap_times " +
                     "WHERE user_id = ? AND track_id = ? AND is_valid = 1";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, trackId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                int bestTime = rs.getInt("best_time");
                return rs.wasNull() ? null : bestTime;
            }
        }
        
        return null;
    }
}
```

---

## Migration Strategy

### Migration Files

```
migrations/
├── 001_initial_schema.sql      # 초기 스키마 생성
├── 002_seed_data.sql           # 기본 데이터 삽입
├── 003_add_achievements.sql    # 도전과제 추가
└── 004_add_multiplayer.sql     # 멀티플레이어 테이블 추가
```

### Migration Tracking Table

```sql
CREATE TABLE schema_migrations (
    version INTEGER PRIMARY KEY,
    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Migration Runner

```java
public class MigrationManager {
    private Connection connection;
    
    public void runMigrations() throws SQLException {
        int currentVersion = getCurrentSchemaVersion();
        int latestVersion = getLatestMigrationVersion();
        
        for (int v = currentVersion + 1; v <= latestVersion; v++) {
            executeMigration(v);
            recordMigration(v);
        }
    }
    
    private int getCurrentSchemaVersion() throws SQLException {
        String sql = "SELECT MAX(version) FROM schema_migrations";
        // ...
    }
    
    private void executeMigration(int version) throws SQLException {
        String filename = String.format("migrations/%03d_*.sql", version);
        // ...
    }
}
```

---

## Backup & Recovery

### Automated Backups

```java
public class BackupManager {
    private static final String BACKUP_DIR = "data/backups/";
    
    public void createBackup() {
        try {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String backupFile = BACKUP_DIR + "game_" + timestamp + ".db";
            
            Files.copy(
                Paths.get("data/game.db"),
                Paths.get(backupFile),
                StandardCopyOption.REPLACE_EXISTING
            );
            
            cleanOldBackups(); // Keep last 7 backups
            
        } catch (IOException e) {
            Gdx.app.error("BackupManager", "Backup failed", e);
        }
    }
    
    private void cleanOldBackups() throws IOException {
        // Keep only last 7 backups
        Files.list(Paths.get(BACKUP_DIR))
            .sorted(Comparator.reverseOrder())
            .skip(7)
            .forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    // Log error
                }
            });
    }
}
```

---

## Testing

### Database Tests

```java
@Test
public void testUserCreation() throws SQLException {
    UserDAO userDAO = new UserDAO(connection);
    
    User user = userDAO.createUser("testuser", "password123", "test@example.com");
    
    assertThat(user).isNotNull();
    assertThat(user.getUsername()).isEqualTo("testuser");
    assertThat(user.getUserId()).isGreaterThan(0);
}

@Test
public void testLapTimeSave() throws SQLException {
    LapTimeDAO lapTimeDAO = new LapTimeDAO(connection);
    
    lapTimeDAO.saveLapTime(1, "monaco", 1, 90000, "soft");
    
    Integer bestTime = lapTimeDAO.getUserBestTime(1, "monaco");
    assertThat(bestTime).isEqualTo(90000);
}
```

---

## Performance Considerations

### Indexing Strategy
- All foreign keys have indexes
- Leaderboard queries optimized with composite index
- User lookup by username indexed (login performance)

### Connection Pooling
- Single connection for SQLite (file-based, no pooling needed)
- Use write-ahead logging (WAL) mode for better concurrency

### Query Optimization
- Use prepared statements (prevents SQL injection + faster)
- Batch inserts for bulk operations
- PRAGMA optimizations:

```sql
PRAGMA journal_mode = WAL;
PRAGMA synchronous = NORMAL;
PRAGMA cache_size = 10000;
PRAGMA temp_store = MEMORY;
```

---

**Version**: 1.0.0  
**Status**: Ready for Implementation  
**Next Review**: After Phase 2 completion
