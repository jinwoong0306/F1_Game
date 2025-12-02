package com.mygame.f1.network;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Listener.ThreadedListener;
import com.mygame.f1.shared.Packets;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/** Minimal lobby client for room create/join/ready/start. */
public class LobbyClient {
    private final Client client;

    private Consumer<List<Packets.RoomState>> roomListHandler;
    private Consumer<Packets.RoomState> roomStateHandler;
    private Consumer<Packets.RaceStartPacket> raceStartHandler;
    private Consumer<String> errorHandler;
    private Consumer<Packets.ChatMessage> chatHandler;
    private Consumer<Packets.GameStatePacket> gameStateHandler;

    private CompletableFuture<Packets.CreateRoomResponse> createFuture;
    private CompletableFuture<Packets.JoinRoomResponse> joinFuture;

    public LobbyClient() {
        this.client = new Client(16384, 8192);
        com.esotericsoftware.minlog.Log.set(com.esotericsoftware.minlog.Log.LEVEL_INFO); // 디버그 로그 최소화
        register(client.getKryo());
        client.addListener(new ThreadedListener(new ClientListener()));
    }

    public void start() { client.start(); }

    public void connect(String host, int tcpPort, int udpPort, int timeoutMs) throws IOException {
        client.connect(timeoutMs, host, tcpPort, udpPort);
    }

    public boolean isConnected() { return client.isConnected(); }

    public void close() { client.stop(); }

    public CompletableFuture<Packets.CreateRoomResponse> createRoom(String roomName, String username, int maxPlayers) {
        Packets.CreateRoomRequest req = new Packets.CreateRoomRequest();
        req.roomName = roomName; req.username = username; req.maxPlayers = maxPlayers;
        createFuture = new CompletableFuture<>();
        client.sendTCP(req);
        return createFuture;
    }

    public CompletableFuture<Packets.JoinRoomResponse> joinRoom(String roomId, String username) {
        Packets.JoinRoomRequest req = new Packets.JoinRoomRequest();
        req.roomId = roomId; req.username = username;
        joinFuture = new CompletableFuture<>();
        client.sendTCP(req);
        return joinFuture;
    }

    public void leaveRoom(String roomId) {
        Packets.LeaveRoomRequest req = new Packets.LeaveRoomRequest();
        req.roomId = roomId;
        client.sendTCP(req);
    }

    public void requestRoomList() { client.sendTCP(new Packets.RoomListRequest()); }

    public void setReady(String roomId, boolean ready) {
        Packets.ReadyRequest req = new Packets.ReadyRequest();
        req.roomId = roomId; req.ready = ready;
        client.sendTCP(req);
    }

    public void startRace(String roomId, String hostName, int countdownSeconds) {
        startRace(roomId, hostName, countdownSeconds, 0, 0);
    }

    public void startRace(String roomId, String hostName, int countdownSeconds, int trackIndex) {
        startRace(roomId, hostName, countdownSeconds, trackIndex, 0);
    }

    public void startRace(String roomId, String hostName, int countdownSeconds, int trackIndex, int vehicleIndex) {
        Packets.StartRaceRequest req = new Packets.StartRaceRequest();
        req.roomId = roomId; req.hostName = hostName; req.countdownSeconds = countdownSeconds; req.trackIndex = trackIndex; req.vehicleIndex = vehicleIndex;
        client.sendTCP(req);
    }

    public void sendSelection(String roomId, int trackIdx, int vehicleIdx) {
        Packets.SelectionRequest req = new Packets.SelectionRequest();
        req.roomId = roomId; req.trackIndex = trackIdx; req.vehicleIndex = vehicleIdx;
        client.sendTCP(req);
    }

    public void sendChat(String roomId, String sender, String text) {
        Packets.ChatMessage msg = new Packets.ChatMessage();
        msg.roomId = roomId; msg.sender = sender; msg.text = text; msg.ts = System.currentTimeMillis();
        client.sendTCP(msg);
    }

    public void sendPlayerState(String roomId, Packets.PlayerState state) {
        Packets.PlayerStateUpdate upd = new Packets.PlayerStateUpdate();
        upd.roomId = roomId; upd.state = state;
        client.sendUDP(upd);
    }

    public void onRoomList(Consumer<List<Packets.RoomState>> handler) { this.roomListHandler = handler; }
    public void onRoomState(Consumer<Packets.RoomState> handler) { this.roomStateHandler = handler; }
    public void onRaceStart(Consumer<Packets.RaceStartPacket> handler) { this.raceStartHandler = handler; }
    public void onError(Consumer<String> handler) { this.errorHandler = handler; }
    public void onChat(Consumer<Packets.ChatMessage> handler) { this.chatHandler = handler; }
    public void onGameState(Consumer<Packets.GameStatePacket> handler) { this.gameStateHandler = handler; }

    private class ClientListener extends Listener {
        @Override public void received(Connection connection, Object object) {
            if (object instanceof Packets.CreateRoomResponse res) {
                if (createFuture != null && !createFuture.isDone()) createFuture.complete(res);
            } else if (object instanceof Packets.JoinRoomResponse res) {
                if (joinFuture != null && !joinFuture.isDone()) joinFuture.complete(res);
            } else if (object instanceof Packets.RoomListResponse res) {
                if (roomListHandler != null) roomListHandler.accept(res.rooms);
            } else if (object instanceof Packets.RoomStatePacket pkt) {
                if (roomStateHandler != null) roomStateHandler.accept(pkt.state);
            } else if (object instanceof Packets.RaceStartPacket pkt) {
                if (raceStartHandler != null) raceStartHandler.accept(pkt);
            } else if (object instanceof Packets.ChatMessage msg) {
                if (chatHandler != null) chatHandler.accept(msg);
            } else if (object instanceof Packets.GameStatePacket gs) {
                if (gameStateHandler != null) gameStateHandler.accept(gs);
            } else if (object instanceof Packets.ErrorResponse err) {
                if (errorHandler != null) errorHandler.accept(err.message);
            }
        }
    }

    private void register(Kryo kryo) {
        kryo.register(Packets.CreateRoomRequest.class);
        kryo.register(Packets.CreateRoomResponse.class);
        kryo.register(Packets.JoinRoomRequest.class);
        kryo.register(Packets.JoinRoomResponse.class);
        kryo.register(Packets.LeaveRoomRequest.class);
        kryo.register(Packets.ErrorResponse.class);
        kryo.register(Packets.RoomStatePacket.class);
        kryo.register(Packets.PlayerInputPacket.class);
        kryo.register(Packets.GameStatePacket.class);
        kryo.register(Packets.ReadyRequest.class);
        kryo.register(Packets.SelectionRequest.class);
        kryo.register(Packets.RoomListRequest.class);
        kryo.register(Packets.RoomListResponse.class);
        kryo.register(Packets.RaceStartPacket.class);
        kryo.register(Packets.StartRaceRequest.class);
        kryo.register(Packets.ChatMessage.class);
        kryo.register(Packets.PlayerStateUpdate.class);
        kryo.register(int[].class);
        kryo.register(Packets.PlayerInfo.class);
        kryo.register(Packets.PlayerState.class);
        kryo.register(Packets.RoomState.class);
        kryo.register(Packets.RoomPhase.class);
        kryo.register(java.util.ArrayList.class);
        kryo.register(Packets.PlayerState[].class);
    }
}
