package com.mygame.f1.shared;

import java.util.List;

/** Shared network packet definitions used by server and clients. */
public final class Packets {
    private Packets() {}

    // Requests / responses for lobby lifecycle
    public static class CreateRoomRequest { public String roomName; public String username; public int maxPlayers; }
    public static class CreateRoomResponse { public boolean ok; public String message; public String roomId; public PlayerInfo self; }

    public static class JoinRoomRequest { public String roomId; public String username; }
    public static class JoinRoomResponse { public boolean ok; public String message; public PlayerInfo self; public RoomState state; }

    public static class LeaveRoomRequest { public String roomId; }
    public static class ReadyRequest { public String roomId; public boolean ready; }
    /** 호스트/플레이어 선택 사항 전달 (맵/차량) */
    public static class SelectionRequest { public String roomId; public int trackIndex; public int vehicleIndex; }

    public static class RoomListRequest { }
    public static class RoomListResponse { public List<RoomState> rooms; }

    public static class StartRaceRequest { public String roomId; public int countdownSeconds; public String hostName; public int trackIndex; public int vehicleIndex; }
    public static class RaceStartPacket { public int countdownSeconds; public long startTimeMillis; public int trackIndex; public int[] playerIds; public int[] vehicleIndices; }
    /** 채팅 메시지 */
    public static class ChatMessage { public String roomId; public String sender; public String text; public long ts; }
    /** 실시간 상태 송신 */
    public static class PlayerStateUpdate { public String roomId; public PlayerState state; }

    public static class RoomStatePacket { public RoomState state; }
    public static class ErrorResponse { public String message; }

    // Race finish packets
    /** 플레이어가 레이스 완주 시 서버에 전송 */
    public static class PlayerFinishedPacket {
        public String roomId;
        public int playerId;
        public float totalTime;
        public float[] lapTimes;
    }

    /** 1등 완주 시 서버가 카운트다운 시작 알림 */
    public static class CountdownStartPacket {
        public int firstPlacePlayerId;
        public String firstPlaceUsername;
        public float firstPlaceTime;
        public int remainingSeconds; // 10초
    }

    /** 카운트다운 진행 중 남은 시간 알림 */
    public static class CountdownUpdatePacket {
        public int remainingSeconds;
    }

    /** 레이스 최종 결과 (순위 + FAIL 목록) */
    public static class RaceResultsPacket {
        public PlayerResult[] results; // 완주자 순위 (시간 순)
        public int[] failedPlayerIds; // 미완주자 (FAIL)
    }

    /** 개별 플레이어 결과 */
    public static class PlayerResult {
        public int playerId;
        public String username;
        public int rank; // 1, 2, 3...
        public float totalTime;
        public float[] lapTimes;
        public boolean failed; // true면 FAIL
    }

    // Game sync placeholders
    public static class PlayerInputPacket {
        public int playerId; public long timestamp; public float acceleration; public float steering; public boolean braking; public int sequenceNumber;
    }

    public static class GameStatePacket {
        public long serverTimestamp; public PlayerState[] playerStates;
    }

    // Data models
    public static class PlayerInfo { public int playerId; public String username; public boolean ready; public int vehicleIndex; }

    public static class PlayerState {
        public int playerId; public float x; public float y; public float rotation; public float velocityX; public float velocityY; public float angularVelocity; public int currentLap; public float lapTime; public int vehicleIndex;
    }

    public static class RoomState {
        public String roomId; public String roomName; public int maxPlayers; public RoomPhase phase; public List<PlayerInfo> players; public int selectedTrackIndex;
    }

    public enum RoomPhase { WAITING, COUNTDOWN, RUNNING, FINISHED }
}
