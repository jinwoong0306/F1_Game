import React, { useState } from 'react';
import { ArrowLeft, Crown, Check, X, Settings, MessageSquare, Send, Users, Map, RotateCw } from 'lucide-react';

interface MultiPlayerRoomProps {
  onBack: () => void;
}

interface Player {
  id: number;
  name: string;
  isReady: boolean;
  isHost: boolean;
  car: string;
  carColor: string;
}

const availableMaps = [
  { id: 1, name: 'NEON CITY CIRCUIT', difficulty: 'EASY' },
  { id: 2, name: 'DESERT RACING TRACK', difficulty: 'NORMAL' },
  { id: 3, name: 'MOUNTAIN WINDING ROAD', difficulty: 'HARD' },
  { id: 4, name: 'FUTURE CITY HIGHWAY', difficulty: 'EXTREME' },
];

export function MultiPlayerRoom({ onBack }: MultiPlayerRoomProps) {
  const [players, setPlayers] = useState<(Player | null)[]>([
    { id: 1, name: 'SpeedRacer', isReady: true, isHost: true, car: 'THUNDER BOLT', carColor: '#dc2626' },
    { id: 2, name: 'NitroKing', isReady: false, isHost: false, car: 'BLUE STORM', carColor: '#2563eb' },
    null,
    null,
  ]);

  const [isReady, setIsReady] = useState(false);
  const [selectedMap, setSelectedMap] = useState(availableMaps[0].id);
  const [laps, setLaps] = useState(5);
  const [chatMessage, setChatMessage] = useState('');
  const [chatMessages, setChatMessages] = useState([
    { player: 'SpeedRacer', message: '안녕하세요!', time: '14:23' },
    { player: 'NitroKing', message: '준비됐습니다', time: '14:24' },
  ]);

  const currentPlayer = players[0]; // 현재 사용자 (예시)
  const isHost = currentPlayer?.isHost || false;
  const allReady = players.filter(p => p !== null).every(p => p?.isReady);
  const canStart = allReady && players.filter(p => p !== null).length >= 2;

  const handleReady = () => {
    setIsReady(!isReady);
  };

  const handleStart = () => {
    if (canStart) {
      alert('게임을 시작합니다!');
    }
  };

  const handleSendMessage = (e: React.FormEvent) => {
    e.preventDefault();
    if (chatMessage.trim()) {
      setChatMessages([...chatMessages, {
        player: 'SpeedRacer',
        message: chatMessage,
        time: new Date().toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })
      }]);
      setChatMessage('');
    }
  };

  const getDifficultyColor = (difficulty: string) => {
    switch (difficulty) {
      case 'EASY': return 'bg-green-600/20 text-green-400 border-green-600/50';
      case 'NORMAL': return 'bg-blue-600/20 text-blue-400 border-blue-600/50';
      case 'HARD': return 'bg-orange-600/20 text-orange-400 border-orange-600/50';
      case 'EXTREME': return 'bg-red-600/20 text-red-400 border-red-600/50';
      default: return 'bg-zinc-600/20 text-zinc-400 border-zinc-600/50';
    }
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
            <div>
              <h1 className="text-white flex items-center gap-3">
                <Users className="w-8 h-8 text-red-600" />
                <span style={{ fontFamily: 'monospace', letterSpacing: '0.1em' }}>MULTIPLAYER ROOM</span>
              </h1>
              <p className="text-zinc-500 text-sm mt-1" style={{ fontFamily: 'monospace' }}>
                ROOM CODE: #A7F2X9
              </p>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <div className="bg-zinc-900/50 border-2 border-zinc-800 rounded-lg px-4 py-2 flex items-center gap-2">
              <div className="w-2 h-2 bg-green-500 rounded-full animate-pulse" />
              <span className="text-zinc-400 text-sm" style={{ fontFamily: 'monospace' }}>
                CONNECTED
              </span>
            </div>
          </div>
        </div>

        <div className="max-w-7xl mx-auto grid lg:grid-cols-3 gap-6">
          {/* Left Column - Players */}
          <div className="lg:col-span-2 space-y-6">
            {/* Player Slots */}
            <div className="bg-zinc-900/50 border-2 border-zinc-800 rounded-lg p-6">
              <h2 className="text-white mb-4 flex items-center gap-2">
                <Users className="w-6 h-6 text-red-600" />
                <span style={{ fontFamily: 'monospace', letterSpacing: '0.1em' }}>
                  PLAYERS ({players.filter(p => p !== null).length}/4)
                </span>
              </h2>

              <div className="grid grid-cols-2 gap-4">
                {players.map((player, index) => (
                  <div
                    key={index}
                    className={`
                      p-5 rounded-lg border-2 transition-all
                      ${player 
                        ? 'border-zinc-700 bg-zinc-800/50' 
                        : 'border-zinc-800 bg-zinc-900/30 border-dashed'
                      }
                    `}
                  >
                    {player ? (
                      <div>
                        <div className="flex items-start justify-between mb-3">
                          <div className="flex-1">
                            <div className="flex items-center gap-2 mb-1">
                              {player.isHost && (
                                <Crown className="w-4 h-4 text-yellow-500" />
                              )}
                              <h3 className="text-white" style={{ fontFamily: 'monospace' }}>
                                {player.name}
                              </h3>
                            </div>
                            <p className="text-xs text-zinc-500">{player.car}</p>
                          </div>
                          <div
                            className="w-8 h-8 rounded border-2 border-zinc-700"
                            style={{ backgroundColor: player.carColor }}
                          />
                        </div>

                        <div className="flex items-center gap-2">
                          {player.isReady ? (
                            <>
                              <Check className="w-4 h-4 text-green-500" />
                              <span className="text-xs text-green-500" style={{ fontFamily: 'monospace' }}>
                                READY
                              </span>
                            </>
                          ) : (
                            <>
                              <X className="w-4 h-4 text-zinc-500" />
                              <span className="text-xs text-zinc-500" style={{ fontFamily: 'monospace' }}>
                                NOT READY
                              </span>
                            </>
                          )}
                        </div>
                      </div>
                    ) : (
                      <div className="flex items-center justify-center h-full">
                        <div className="text-center">
                          <Users className="w-8 h-8 text-zinc-700 mx-auto mb-2" />
                          <p className="text-xs text-zinc-600" style={{ fontFamily: 'monospace' }}>
                            WAITING...
                          </p>
                        </div>
                      </div>
                    )}
                  </div>
                ))}
              </div>

              {/* Ready Button */}
              <div className="mt-6 flex gap-3">
                <button
                  onClick={handleReady}
                  className={`
                    flex-1 py-4 rounded-lg border-2 transition-all transform hover:scale-[1.02] active:scale-[0.98]
                    ${isReady
                      ? 'bg-green-600 hover:bg-green-500 border-green-500 text-white'
                      : 'bg-zinc-800 hover:bg-zinc-700 border-zinc-700 text-white'
                    }
                  `}
                >
                  <span style={{ fontFamily: 'monospace', letterSpacing: '0.1em' }}>
                    {isReady ? 'READY ✓' : 'READY UP'}
                  </span>
                </button>

                {isHost && (
                  <button
                    onClick={handleStart}
                    disabled={!canStart}
                    className={`
                      flex-1 py-4 rounded-lg border-2 transition-all
                      ${canStart
                        ? 'bg-gradient-to-r from-red-600 to-red-700 hover:from-red-500 hover:to-red-600 border-red-500 text-white transform hover:scale-[1.02] active:scale-[0.98]'
                        : 'bg-zinc-800 border-zinc-700 text-zinc-600 cursor-not-allowed'
                      }
                    `}
                  >
                    <span style={{ fontFamily: 'monospace', letterSpacing: '0.1em' }}>
                      START RACE
                    </span>
                  </button>
                )}
              </div>
            </div>

            {/* Chat */}
            <div className="bg-zinc-900/50 border-2 border-zinc-800 rounded-lg p-6">
              <h2 className="text-white mb-4 flex items-center gap-2">
                <MessageSquare className="w-6 h-6 text-red-600" />
                <span style={{ fontFamily: 'monospace', letterSpacing: '0.1em' }}>CHAT</span>
              </h2>

              <div className="bg-zinc-950/50 border border-zinc-800 rounded-lg p-4 h-48 overflow-y-auto mb-3">
                <div className="space-y-3">
                  {chatMessages.map((msg, index) => (
                    <div key={index} className="text-sm">
                      <span className="text-zinc-500 text-xs mr-2">[{msg.time}]</span>
                      <span className="text-red-500" style={{ fontFamily: 'monospace' }}>
                        {msg.player}:
                      </span>
                      <span className="text-zinc-300 ml-2">{msg.message}</span>
                    </div>
                  ))}
                </div>
              </div>

              <form onSubmit={handleSendMessage} className="flex gap-2">
                <input
                  type="text"
                  value={chatMessage}
                  onChange={(e) => setChatMessage(e.target.value)}
                  placeholder="메시지를 입력하세요..."
                  className="flex-1 bg-zinc-800/50 border border-zinc-700 text-white rounded px-4 py-2 focus:outline-none focus:border-red-600 transition-colors text-sm"
                />
                <button
                  type="submit"
                  className="px-4 py-2 bg-red-600 hover:bg-red-500 text-white rounded transition-colors"
                >
                  <Send className="w-4 h-4" />
                </button>
              </form>
            </div>
          </div>

          {/* Right Column - Settings */}
          <div className="space-y-6">
            {/* Map Selection */}
            <div className="bg-zinc-900/50 border-2 border-zinc-800 rounded-lg p-6">
              <h2 className="text-white mb-4 flex items-center gap-2">
                <Map className="w-6 h-6 text-red-600" />
                <span style={{ fontFamily: 'monospace', letterSpacing: '0.1em' }}>MAP</span>
              </h2>

              <div className="space-y-2">
                {availableMaps.map((map) => (
                  <button
                    key={map.id}
                    onClick={() => isHost && setSelectedMap(map.id)}
                    disabled={!isHost}
                    className={`
                      w-full text-left p-3 rounded-lg border-2 transition-all text-sm
                      ${selectedMap === map.id
                        ? 'border-red-600 bg-red-600/10'
                        : 'border-zinc-700 bg-zinc-800/30 hover:bg-zinc-800/50'
                      }
                      ${!isHost && 'cursor-not-allowed opacity-60'}
                    `}
                  >
                    <div className="text-white mb-1" style={{ fontFamily: 'monospace', fontSize: '0.8rem' }}>
                      {map.name}
                    </div>
                    <span className={`text-xs px-2 py-1 rounded border ${getDifficultyColor(map.difficulty)}`}>
                      {map.difficulty}
                    </span>
                  </button>
                ))}
              </div>

              {!isHost && (
                <p className="text-xs text-zinc-600 mt-3" style={{ fontFamily: 'monospace' }}>
                  * HOST ONLY
                </p>
              )}
            </div>

            {/* Laps */}
            <div className="bg-zinc-900/50 border-2 border-zinc-800 rounded-lg p-6">
              <h2 className="text-white mb-4 flex items-center gap-2">
                <RotateCw className="w-6 h-6 text-red-600" />
                <span style={{ fontFamily: 'monospace', letterSpacing: '0.1em' }}>LAPS</span>
              </h2>

              <div className="flex items-center gap-3 mb-4">
                <button
                  onClick={() => isHost && setLaps(Math.max(1, laps - 1))}
                  disabled={!isHost}
                  className="w-10 h-10 bg-zinc-800 hover:bg-zinc-700 border-2 border-zinc-700 text-white rounded transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  -
                </button>
                <div className="flex-1 text-center">
                  <div className="text-white" style={{ fontSize: '2rem', fontFamily: 'monospace' }}>
                    {laps}
                  </div>
                </div>
                <button
                  onClick={() => isHost && setLaps(Math.min(20, laps + 1))}
                  disabled={!isHost}
                  className="w-10 h-10 bg-zinc-800 hover:bg-zinc-700 border-2 border-zinc-700 text-white rounded transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  +
                </button>
              </div>

              {!isHost && (
                <p className="text-xs text-zinc-600 text-center" style={{ fontFamily: 'monospace' }}>
                  * HOST ONLY
                </p>
              )}
            </div>

            {/* Room Info */}
            <div className="bg-zinc-900/50 border-2 border-zinc-800 rounded-lg p-6">
              <h2 className="text-white mb-4 flex items-center gap-2">
                <Settings className="w-6 h-6 text-red-600" />
                <span style={{ fontFamily: 'monospace', letterSpacing: '0.1em' }}>INFO</span>
              </h2>

              <div className="space-y-2 text-sm">
                <div className="flex justify-between">
                  <span className="text-zinc-500">HOST:</span>
                  <span className="text-white" style={{ fontFamily: 'monospace' }}>
                    {players.find(p => p?.isHost)?.name}
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-zinc-500">PLAYERS:</span>
                  <span className="text-white" style={{ fontFamily: 'monospace' }}>
                    {players.filter(p => p !== null).length}/4
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-zinc-500">READY:</span>
                  <span className="text-white" style={{ fontFamily: 'monospace' }}>
                    {players.filter(p => p?.isReady).length}/{players.filter(p => p !== null).length}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
