import React, { useState } from 'react';
import { ArrowLeft, Car, Map, RotateCw, Flag, Play, Zap, Gauge, TrendingUp } from 'lucide-react';

interface SinglePlayerSetupProps {
  onBack: () => void;
}

const vehicles = [
  { 
    id: 1, 
    name: 'THUNDER BOLT', 
    speed: 95, 
    handling: 85, 
    acceleration: 90, 
    color: '#dc2626',
    description: '최고 속도에 특화된 레이싱카'
  },
  { 
    id: 2, 
    name: 'SILVER ARROW', 
    speed: 88, 
    handling: 92, 
    acceleration: 85, 
    color: '#71717a',
    description: '완벽한 핸들링의 정석'
  },
  { 
    id: 3, 
    name: 'BLUE STORM', 
    speed: 90, 
    handling: 90, 
    acceleration: 88, 
    color: '#2563eb',
    description: '균형잡힌 올라운더'
  },
  { 
    id: 4, 
    name: 'GOLD RUSH', 
    speed: 85, 
    handling: 88, 
    acceleration: 95, 
    color: '#eab308',
    description: '폭발적인 가속력'
  },
];

const maps = [
  { 
    id: 1, 
    name: 'NEON CITY CIRCUIT', 
    difficulty: 'EASY', 
    laps: 3,
    length: '2.4 KM',
    turns: 12
  },
  { 
    id: 2, 
    name: 'DESERT RACING TRACK', 
    difficulty: 'NORMAL', 
    laps: 5,
    length: '3.8 KM',
    turns: 18
  },
  { 
    id: 3, 
    name: 'MOUNTAIN WINDING ROAD', 
    difficulty: 'HARD', 
    laps: 7,
    length: '5.2 KM',
    turns: 24
  },
  { 
    id: 4, 
    name: 'FUTURE CITY HIGHWAY', 
    difficulty: 'EXTREME', 
    laps: 10,
    length: '6.5 KM',
    turns: 30
  },
];

export function SinglePlayerSetup({ onBack }: SinglePlayerSetupProps) {
  const [selectedVehicle, setSelectedVehicle] = useState(vehicles[0].id);
  const [selectedMap, setSelectedMap] = useState(maps[0].id);
  const [laps, setLaps] = useState(3);

  const currentVehicle = vehicles.find(v => v.id === selectedVehicle)!;
  const currentMap = maps.find(m => m.id === selectedMap)!;

  const handleStartRace = () => {
    alert(`레이스 시작!\n차량: ${currentVehicle.name}\n맵: ${currentMap.name}\n랩: ${laps}`);
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
        <div className="flex items-center gap-4 mb-8">
          <button
            onClick={onBack}
            className="p-3 bg-zinc-800/50 hover:bg-zinc-700/50 border-2 border-zinc-700 rounded transition-colors"
          >
            <ArrowLeft className="w-6 h-6 text-white" />
          </button>
          <h1 className="text-white flex items-center gap-3">
            <Flag className="w-8 h-8 text-red-600" />
            <span style={{ fontFamily: 'monospace', letterSpacing: '0.1em' }}>SINGLE PLAYER</span>
          </h1>
        </div>

        <div className="max-w-6xl mx-auto grid lg:grid-cols-2 gap-6">
          {/* Vehicle Selection */}
          <div className="bg-zinc-900/50 border-2 border-zinc-800 rounded-lg p-6">
            <div className="flex items-center gap-2 mb-6">
              <Car className="w-6 h-6 text-red-600" />
              <h2 className="text-white" style={{ fontFamily: 'monospace', letterSpacing: '0.1em' }}>
                SELECT VEHICLE
              </h2>
            </div>

            <div className="space-y-3">
              {vehicles.map((vehicle) => (
                <button
                  key={vehicle.id}
                  onClick={() => setSelectedVehicle(vehicle.id)}
                  className={`
                    w-full text-left p-5 rounded-lg border-2 transition-all
                    ${selectedVehicle === vehicle.id
                      ? 'border-red-600 bg-red-600/10 shadow-lg shadow-red-600/20'
                      : 'border-zinc-700 bg-zinc-800/30 hover:bg-zinc-800/50 hover:border-zinc-600'
                    }
                  `}
                >
                  <div className="flex items-start justify-between mb-3">
                    <div>
                      <h3 className="text-white mb-1" style={{ fontFamily: 'monospace', letterSpacing: '0.05em' }}>
                        {vehicle.name}
                      </h3>
                      <p className="text-xs text-zinc-500">{vehicle.description}</p>
                    </div>
                    <div 
                      className="w-10 h-10 rounded border-2 border-zinc-700"
                      style={{ backgroundColor: vehicle.color }}
                    />
                  </div>
                  
                  <div className="space-y-2">
                    {/* Speed */}
                    <div className="flex items-center gap-3">
                      <Zap className="w-4 h-4 text-red-500" />
                      <span className="text-xs text-zinc-400 w-20">SPEED</span>
                      <div className="flex-1 bg-zinc-700/50 h-2 rounded-full overflow-hidden">
                        <div 
                          className="h-full bg-gradient-to-r from-red-600 to-red-500 transition-all duration-500"
                          style={{ width: `${vehicle.speed}%` }}
                        />
                      </div>
                      <span className="text-xs text-zinc-400 w-8 text-right">{vehicle.speed}</span>
                    </div>
                    
                    {/* Handling */}
                    <div className="flex items-center gap-3">
                      <Gauge className="w-4 h-4 text-blue-500" />
                      <span className="text-xs text-zinc-400 w-20">HANDLING</span>
                      <div className="flex-1 bg-zinc-700/50 h-2 rounded-full overflow-hidden">
                        <div 
                          className="h-full bg-gradient-to-r from-blue-600 to-blue-500 transition-all duration-500"
                          style={{ width: `${vehicle.handling}%` }}
                        />
                      </div>
                      <span className="text-xs text-zinc-400 w-8 text-right">{vehicle.handling}</span>
                    </div>
                    
                    {/* Acceleration */}
                    <div className="flex items-center gap-3">
                      <TrendingUp className="w-4 h-4 text-green-500" />
                      <span className="text-xs text-zinc-400 w-20">ACCEL</span>
                      <div className="flex-1 bg-zinc-700/50 h-2 rounded-full overflow-hidden">
                        <div 
                          className="h-full bg-gradient-to-r from-green-600 to-green-500 transition-all duration-500"
                          style={{ width: `${vehicle.acceleration}%` }}
                        />
                      </div>
                      <span className="text-xs text-zinc-400 w-8 text-right">{vehicle.acceleration}</span>
                    </div>
                  </div>
                </button>
              ))}
            </div>
          </div>

          {/* Map & Laps Selection */}
          <div className="space-y-6">
            {/* Map Selection */}
            <div className="bg-zinc-900/50 border-2 border-zinc-800 rounded-lg p-6">
              <div className="flex items-center gap-2 mb-6">
                <Map className="w-6 h-6 text-red-600" />
                <h2 className="text-white" style={{ fontFamily: 'monospace', letterSpacing: '0.1em' }}>
                  SELECT TRACK
                </h2>
              </div>

              <div className="space-y-3">
                {maps.map((map) => (
                  <button
                    key={map.id}
                    onClick={() => setSelectedMap(map.id)}
                    className={`
                      w-full text-left p-5 rounded-lg border-2 transition-all
                      ${selectedMap === map.id
                        ? 'border-red-600 bg-red-600/10 shadow-lg shadow-red-600/20'
                        : 'border-zinc-700 bg-zinc-800/30 hover:bg-zinc-800/50 hover:border-zinc-600'
                      }
                    `}
                  >
                    <h3 className="text-white mb-3" style={{ fontFamily: 'monospace', letterSpacing: '0.05em' }}>
                      {map.name}
                    </h3>
                    <div className="flex items-center gap-3 flex-wrap">
                      <span className={`text-xs px-3 py-1 rounded border ${getDifficultyColor(map.difficulty)}`}>
                        {map.difficulty}
                      </span>
                      <span className="text-xs text-zinc-400 bg-zinc-800 px-3 py-1 rounded">
                        {map.length}
                      </span>
                      <span className="text-xs text-zinc-400 bg-zinc-800 px-3 py-1 rounded">
                        {map.turns} TURNS
                      </span>
                    </div>
                  </button>
                ))}
              </div>
            </div>

            {/* Laps Selection */}
            <div className="bg-zinc-900/50 border-2 border-zinc-800 rounded-lg p-6">
              <div className="flex items-center gap-2 mb-6">
                <RotateCw className="w-6 h-6 text-red-600" />
                <h2 className="text-white" style={{ fontFamily: 'monospace', letterSpacing: '0.1em' }}>
                  LAPS
                </h2>
              </div>

              <div className="flex items-center gap-4 mb-4">
                <button
                  onClick={() => setLaps(Math.max(1, laps - 1))}
                  className="w-14 h-14 bg-zinc-800 hover:bg-zinc-700 border-2 border-zinc-700 text-white rounded transition-colors"
                >
                  <span className="text-xl">-</span>
                </button>
                <div className="flex-1 text-center">
                  <div className="text-white mb-1" style={{ fontSize: '3rem', fontFamily: 'monospace' }}>
                    {laps}
                  </div>
                  <div className="text-xs text-zinc-500" style={{ fontFamily: 'monospace', letterSpacing: '0.2em' }}>
                    LAPS
                  </div>
                </div>
                <button
                  onClick={() => setLaps(Math.min(20, laps + 1))}
                  className="w-14 h-14 bg-zinc-800 hover:bg-zinc-700 border-2 border-zinc-700 text-white rounded transition-colors"
                >
                  <span className="text-xl">+</span>
                </button>
              </div>

              <div className="mt-4">
                <input
                  type="range"
                  min="1"
                  max="20"
                  value={laps}
                  onChange={(e) => setLaps(Number(e.target.value))}
                  className="w-full accent-red-600"
                />
              </div>
            </div>

            {/* Start Button */}
            <button
              onClick={handleStartRace}
              className="w-full bg-gradient-to-r from-red-600 to-red-700 hover:from-red-500 hover:to-red-600 text-white py-6 rounded-lg transition-all transform hover:scale-[1.02] active:scale-[0.98] shadow-lg shadow-red-600/30 border-2 border-red-500 flex items-center justify-center gap-3"
            >
              <Play className="w-6 h-6" />
              <span style={{ fontFamily: 'monospace', letterSpacing: '0.2em' }}>START RACE</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
