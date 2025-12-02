import React, { useState } from 'react';
import { ArrowLeft, Plus, LogIn, Users, Lock, Globe, User, Search, MapPin, Flag } from 'lucide-react';

interface MultiPlayerLobbyProps {
  onBack: () => void;
}

interface Room {
  id: number;
  name: string;
  host: string;
  players: number;
  maxPlayers: number;
  map: string;
  isPrivate: boolean;
  status: 'waiting' | 'playing';
  ping: number;
}

const mockRooms: Room[] = [
  { id: 1, name: 'NOOB FRIENDLY', host: 'SpeedRacer', players: 3, maxPlayers: 8, map: 'NEON CITY', isPrivate: false, status: 'waiting', ping: 25 },
  { id: 2, name: 'PRO ONLY', host: 'ProDriver99', players: 6, maxPlayers: 8, map: 'MOUNTAIN ROAD', isPrivate: false, status: 'waiting', ping: 42 },
  { id: 3, name: 'PRIVATE MATCH', host: 'CasualGamer', players: 2, maxPlayers: 4, map: 'DESERT TRACK', isPrivate: true, status: 'waiting', ping: 18 },
  { id: 4, name: 'RANKED MATCH', host: 'CompetitiveKing', players: 8, maxPlayers: 8, map: 'FUTURE HIGHWAY', isPrivate: false, status: 'playing', ping: 33 },
  { id: 5, name: 'NIGHT RACE', host: 'NightRider', players: 4, maxPlayers: 6, map: 'NEON CITY', isPrivate: false, status: 'waiting', ping: 56 },
  { id: 6, name: 'SPEED DEMONS', host: 'FastFury', players: 5, maxPlayers: 8, map: 'FUTURE HIGHWAY', isPrivate: false, status: 'waiting', ping: 29 },
];

export function MultiPlayerLobby({ onBack }: MultiPlayerLobbyProps) {
  const [showCreateRoom, setShowCreateRoom] = useState(false);
  const [roomName, setRoomName] = useState('');
  const [maxPlayers, setMaxPlayers] = useState(8);
  const [isPrivate, setIsPrivate] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');

  const filteredRooms = mockRooms.filter(room => 
    room.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
    room.host.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const handleCreateRoom = () => {
    alert(`방 생성됨!\n방 이름: ${roomName}\n최대 인원: ${maxPlayers}\n비공개: ${isPrivate ? '예' : '아니오'}`);
    setShowCreateRoom(false);
  };

  const handleJoinRoom = (room: Room) => {
    if (room.status === 'playing') {
      alert('진행 중인 게임에는 입장할 수 없습니다.');
      return;
    }
    if (room.players >= room.maxPlayers) {
      alert('방이 가득 찼습니다.');
      return;
    }
    alert(`${room.name} 방에 입장합니다!`);
  };

  const getPingColor = (ping: number) => {
    if (ping < 30) return 'text-green-500';
    if (ping < 60) return 'text-yellow-500';
    return 'text-red-500';
  };

  return (
    <div className="w-full h-full bg-gradient-to-br from-zinc-950 via-black to-zinc-900 overflow-auto">
      <div className="min-h-full p-6">
        {/* Header */}
        <div className="flex items-center justify-between mb-8">
          <div className="flex items-center gap-4">
            <button
              onClick={onBack}
              className="p-3 bg-zinc-800/50 hover:bg-zinc-700/50 border-2 border-zinc-700 rounded transition-colors"
            >
              <ArrowLeft className="w-6 h-6 text-white" />
            </button>
            <h1 className="text-white flex items-center gap-3">
              <Users className="w-8 h-8 text-red-600" />
              <span style={{ fontFamily: 'monospace', letterSpacing: '0.1em' }}>MULTIPLAYER LOBBY</span>
            </h1>
          </div>

          <button
            onClick={() => setShowCreateRoom(true)}
            className="flex items-center gap-2 bg-gradient-to-r from-red-600 to-red-700 hover:from-red-500 hover:to-red-600 text-white px-6 py-3 rounded-lg transition-all transform hover:scale-[1.02] active:scale-[0.98] shadow-lg shadow-red-600/20 border-2 border-red-500"
          >
            <Plus className="w-5 h-5" />
            <span style={{ fontFamily: 'monospace', letterSpacing: '0.1em' }}>CREATE ROOM</span>
          </button>
        </div>

        <div className="max-w-6xl mx-auto">
          {/* Search bar and stats */}
          <div className="mb-6 flex gap-4">
            <div className="flex-1 relative">
              <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-zinc-500" />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="SEARCH ROOM OR HOST..."
                className="w-full bg-zinc-900/50 border-2 border-zinc-800 text-white rounded-lg pl-12 pr-4 py-4 focus:outline-none focus:border-red-600 transition-colors"
                style={{ fontFamily: 'monospace' }}
              />
            </div>
            <div className="bg-zinc-900/50 border-2 border-zinc-800 rounded-lg px-6 py-4 flex items-center gap-3">
              <div className="w-3 h-3 bg-green-500 rounded-full animate-pulse" />
              <span className="text-zinc-400 text-sm" style={{ fontFamily: 'monospace' }}>
                ONLINE: <span className="text-white">1,247</span>
              </span>
            </div>
          </div>

          {/* Column headers */}
          <div className="bg-zinc-900/80 border-2 border-zinc-800 rounded-t-lg px-6 py-3 grid grid-cols-12 gap-4 text-xs text-zinc-500" style={{ fontFamily: 'monospace', letterSpacing: '0.1em' }}>
            <div className="col-span-3">ROOM NAME</div>
            <div className="col-span-2">HOST</div>
            <div className="col-span-2">MAP</div>
            <div className="col-span-2 text-center">PLAYERS</div>
            <div className="col-span-1 text-center">PING</div>
            <div className="col-span-1 text-center">STATUS</div>
            <div className="col-span-1"></div>
          </div>

          {/* Rooms list */}
          <div className="bg-zinc-900/50 border-2 border-t-0 border-zinc-800 rounded-b-lg divide-y divide-zinc-800">
            {filteredRooms.map((room) => (
              <div
                key={room.id}
                className="px-6 py-4 hover:bg-zinc-900/70 transition-colors grid grid-cols-12 gap-4 items-center"
              >
                {/* Room name */}
                <div className="col-span-3 flex items-center gap-2">
                  {room.isPrivate ? (
                    <Lock className="w-4 h-4 text-zinc-500" />
                  ) : (
                    <Globe className="w-4 h-4 text-zinc-500" />
                  )}
                  <span className="text-white" style={{ fontFamily: 'monospace' }}>
                    {room.name}
                  </span>
                </div>

                {/* Host */}
                <div className="col-span-2 flex items-center gap-2">
                  <User className="w-4 h-4 text-zinc-500" />
                  <span className="text-zinc-400 text-sm">{room.host}</span>
                </div>

                {/* Map */}
                <div className="col-span-2 flex items-center gap-2">
                  <MapPin className="w-4 h-4 text-red-600" />
                  <span className="text-zinc-400 text-sm">{room.map}</span>
                </div>

                {/* Players */}
                <div className="col-span-2 text-center">
                  <span className="text-zinc-400 text-sm">
                    <span className="text-white">{room.players}</span>/{room.maxPlayers}
                  </span>
                </div>

                {/* Ping */}
                <div className="col-span-1 text-center">
                  <span className={`text-sm ${getPingColor(room.ping)}`}>
                    {room.ping}ms
                  </span>
                </div>

                {/* Status */}
                <div className="col-span-1 text-center">
                  <span className={`
                    text-xs px-2 py-1 rounded
                    ${room.status === 'waiting' 
                      ? 'bg-green-600/20 text-green-400' 
                      : 'bg-red-600/20 text-red-400'
                    }
                  `}>
                    {room.status === 'waiting' ? 'OPEN' : 'PLAY'}
                  </span>
                </div>

                {/* Join button */}
                <div className="col-span-1 text-right">
                  <button
                    onClick={() => handleJoinRoom(room)}
                    disabled={room.status === 'playing' || room.players >= room.maxPlayers}
                    className={`
                      px-4 py-2 rounded transition-all text-sm
                      ${room.status === 'playing' || room.players >= room.maxPlayers
                        ? 'bg-zinc-800 text-zinc-600 cursor-not-allowed'
                        : 'bg-red-600 hover:bg-red-500 text-white transform hover:scale-[1.05] active:scale-[0.95]'
                      }
                    `}
                  >
                    JOIN
                  </button>
                </div>
              </div>
            ))}
          </div>

          {/* Quick join button */}
          <div className="mt-6 grid grid-cols-2 gap-4">
            <button className="bg-zinc-900/50 hover:bg-zinc-800/50 border-2 border-zinc-800 text-zinc-400 hover:text-white py-4 rounded-lg transition-all">
              <span style={{ fontFamily: 'monospace', letterSpacing: '0.1em' }}>QUICK MATCH</span>
            </button>
            <button className="bg-zinc-900/50 hover:bg-zinc-800/50 border-2 border-zinc-800 text-zinc-400 hover:text-white py-4 rounded-lg transition-all">
              <span style={{ fontFamily: 'monospace', letterSpacing: '0.1em' }}>REFRESH</span>
            </button>
          </div>
        </div>
      </div>

      {/* Create Room Modal */}
      {showCreateRoom && (
        <div className="fixed inset-0 bg-black/80 backdrop-blur-sm flex items-center justify-center z-50 p-4">
          <div className="bg-zinc-900 border-2 border-red-600/30 rounded-lg p-8 w-full max-w-md">
            <div className="flex items-center gap-3 mb-6">
              <Flag className="w-6 h-6 text-red-600" />
              <h2 className="text-white" style={{ fontFamily: 'monospace', letterSpacing: '0.1em' }}>
                CREATE ROOM
              </h2>
            </div>

            <div className="space-y-5">
              {/* Room name */}
              <div>
                <label className="block text-zinc-400 text-sm mb-2" style={{ fontFamily: 'monospace' }}>
                  ROOM NAME
                </label>
                <input
                  type="text"
                  value={roomName}
                  onChange={(e) => setRoomName(e.target.value)}
                  placeholder="ENTER ROOM NAME..."
                  className="w-full bg-zinc-800/50 border-2 border-zinc-700 text-white rounded px-4 py-3 focus:outline-none focus:border-red-600 transition-colors"
                  style={{ fontFamily: 'monospace' }}
                />
              </div>

              {/* Max players */}
              <div>
                <label className="block text-zinc-400 text-sm mb-2" style={{ fontFamily: 'monospace' }}>
                  MAX PLAYERS
                </label>
                <div className="flex items-center gap-4">
                  <button
                    onClick={() => setMaxPlayers(Math.max(2, maxPlayers - 1))}
                    className="w-12 h-12 bg-zinc-800 hover:bg-zinc-700 border-2 border-zinc-700 text-white rounded transition-colors"
                  >
                    -
                  </button>
                  <div className="flex-1 text-center text-white" style={{ fontSize: '2rem', fontFamily: 'monospace' }}>
                    {maxPlayers}
                  </div>
                  <button
                    onClick={() => setMaxPlayers(Math.min(16, maxPlayers + 1))}
                    className="w-12 h-12 bg-zinc-800 hover:bg-zinc-700 border-2 border-zinc-700 text-white rounded transition-colors"
                  >
                    +
                  </button>
                </div>
              </div>

              {/* Private room */}
              <div className="flex items-center justify-between p-4 bg-zinc-800/30 border border-zinc-700 rounded">
                <label className="text-zinc-400 text-sm flex items-center gap-2" style={{ fontFamily: 'monospace' }}>
                  <Lock className="w-4 h-4" />
                  PRIVATE ROOM
                </label>
                <button
                  onClick={() => setIsPrivate(!isPrivate)}
                  className={`
                    relative w-14 h-8 rounded-full transition-colors
                    ${isPrivate ? 'bg-red-600' : 'bg-zinc-700'}
                  `}
                >
                  <div className={`
                    absolute top-1 w-6 h-6 bg-white rounded-full transition-transform
                    ${isPrivate ? 'right-1' : 'left-1'}
                  `} />
                </button>
              </div>

              {/* Buttons */}
              <div className="flex gap-3 pt-4">
                <button
                  onClick={() => setShowCreateRoom(false)}
                  className="flex-1 bg-zinc-800 hover:bg-zinc-700 border-2 border-zinc-700 text-white py-3 rounded-lg transition-colors"
                  style={{ fontFamily: 'monospace', letterSpacing: '0.1em' }}
                >
                  CANCEL
                </button>
                <button
                  onClick={handleCreateRoom}
                  className="flex-1 bg-gradient-to-r from-red-600 to-red-700 hover:from-red-500 hover:to-red-600 text-white py-3 rounded-lg transition-all border-2 border-red-500"
                  style={{ fontFamily: 'monospace', letterSpacing: '0.1em' }}
                >
                  CREATE
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
