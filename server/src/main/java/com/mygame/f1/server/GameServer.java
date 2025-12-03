package com.mygame.f1.server;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.esotericsoftware.minlog.Log;
import com.mygame.f1.shared.Packets;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Minimal lobby server: create/join/leave rooms with max 4 players; broadcasts room state.
 * Physics/state-sync will be added later per docs/specs/network/MULTIPLAYER-SYNC.md.
 */
public class GameServer {
    private final int tcpPort;
    private final int udpPort;
    private final Server server;

    // roomId -> Room
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    // connectionId -> roomId (single membership for now)
    private final Map<Integer, String> membership = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public GameServer(int tcpPort, int udpPort) {
        this.tcpPort = tcpPort; this.udpPort = udpPort;
        // 과도한 로그로 인한 I/O 부담을 줄이기 위해 INFO로 설정
        Log.set(Log.LEVEL_INFO);
        this.server = new Server(16384, 8192);
        Kryo kryo = server.getKryo();
        PacketRegistry.register(kryo);
        server.addListener(new ServerListener());
        scheduler.scheduleAtFixedRate(this::broadcastStates, 50, 50, TimeUnit.MILLISECONDS); // 20Hz
    }

    public void start() throws IOException {
        server.bind(tcpPort, udpPort);
        server.start();
        System.out.printf("Server started TCP=%d UDP=%d%n", tcpPort, udpPort);
    }

    private class ServerListener extends Listener {
        @Override public void connected(Connection connection) {
            // Keep-Alive 설정: 8초마다 하트비트 전송
            connection.setKeepAliveTCP(8000);
            // Timeout 설정: 20초 동안 응답 없으면 연결 종료 (ghost player 방지)
            connection.setTimeout(20000);
            System.out.printf("connected: %d (keep-alive=8s, timeout=20s)%n", connection.getID());
        }

        @Override public void disconnected(Connection connection) {
            System.out.printf("disconnected: %d%n", connection.getID());
            leaveAll(connection);
        }

        @Override public void received(Connection c, Object object) {
            try {
                if (object instanceof Packets.CreateRoomRequest req) {
                    onCreateRoom(c, req);
                } else if (object instanceof Packets.JoinRoomRequest req) {
                    onJoinRoom(c, req);
            } else if (object instanceof Packets.LeaveRoomRequest req) {
                onLeaveRoom(c, req);
            } else if (object instanceof Packets.ReadyRequest req) {
                onReady(c, req);
            } else if (object instanceof Packets.SelectionRequest req) {
                onSelection(c, req);
            } else if (object instanceof Packets.RoomListRequest) {
                onRoomList(c);
            } else if (object instanceof Packets.StartRaceRequest req) {
                onStartRace(c, req);
            } else if (object instanceof Packets.ChatMessage msg) {
                onChat(c, msg);
            } else if (object instanceof Packets.PlayerStateUpdate upd) {
                onPlayerState(c, upd);
            } else if (object instanceof Packets.PlayerFinishedPacket pkt) {
                onPlayerFinished(c, pkt);
            }
        } catch (Exception e) {
            Packets.ErrorResponse err = new Packets.ErrorResponse();
            err.message = "server error: " + e.getMessage();
            c.sendTCP(err);
            }
        }
    }

    private void onCreateRoom(Connection c, Packets.CreateRoomRequest req) {
        // 단일 로비 유지: 이미 방이 있으면 새로 만들지 않고 첫 방에 합류
        // 방은 생성 요청마다 새로 만든다 (재사용 제거)
        int max = req.maxPlayers > 0 ? req.maxPlayers : 4;
        if (max > 4) max = 4; // hard cap per request
        String newRoomId = UUID.randomUUID().toString().substring(0, 8);
        Room room = new Room(newRoomId, Optional.ofNullable(req.roomName).orElse("Room"), max);
        rooms.put(newRoomId, room);
        System.out.printf("createRoom: %s by %s%n", room.id, req.username);

        Packets.PlayerInfo self = new Packets.PlayerInfo();
        self.playerId = c.getID();
        self.username = safeName(req.username);
        self.vehicleIndex = 0;
        room.join(c, self);
        membership.put(c.getID(), room.id);

        Packets.CreateRoomResponse res = new Packets.CreateRoomResponse();
        res.ok = true; res.roomId = room.id; res.message = "created"; res.self = self;
        c.sendTCP(res);
        broadcastRoomState(room);
    }

    private void onJoinRoom(Connection c, Packets.JoinRoomRequest req) {
        Room room = rooms.get(req.roomId);
        Packets.JoinRoomResponse res = new Packets.JoinRoomResponse();
        if (room == null) {
            res.ok = false; res.message = "room not found";
            c.sendTCP(res); return;
        }
        if (room.players.size() >= room.maxPlayers) {
            res.ok = false; res.message = "room full";
            c.sendTCP(res); return;
        }
        Packets.PlayerInfo self = new Packets.PlayerInfo();
        self.playerId = c.getID();
        self.username = safeName(req.username);
        self.vehicleIndex = 0;
        room.join(c, self);
        membership.put(c.getID(), room.id);

        res.ok = true; res.message = "joined"; res.self = self; res.state = room.toState();
        c.sendTCP(res);
        broadcastRoomState(room);
    }

    private void onLeaveRoom(Connection c, Packets.LeaveRoomRequest req) {
        Room room = rooms.get(req.roomId);
        if (room != null) {
            room.leave(c.getID());
            membership.remove(c.getID());
            broadcastRoomState(room);
            cleanupIfEmpty(room);
        }
    }

    private void onReady(Connection c, Packets.ReadyRequest req) {
        Room room = rooms.get(req.roomId);
        if (room == null) { sendError(c, "room not found"); return; }
        room.setReady(c.getID(), req.ready);
        broadcastRoomState(room);
    }

    private void onSelection(Connection c, Packets.SelectionRequest req) {
        Room room = rooms.get(req.roomId);
        if (room == null) { sendError(c, "selection denied: room not found"); return; }
        room.setSelection(c.getID(), req.trackIndex, req.vehicleIndex);
        broadcastRoomState(room);
    }

    private void onRoomList(Connection c) {
        Packets.RoomListResponse res = new Packets.RoomListResponse();
        res.rooms = new ArrayList<>();
        for (Room r : rooms.values()) {
            res.rooms.add(r.toState());
        }
        c.sendTCP(res);
    }

    private void onStartRace(Connection c, Packets.StartRaceRequest req) {
        Room room = rooms.get(req.roomId);
        if (room == null) {
            sendError(c, "start denied: room not found");
            return;
        }
        if (room.phase != Packets.RoomPhase.WAITING) {
            sendError(c, "start denied: not in WAITING phase");
            return;
        }
        if (!room.isHost(c.getID())) {
            sendError(c, "start denied: only host can start");
            return;
        }
        if (room.players.size() < 2) {
            sendError(c, "start denied: need at least 2 players");
            return;
        }
        if (!room.allReady()) {
            sendError(c, "start denied: not all players ready");
            return;
        }
        int seconds = req.countdownSeconds <= 0 ? 5 : Math.min(req.countdownSeconds, 10);
        // 트랙 인덱스 확정: 요청값이 유효하면 설정, 아니면 room의 선택 사용
        int trackIdx = req.trackIndex >= 0 ? req.trackIndex : room.selectedTrackIndex;
        room.selectedTrackIndex = trackIdx;
        room.phase = Packets.RoomPhase.COUNTDOWN;
        Packets.RaceStartPacket start = new Packets.RaceStartPacket();
        start.countdownSeconds = seconds;
        start.startTimeMillis = System.currentTimeMillis() + seconds * 1000L;
        start.trackIndex = trackIdx;
        // 차량 인덱스/플레이어 ID 전달
        List<Integer> ids = room.order;
        start.playerIds = ids.stream().mapToInt(Integer::intValue).toArray();
        start.vehicleIndices = ids.stream().mapToInt(id -> {
            Packets.PlayerInfo p = room.players.get(id);
            return p != null ? p.vehicleIndex : 0;
        }).toArray();
        for (int connId : room.connectionIds()) {
            server.sendToTCP(connId, start);
        }
        broadcastRoomState(room);
    }

    private void onChat(Connection c, Packets.ChatMessage msg) {
        Room room = rooms.get(msg.roomId);
        if (room == null) { sendError(c, "chat denied: room not found"); return; }
        msg.sender = safeName(msg.sender);
        msg.ts = msg.ts == 0 ? System.currentTimeMillis() : msg.ts;
        for (int connId : room.connectionIds()) {
            server.sendToTCP(connId, msg);
        }
    }

    private void onPlayerState(Connection c, Packets.PlayerStateUpdate upd) {
        Room room = rooms.get(upd.roomId);
        if (room == null) return;
        Packets.PlayerState state = upd.state;
        if (state == null) return;
        state.playerId = c.getID();
        room.latestStates.put(c.getID(), state);
    }

    private void broadcastStates() {
        for (Room room : rooms.values()) {
            if (room.latestStates.isEmpty()) continue;
            applySimpleCollisions(room);
            Packets.GameStatePacket gs = new Packets.GameStatePacket();
            gs.serverTimestamp = System.currentTimeMillis();
            Collection<Packets.PlayerState> values = room.latestStates.values();
            gs.playerStates = values.toArray(new Packets.PlayerState[0]);
            for (int connId : room.connectionIds()) {
                server.sendToUDP(connId, gs);
            }
        }
    }

    // 단순 원형 충돌 처리: 서로 붙어 있으면 속도 감쇠 + 겹침 해소
    private void applySimpleCollisions(Room room) {
        final float radius = 0.19f; // 대략 차량 반경(로컬 렌더 크기 19px/PPM=0.19m)
        final float minDist = radius * 2f;
        final float minDistSq = minDist * minDist;
        final float damping = 0.3f; // 속도 감쇠
        final float pushScale = 0.5f; // 겹침 깊이의 절반만큼 밀어내기
        List<Packets.PlayerState> states = new ArrayList<>(room.latestStates.values());
        int n = states.size();
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                Packets.PlayerState a = states.get(i);
                Packets.PlayerState b = states.get(j);
                float dx = a.x - b.x;
                float dy = a.y - b.y;
                float distSq = dx * dx + dy * dy;
                if (distSq < minDistSq) {
                    // 속도 감쇠
                    a.velocityX *= damping; a.velocityY *= damping;
                    b.velocityX *= damping; b.velocityY *= damping;
                    // 겹침 해소 및 반발
                    float nx = dx, ny = dy;
                    float len = (float)Math.sqrt(distSq);
                    if (len > 0.0001f) {
                        nx /= len; ny /= len;
                        float penetration = minDist - len;
                        float move = penetration * pushScale;
                        a.x += nx * move; a.y += ny * move;
                        b.x -= nx * move; b.y -= ny * move;
                    }
                    room.latestStates.put(a.playerId, a);
                    room.latestStates.put(b.playerId, b);
                }
            }
        }
    }

    private void sendError(Connection c, String message) {
        Packets.ErrorResponse err = new Packets.ErrorResponse();
        err.message = message;
        c.sendTCP(err);
    }

    private void leaveAll(Connection c) {
        String roomId = membership.remove(c.getID());
        if (roomId != null) {
            Room room = rooms.get(roomId);
            if (room != null) {
                room.leave(c.getID());
                broadcastRoomState(room);
                cleanupIfEmpty(room);
            }
        }
    }

    private void broadcastRoomState(Room room) {
        Packets.RoomStatePacket pkt = new Packets.RoomStatePacket();
        pkt.state = room.toState();
        for (int connId : room.connectionIds()) {
            server.sendToTCP(connId, pkt);
        }
    }

    private void cleanupIfEmpty(Room room) {
        if (room.players.isEmpty()) {
            rooms.remove(room.id);
            System.out.printf("cleanup room: %s%n", room.id);
        }
    }

    private static String safeName(String s) {
        String n = (s == null || s.isBlank()) ? "Player" : s.trim();
        return n.length() > 24 ? n.substring(0, 24) : n;
    }

    private void onPlayerFinished(Connection c, Packets.PlayerFinishedPacket pkt) {
        String roomId = membership.get(c.getID());
        if (roomId == null || !roomId.equals(pkt.roomId)) {
            sendError(c, "not in room");
            return;
        }
        Room room = rooms.get(roomId);
        if (room == null) return;

        synchronized (room) {
            // 완주 데이터 저장
            room.finishedPlayers.put(pkt.playerId, pkt);
            System.out.printf("[Room %s] Player %d finished with time %.2fs%n", roomId, pkt.playerId, pkt.totalTime);

            // 첫 번째 완주자인 경우 카운트다운 시작
            if (room.finishedPlayers.size() == 1) {
                room.firstFinishTime = System.currentTimeMillis();
                room.countdownActive = true;

                // 첫 번째 완주자 정보 브로드캐스트
                Packets.PlayerInfo firstPlayer = room.players.get(pkt.playerId);
                Packets.CountdownStartPacket countdownPkt = new Packets.CountdownStartPacket();
                countdownPkt.firstPlacePlayerId = pkt.playerId;
                countdownPkt.firstPlaceUsername = firstPlayer != null ? firstPlayer.username : "Player";
                countdownPkt.firstPlaceTime = pkt.totalTime;
                countdownPkt.remainingSeconds = 10;

                for (int connId : room.connectionIds()) {
                    server.sendToTCP(connId, countdownPkt);
                }

                // 10초 후 결과 계산 및 전송 스케줄
                scheduler.schedule(() -> finalizeRace(room), 10, TimeUnit.SECONDS);

                // 카운트다운 업데이트 (1초마다)
                for (int i = 9; i >= 1; i--) {
                    final int remaining = i;
                    scheduler.schedule(() -> {
                        Packets.CountdownUpdatePacket updatePkt = new Packets.CountdownUpdatePacket();
                        updatePkt.remainingSeconds = remaining;
                        for (int connId : room.connectionIds()) {
                            server.sendToTCP(connId, updatePkt);
                        }
                    }, (10 - i), TimeUnit.SECONDS);
                }
            }
        }
    }

    private void finalizeRace(Room room) {
        synchronized (room) {
            // 순위 계산
            List<Packets.PlayerFinishedPacket> finishedList = new ArrayList<>(room.finishedPlayers.values());
            finishedList.sort(Comparator.comparingDouble(p -> p.totalTime));

            // 미완주자 목록
            List<Integer> failedIds = new ArrayList<>();
            for (Integer playerId : room.players.keySet()) {
                if (!room.finishedPlayers.containsKey(playerId)) {
                    failedIds.add(playerId);
                }
            }

            // 결과 패킷 생성
            Packets.RaceResultsPacket resultsPkt = new Packets.RaceResultsPacket();
            resultsPkt.results = new Packets.PlayerResult[finishedList.size() + failedIds.size()];

            // 완주자
            for (int i = 0; i < finishedList.size(); i++) {
                Packets.PlayerFinishedPacket fp = finishedList.get(i);
                Packets.PlayerInfo pInfo = room.players.get(fp.playerId);

                Packets.PlayerResult result = new Packets.PlayerResult();
                result.playerId = fp.playerId;
                result.username = pInfo != null ? pInfo.username : "Player";
                result.rank = i + 1;
                result.totalTime = fp.totalTime;
                result.lapTimes = fp.lapTimes;
                result.failed = false;

                resultsPkt.results[i] = result;
            }

            // 미완주자 (FAIL)
            for (int i = 0; i < failedIds.size(); i++) {
                int playerId = failedIds.get(i);
                Packets.PlayerInfo pInfo = room.players.get(playerId);

                Packets.PlayerResult result = new Packets.PlayerResult();
                result.playerId = playerId;
                result.username = pInfo != null ? pInfo.username : "Player";
                result.rank = 0; // FAIL은 순위 없음
                result.totalTime = 0;
                result.lapTimes = new float[0];
                result.failed = true;

                resultsPkt.results[finishedList.size() + i] = result;
            }

            resultsPkt.failedPlayerIds = failedIds.stream().mapToInt(Integer::intValue).toArray();

            // 모든 플레이어에게 결과 전송
            for (int connId : room.connectionIds()) {
                server.sendToTCP(connId, resultsPkt);
            }

            // 방 상태를 FINISHED로 변경
            room.phase = Packets.RoomPhase.FINISHED;
            broadcastRoomState(room);

            System.out.printf("[Room %s] Race finalized. %d finished, %d failed%n",
                room.id, finishedList.size(), failedIds.size());
        }
    }

    private static final class Room {
        final String id; final String name; final int maxPlayers;
        final Map<Integer, Packets.PlayerInfo> players = new ConcurrentHashMap<>();
        final List<Integer> order = new CopyOnWriteArrayList<>();
        final Set<Integer> ready = Collections.synchronizedSet(new HashSet<>());
        Packets.RoomPhase phase = Packets.RoomPhase.WAITING;
        int selectedTrackIndex = 0;
        final Map<Integer, Packets.PlayerState> latestStates = new ConcurrentHashMap<>();

        // Race finish tracking
        final Map<Integer, Packets.PlayerFinishedPacket> finishedPlayers = new ConcurrentHashMap<>();
        long firstFinishTime = 0;
        boolean countdownActive = false;

        Room(String id, String name, int maxPlayers) {
            this.id = id; this.name = name; this.maxPlayers = maxPlayers;
        }

        synchronized void join(Connection c, Packets.PlayerInfo info) {
            players.put(c.getID(), info);
            if (!order.contains(c.getID())) order.add(c.getID());
        }

        synchronized void leave(int connectionId) {
            players.remove(connectionId);
            order.remove((Integer) connectionId);
            ready.remove(connectionId);
            latestStates.remove(connectionId);
        }

        List<Integer> connectionIds() { return new ArrayList<>(players.keySet()); }

        Packets.RoomState toState() {
            Packets.RoomState rs = new Packets.RoomState();
            rs.roomId = id; rs.roomName = name; rs.maxPlayers = maxPlayers; rs.phase = phase;
            rs.players = new ArrayList<>();
            for (Integer id : order) {
                Packets.PlayerInfo p = players.get(id);
                if (p != null) {
                    Packets.PlayerInfo copy = new Packets.PlayerInfo();
                    copy.playerId = p.playerId;
                    copy.username = p.username;
                    copy.ready = ready.contains(id);
                    copy.vehicleIndex = p.vehicleIndex;
                    rs.players.add(copy);
                }
            }
            rs.selectedTrackIndex = selectedTrackIndex;
            return rs;
        }

        synchronized void setReady(int connectionId, boolean value) {
            if (!players.containsKey(connectionId)) return;
            if (value) ready.add(connectionId); else ready.remove(connectionId);
        }

        synchronized void setSelection(int connectionId, int trackIndex, int vehicleIndex) {
            Packets.PlayerInfo p = players.get(connectionId);
            if (p == null) return;
            if (vehicleIndex >= 0) p.vehicleIndex = vehicleIndex;
            boolean isHost = !order.isEmpty() && order.get(0) == connectionId;
            if (isHost && trackIndex >= 0) {
                selectedTrackIndex = trackIndex;
            }
        }

        synchronized boolean allReady() {
            return !players.isEmpty() && ready.containsAll(players.keySet());
        }

        synchronized boolean isHost(int connectionId) {
            return !order.isEmpty() && order.get(0) == connectionId;
        }
    }
}
