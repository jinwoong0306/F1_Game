package com.mygame.f1.server;

import com.esotericsoftware.kryo.Kryo;
import com.mygame.f1.shared.Packets;

/** Registers all shared packet classes with Kryo/KryoNet. */
public final class PacketRegistry {
    private PacketRegistry() {}

    public static void register(Kryo kryo) {
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
