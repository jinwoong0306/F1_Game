# MULTIPLAYER-SYNC.md

## Overview
KryoNet 기반 멀티플레이어 실시간 동기화 시스템 명세서입니다. 클라이언트-서버 아키텍처와 예측/보간 알고리즘을 정의합니다.

**Owner**: Network Lead  
**Status**: Draft  
**Last Updated**: 2025-01-15  
**Related Specs**: `LOBBY-SYSTEM.md`, `VEHICLE-PHYSICS.md`, `PITSTOP-MINIGAME.md`

---

## 1. Architecture Overview (아키텍처 개요)

### 1.1 Client-Server Model

```
┌─────────────┐      ┌─────────────┐      ┌─────────────┐
│  Client 1   │◄────►│   Server    │◄────►│  Client 2   │
│ (Player 1)  │      │(Authoritative│      │ (Player 2)  │
└─────────────┘      └─────────────┘      └─────────────┘
       ▲                    ▲                     ▲
       │                    │                     │
       └────────────────────┴─────────────────────┘
                    TCP/UDP (KryoNet)
```

**핵심 원칙**:
- **서버가 권위를 가짐**: 모든 게임 상태는 서버가 결정
- **클라이언트 예측**: 입력 즉시 로컬에서 시뮬레이션
- **서버 조정**: 서버 업데이트로 클라이언트 보정
- **보간**: 다른 플레이어 위치를 부드럽게 표시

---

## 2. Network Protocol (네트워크 프로토콜)

### 2.1 패킷 정의

```java
// 모든 패킷은 KryoNet으로 자동 직렬화
public class NetworkPackets {
    
    // 연결 관리
    public static class PlayerJoinPacket {
        public String username;
        public int vehicleId;
    }
    
    public static class PlayerJoinedPacket {
        public int assignedPlayerId;
        public String username;
        public int vehicleId;
        public float startX, startY;
        public float startRotation;
    }
    
    public static class PlayerLeftPacket {
        public int playerId;
        public String reason; // "disconnect", "quit", "timeout"
    }
    
    // 입력 동기화 (Client → Server)
    public static class PlayerInputPacket {
        public int playerId;
        public long timestamp;        // 클라이언트 타임스탬프
        public float acceleration;    // -1.0 ~ 1.0
        public float steering;        // -1.0 ~ 1.0
        public boolean braking;
        public int sequenceNumber;    // 입력 순서
    }
    
    // 상태 동기화 (Server → Clients)
    public static class GameStatePacket {
        public long serverTimestamp;
        public PlayerState[] playerStates;
    }
    
    public static class PlayerState {
        public int playerId;
        public float x, y;
        public float rotation;
        public float velocityX, velocityY;
        public float angularVelocity;
        public int currentLap;
        public float lapTime;
        public TireType currentTire;
        public float tireCondition;
        public float vehicleDurability;
        public boolean isInPit;
    }
    
    // 레이스 이벤트
    public static class RaceStartPacket {
        public long startTime;        // 동기화된 시작 시간
        public int countdownSeconds;  // 5, 4, 3, 2, 1, GO!
    }
    
    public static class LapCompletePacket {
        public int playerId;
        public int lapNumber;
        public float lapTime;
        public int currentPosition;   // 순위
    }
    
    public static class RaceEndPacket {
        public PlayerResult[] results;
    }
    
    public static class PlayerResult {
        public int playerId;
        public String username;
        public int finalPosition;
        public float totalTime;
        public float bestLapTime;
    }
    
    // 피트 스톱
    public static class PitStopPacket {
        public int playerId;
        public PitStopState state;    // ENTERING, SERVICING, EXITING
        public PitStopResult result;  // PERFECT, GOOD, BAD
        public TireType newTire;
        public float remainingTime;
    }
}
```

### 2.2 패킷 등록 (Kryo)

```java
public class PacketRegistry {
    public static void register(Kryo kryo) {
        // Connection packets
        kryo.register(PlayerJoinPacket.class);
        kryo.register(PlayerJoinedPacket.class);
        kryo.register(PlayerLeftPacket.class);
        
        // Input packets
        kryo.register(PlayerInputPacket.class);
        
        // State packets
        kryo.register(GameStatePacket.class);
        kryo.register(PlayerState.class);
        kryo.register(PlayerState[].class);
        
        // Race events
        kryo.register(RaceStartPacket.class);
        kryo.register(LapCompletePacket.class);
        kryo.register(RaceEndPacket.class);
        kryo.register(PlayerResult.class);
        kryo.register(PlayerResult[].class);
        
        // Pit stop
        kryo.register(PitStopPacket.class);
        
        // Enums
        kryo.register(TireType.class);
        kryo.register(PitStopState.class);
        kryo.register(PitStopResult.class);
    }
}
```

---

## 3. Server Implementation (서버 구현)

### 3.1 GameServer Class

```java
public class GameServer {
    private Server server;
    private Map<Integer, PlayerState> players;
    private Map<Integer, VehicleController> vehicleControllers;
    private World physicsWorld;
    
    private static final int TCP_PORT = 54555;
    private static final int UDP_PORT = 54777;
    private static final float TICK_RATE = 20f; // 20 Hz (50ms)
    private static final float TIME_STEP = 1/60f;
    
    private float accumulator = 0f;
    private long gameStartTime;
    
    public GameServer() {
        server = new Server(16384, 8192); // write/read buffer sizes
        PacketRegistry.register(server.getKryo());
        
        server.addListener(new ServerListener());
        
        players = new ConcurrentHashMap<>();
        vehicleControllers = new ConcurrentHashMap<>();
        
        // Box2D 물리 월드
        physicsWorld = new World(new Vector2(0, 0), true);
    }
    
    public void start() throws IOException {
        server.bind(TCP_PORT, UDP_PORT);
        server.start();
        
        System.out.println("Server started on ports " + TCP_PORT + "/" + UDP_PORT);
        
        // 게임 루프 시작
        new Thread(this::gameLoop).start();
    }
    
    private void gameLoop() {
        long lastTime = System.nanoTime();
        
        while (true) {
            long currentTime = System.nanoTime();
            float delta = (currentTime - lastTime) / 1_000_000_000f;
            lastTime = currentTime;
            
            accumulator += Math.min(delta, 0.25f);
            
            // 고정 타임스텝 물리 업데이트
            while (accumulator >= TIME_STEP) {
                updatePhysics(TIME_STEP);
                accumulator -= TIME_STEP;
            }
            
            // 상태 브로드캐스트 (20 Hz)
            if (System.currentTimeMillis() % 50 < 17) { // ~50ms 간격
                broadcastGameState();
            }
            
            try {
                Thread.sleep(1); // CPU 점유율 낮춤
            } catch (InterruptedException e) {
                break;
            }
        }
    }
    
    private void updatePhysics(float delta) {
        // 모든 플레이어 차량 업데이트
        for (Map.Entry<Integer, VehicleController> entry : vehicleControllers.entrySet()) {
            VehicleController vehicle = entry.getValue();
            vehicle.update(delta);
        }
        
        // Box2D 스텝
        physicsWorld.step(TIME_STEP, 8, 3);
        
        // 상태 동기화
        for (Map.Entry<Integer, VehicleController> entry : vehicleControllers.entrySet()) {
            int playerId = entry.getKey();
            VehicleController vehicle = entry.getValue();
            PlayerState state = players.get(playerId);
            
            // 물리 상태 → PlayerState 복사
            Vector2 pos = vehicle.getPosition();
            state.x = pos.x;
            state.y = pos.y;
            state.rotation = vehicle.getRotation();
            
            Vector2 vel = vehicle.getVelocity();
            state.velocityX = vel.x;
            state.velocityY = vel.y;
            state.angularVelocity = vehicle.getAngularVelocity();
        }
    }
    
    private void broadcastGameState() {
        GameStatePacket packet = new GameStatePacket();
        packet.serverTimestamp = System.currentTimeMillis();
        packet.playerStates = players.values().toArray(new PlayerState[0]);
        
        server.sendToAllUDP(packet);
    }
    
    private class ServerListener extends Listener {
        @Override
        public void connected(Connection connection) {
            System.out.println("Client connected: " + connection.getID());
        }
        
        @Override
        public void received(Connection connection, Object object) {
            if (object instanceof PlayerJoinPacket) {
                handlePlayerJoin(connection, (PlayerJoinPacket) object);
            }
            else if (object instanceof PlayerInputPacket) {
                handlePlayerInput(connection, (PlayerInputPacket) object);
            }
            else if (object instanceof PitStopPacket) {
                handlePitStop(connection, (PitStopPacket) object);
            }
        }
        
        @Override
        public void disconnected(Connection connection) {
            int playerId = connection.getID();
            handlePlayerLeft(playerId, "disconnect");
        }
    }
    
    private void handlePlayerJoin(Connection connection, PlayerJoinPacket packet) {
        int playerId = connection.getID();
        
        // 차량 생성
        VehicleController vehicle = new VehicleController(physicsWorld);
        vehicle.setPosition(getSpawnPosition(players.size()));
        vehicleControllers.put(playerId, vehicle);
        
        // 플레이어 상태 생성
        PlayerState state = new PlayerState();
        state.playerId = playerId;
        state.currentLap = 0;
        state.lapTime = 0f;
        players.put(playerId, state);
        
        // 클라이언트에 확인 패킷 전송
        PlayerJoinedPacket response = new PlayerJoinedPacket();
        response.assignedPlayerId = playerId;
        response.username = packet.username;
        response.vehicleId = packet.vehicleId;
        Vector2 pos = vehicle.getPosition();
        response.startX = pos.x;
        response.startY = pos.y;
        response.startRotation = vehicle.getRotation();
        
        connection.sendTCP(response);
        
        // 다른 클라이언트들에게 알림
        server.sendToAllExceptTCP(connection.getID(), response);
    }
    
    private void handlePlayerInput(Connection connection, PlayerInputPacket packet) {
        int playerId = packet.playerId;
        VehicleController vehicle = vehicleControllers.get(playerId);
        
        if (vehicle != null) {
            vehicle.setInput(packet.acceleration, packet.steering, packet.braking);
        }
    }
    
    private void handlePitStop(Connection connection, PitStopPacket packet) {
        // 피트 스톱 처리 (타이어 교체, 수리)
        PlayerState state = players.get(packet.playerId);
        if (state != null) {
            state.currentTire = packet.newTire;
            state.tireCondition = 1.0f;
            state.vehicleDurability = 1.0f;
            state.isInPit = true;
        }
        
        // 다른 클라이언트들에게 브로드캐스트
        server.sendToAllExceptUDP(connection.getID(), packet);
    }
}
```

---

## 4. Client Implementation (클라이언트 구현)

### 4.1 GameClient Class

```java
public class GameClient {
    private Client client;
    private int localPlayerId;
    private VehicleController localVehicle;
    
    private Map<Integer, RemotePlayer> remotePlayers;
    private List<PlayerInputPacket> inputHistory;
    
    private static final int MAX_INPUT_HISTORY = 120; // 2초 (60 FPS)
    
    public GameClient() {
        client = new Client(16384, 8192);
        PacketRegistry.register(client.getKryo());
        
        client.addListener(new ClientListener());
        
        remotePlayers = new ConcurrentHashMap<>();
        inputHistory = new ArrayList<>();
    }
    
    public void connect(String serverIP) throws IOException {
        client.connect(5000, serverIP, TCP_PORT, UDP_PORT);
        
        // 참가 요청
        PlayerJoinPacket joinPacket = new PlayerJoinPacket();
        joinPacket.username = GameSession.getCurrentUser().getUsername();
        joinPacket.vehicleId = GameSession.getSelectedVehicle().getId();
        
        client.sendTCP(joinPacket);
    }
    
    public void update(float delta) {
        // 로컬 플레이어 입력 처리
        handleLocalInput(delta);
        
        // 로컬 차량 예측 시뮬레이션
        localVehicle.update(delta);
        
        // 원격 플레이어 보간
        for (RemotePlayer remote : remotePlayers.values()) {
            remote.interpolate(delta);
        }
    }
    
    private void handleLocalInput(float delta) {
        // 입력 수집
        float acceleration = getAccelerationInput();
        float steering = getSteeringInput();
        boolean braking = isBrakingInput();
        
        // 로컬에 즉시 적용 (클라이언트 예측)
        localVehicle.setInput(acceleration, steering, braking);
        
        // 서버로 전송
        PlayerInputPacket inputPacket = new PlayerInputPacket();
        inputPacket.playerId = localPlayerId;
        inputPacket.timestamp = System.currentTimeMillis();
        inputPacket.acceleration = acceleration;
        inputPacket.steering = steering;
        inputPacket.braking = braking;
        inputPacket.sequenceNumber = inputHistory.size();
        
        client.sendUDP(inputPacket);
        
        // 히스토리 저장 (서버 조정용)
        inputHistory.add(inputPacket);
        if (inputHistory.size() > MAX_INPUT_HISTORY) {
            inputHistory.remove(0);
        }
    }
    
    private class ClientListener extends Listener {
        @Override
        public void received(Connection connection, Object object) {
            if (object instanceof PlayerJoinedPacket) {
                handlePlayerJoined((PlayerJoinedPacket) object);
            }
            else if (object instanceof GameStatePacket) {
                handleGameState((GameStatePacket) object);
            }
            else if (object instanceof LapCompletePacket) {
                handleLapComplete((LapCompletePacket) object);
            }
            else if (object instanceof PitStopPacket) {
                handlePitStop((PitStopPacket) object);
            }
        }
    }
    
    private void handlePlayerJoined(PlayerJoinedPacket packet) {
        if (packet.assignedPlayerId == client.getID()) {
            // 로컬 플레이어 설정
            localPlayerId = packet.assignedPlayerId;
            localVehicle = new VehicleController(localPhysicsWorld);
            localVehicle.setPosition(packet.startX, packet.startY);
            localVehicle.setRotation(packet.startRotation);
        } else {
            // 원격 플레이어 추가
            RemotePlayer remote = new RemotePlayer(packet);
            remotePlayers.put(packet.assignedPlayerId, remote);
        }
    }
    
    private void handleGameState(GameStatePacket packet) {
        for (PlayerState state : packet.playerStates) {
            if (state.playerId == localPlayerId) {
                // 로컬 플레이어 서버 조정
                reconcileLocalPlayer(state);
            } else {
                // 원격 플레이어 업데이트
                RemotePlayer remote = remotePlayers.get(state.playerId);
                if (remote != null) {
                    remote.updateFromServer(state);
                }
            }
        }
    }
    
    private void reconcileLocalPlayer(PlayerState serverState) {
        Vector2 serverPos = new Vector2(serverState.x, serverState.y);
        Vector2 localPos = localVehicle.getPosition();
        
        float distance = serverPos.dst(localPos);
        
        // 오차가 큰 경우에만 보정
        if (distance > 0.5f) { // 0.5미터 이상 차이
            // 서버 위치로 스냅
            localVehicle.setPosition(serverState.x, serverState.y);
            localVehicle.setRotation(serverState.rotation);
            
            // 예측 히스토리 재적용 (서버 이후의 입력들)
            // (고급 구현: 서버 타임스탬프 이후 입력 재시뮬레이션)
        } else {
            // 작은 오차는 부드럽게 보정
            localVehicle.smoothCorrect(serverPos, 0.1f);
        }
    }
}
```

### 4.2 RemotePlayer Class (원격 플레이어 보간)

```java
public class RemotePlayer {
    private int playerId;
    private String username;
    
    // 보간용 상태 버퍼
    private Queue<PlayerState> stateBuffer;
    private static final int BUFFER_SIZE = 3; // 150ms 버퍼 (20 Hz)
    
    // 현재 렌더링 상태
    private Vector2 currentPosition;
    private float currentRotation;
    
    // 보간 대상
    private PlayerState targetState;
    private float interpolationAlpha = 0f;
    
    public RemotePlayer(PlayerJoinedPacket packet) {
        this.playerId = packet.assignedPlayerId;
        this.username = packet.username;
        
        stateBuffer = new LinkedList<>();
        currentPosition = new Vector2(packet.startX, packet.startY);
        currentRotation = packet.startRotation;
    }
    
    public void updateFromServer(PlayerState state) {
        stateBuffer.add(state);
        
        // 버퍼 크기 제한
        while (stateBuffer.size() > BUFFER_SIZE) {
            stateBuffer.poll();
        }
    }
    
    public void interpolate(float delta) {
        if (stateBuffer.isEmpty()) {
            // 예측 (마지막 속도로 외삽)
            if (targetState != null) {
                currentPosition.x += targetState.velocityX * delta;
                currentPosition.y += targetState.velocityY * delta;
                currentRotation += targetState.angularVelocity * delta;
            }
            return;
        }
        
        // 다음 상태로 보간
        if (targetState == null || interpolationAlpha >= 1.0f) {
            targetState = stateBuffer.poll();
            interpolationAlpha = 0f;
        }
        
        if (targetState != null) {
            // 선형 보간
            float speed = 5.0f; // 보간 속도
            interpolationAlpha += delta * speed;
            interpolationAlpha = Math.min(interpolationAlpha, 1.0f);
            
            // 위치 보간
            currentPosition.x = MathUtils.lerp(
                currentPosition.x,
                targetState.x,
                interpolationAlpha
            );
            currentPosition.y = MathUtils.lerp(
                currentPosition.y,
                targetState.y,
                interpolationAlpha
            );
            
            // 회전 보간 (각도)
            currentRotation = MathUtils.lerpAngleDeg(
                currentRotation,
                targetState.rotation,
                interpolationAlpha
            );
        }
    }
    
    public Vector2 getPosition() {
        return currentPosition;
    }
    
    public float getRotation() {
        return currentRotation;
    }
}
```

---

## 5. Lag Compensation (지연 보상)

### 5.1 클라이언트 예측 (Client-Side Prediction)

```
Client Input → Local Simulation (즉시) → Display
       ↓
   Send to Server
       ↓
Server Receives → Simulate → Send Correction
       ↓
Client Receives → Reconcile (오차 보정)
```

**장점**: 입력 즉시 반응, 지연 느껴지지 않음  
**단점**: 서버 조정 시 순간적인 "점프" 가능

### 5.2 서버 리와인드 (Server Rewinding)

충돌 감지 시 클라이언트의 지연을 고려하여 과거 상태로 되돌려 판정:

```java
public class ServerRewinding {
    private Map<Integer, Queue<PlayerState>> playerHistory;
    
    public boolean checkCollision(int attackerId, int victimId, long clientTimestamp) {
        // 공격자의 클라이언트 시간에 해당하는 피해자 상태 찾기
        Queue<PlayerState> victimHistory = playerHistory.get(victimId);
        
        PlayerState victimAtAttackTime = findStateAtTime(victimHistory, clientTimestamp);
        
        // 해당 시점의 위치에서 충돌 판정
        return isColliding(attackerPosition, victimAtAttackTime.position);
    }
}
```

---

## 6. Performance Optimization (성능 최적화)

### 6.1 대역폭 최적화

```java
// Delta Compression: 변화가 있는 값만 전송
public class CompressedPlayerState {
    public int playerId;
    public byte flags; // 비트 플래그: 어떤 필드가 변경되었는지
    
    // flags & 0x01: 위치 변경
    public Float x, y; // nullable
    
    // flags & 0x02: 회전 변경
    public Float rotation;
    
    // flags & 0x04: 속도 변경
    public Float velocityX, velocityY;
    
    // flags & 0x08: 랩 변경
    public Integer currentLap;
}
```

### 6.2 네트워크 통계

```java
public class NetworkStats {
    private long bytesSent;
    private long bytesReceived;
    private int packetsSent;
    private int packetsReceived;
    private float averagePing;
    
    public void update(long rtt) {
        averagePing = averagePing * 0.9f + rtt * 0.1f; // 이동 평균
    }
    
    public float getBandwidthUsage() {
        return (bytesSent + bytesReceived) / 1024f; // KB/s
    }
}
```

---

## 7. Testing & Validation

### 7.1 Network Simulation

```java
// 지연/패킷 손실 시뮬레이션
public class NetworkSimulator {
    private int simulatedLatency = 100; // ms
    private float packetLossRate = 0.05f; // 5%
    
    public void send(Object packet) {
        if (Math.random() < packetLossRate) {
            return; // 패킷 손실
        }
        
        // 지연 시뮬레이션
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                client.sendUDP(packet);
            }
        }, simulatedLatency);
    }
}
```

### 7.2 동기화 테스트

```java
@Test
@DisplayName("Client prediction should match server after reconciliation")
public void testClientPrediction() {
    // Setup
    GameServer server = new GameServer();
    GameClient client = new GameClient();
    
    // Simulate input
    client.sendInput(1.0f, 0.0f, false); // Full throttle
    
    // Wait for server update
    Thread.sleep(100);
    
    // Check positions match within tolerance
    Vector2 clientPos = client.getLocalVehicle().getPosition();
    Vector2 serverPos = server.getPlayerState(client.getId()).getPosition();
    
    assertThat(clientPos.dst(serverPos)).isLessThan(0.1f);
}
```

---

## 8. Security Considerations (보안 고려사항)

### 8.1 치트 방지

```java
// 서버 측 유효성 검사
public boolean validatePlayerInput(PlayerInputPacket input) {
    // 입력 범위 검증
    if (Math.abs(input.acceleration) > 1.0f) return false;
    if (Math.abs(input.steering) > 1.0f) return false;
    
    // 타임스탬프 검증 (시간 조작 방지)
    long serverTime = System.currentTimeMillis();
    if (Math.abs(input.timestamp - serverTime) > 5000) return false;
    
    // 속도 제한 (스피드핵 감지)
    float speed = getPlayerSpeed(input.playerId);
    if (speed > MAX_SPEED * 1.1f) return false;
    
    return true;
}
```

### 8.2 DDoS 방어

```java
// Rate Limiting
public class RateLimiter {
    private Map<InetAddress, Integer> requestCounts;
    private static final int MAX_REQUESTS_PER_SECOND = 100;
    
    public boolean allowRequest(InetAddress address) {
        int count = requestCounts.getOrDefault(address, 0);
        if (count > MAX_REQUESTS_PER_SECOND) {
            return false; // Block
        }
        requestCounts.put(address, count + 1);
        return true;
    }
}
```

---

**Version**: 1.0.0  
**Status**: Ready for Implementation  
**Next Review**: After Phase 4 completion
