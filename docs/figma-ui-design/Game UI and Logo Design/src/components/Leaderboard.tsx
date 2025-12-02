import React, { useState } from 'react';
import { ArrowLeft, Trophy, Medal, Crown, TrendingUp } from 'lucide-react';

interface LeaderboardProps {
  onBack: () => void;
}

interface Player {
  rank: number;
  name: string;
  wins: number;
  races: number;
  bestTime: string;
  points: number;
}

const mockLeaderboard: Player[] = [
  { rank: 1, name: 'SpeedDemon', wins: 147, races: 200, bestTime: '1:23.45', points: 9850 },
  { rank: 2, name: 'TurboKing', wins: 138, races: 195, bestTime: '1:24.12', points: 9720 },
  { rank: 3, name: 'RacingLegend', wins: 129, races: 188, bestTime: '1:24.89', points: 9580 },
  { rank: 4, name: 'NitroBoost', wins: 115, races: 175, bestTime: '1:25.34', points: 9320 },
  { rank: 5, name: 'DriftMaster', wins: 108, races: 168, bestTime: '1:25.67', points: 9150 },
  { rank: 6, name: 'ThunderBolt', wins: 98, races: 155, bestTime: '1:26.23', points: 8890 },
  { rank: 7, name: 'FastFury', wins: 92, races: 148, bestTime: '1:26.78', points: 8650 },
  { rank: 8, name: 'VelocityX', wins: 85, races: 140, bestTime: '1:27.12', points: 8420 },
  { rank: 9, name: 'RoadRunner', wins: 78, races: 132, bestTime: '1:27.56', points: 8180 },
  { rank: 10, name: 'CircuitKing', wins: 71, races: 125, bestTime: '1:28.01', points: 7950 },
];

export function Leaderboard({ onBack }: LeaderboardProps) {
  const [filter, setFilter] = useState<'weekly' | 'monthly' | 'allTime'>('allTime');

  const getRankIcon = (rank: number) => {
    if (rank === 1) return <Crown className="w-6 h-6 text-yellow-400" />;
    if (rank === 2) return <Medal className="w-6 h-6 text-gray-400" />;
    if (rank === 3) return <Medal className="w-6 h-6 text-amber-700" />;
    return <span className="text-zinc-500 w-6 text-center">{rank}</span>;
  };

  const getRankBg = (rank: number) => {
    if (rank === 1) return 'bg-gradient-to-r from-yellow-600/20 to-yellow-700/20 border-yellow-600/50';
    if (rank === 2) return 'bg-gradient-to-r from-gray-600/20 to-gray-700/20 border-gray-600/50';
    if (rank === 3) return 'bg-gradient-to-r from-amber-700/20 to-amber-800/20 border-amber-700/50';
    return 'bg-zinc-900/50 border-zinc-800';
  };

  return (
    <div className="w-full h-full bg-gradient-to-br from-zinc-950 via-black to-zinc-900 overflow-auto">
      <div className="min-h-full p-6">
        {/* Header */}
        <div className="flex items-center gap-4 mb-8">
          <button
            onClick={onBack}
            className="p-3 bg-zinc-800/50 hover:bg-zinc-700/50 border border-zinc-700 rounded-lg transition-colors"
          >
            <ArrowLeft className="w-6 h-6 text-white" />
          </button>
          <h1 className="text-white flex items-center gap-3">
            <Trophy className="w-8 h-8 text-red-600" />
            리더보드
          </h1>
        </div>

        <div className="max-w-5xl mx-auto">
          {/* Filter buttons */}
          <div className="flex gap-3 mb-6">
            <button
              onClick={() => setFilter('weekly')}
              className={`
                px-6 py-3 rounded-lg transition-all
                ${filter === 'weekly'
                  ? 'bg-red-600 text-white'
                  : 'bg-zinc-800/50 border border-zinc-700 text-zinc-400 hover:bg-zinc-800'
                }
              `}
            >
              주간
            </button>
            <button
              onClick={() => setFilter('monthly')}
              className={`
                px-6 py-3 rounded-lg transition-all
                ${filter === 'monthly'
                  ? 'bg-red-600 text-white'
                  : 'bg-zinc-800/50 border border-zinc-700 text-zinc-400 hover:bg-zinc-800'
                }
              `}
            >
              월간
            </button>
            <button
              onClick={() => setFilter('allTime')}
              className={`
                px-6 py-3 rounded-lg transition-all
                ${filter === 'allTime'
                  ? 'bg-red-600 text-white'
                  : 'bg-zinc-800/50 border border-zinc-700 text-zinc-400 hover:bg-zinc-800'
                }
              `}
            >
              전체
            </button>
          </div>

          {/* Top 3 Podium */}
          <div className="grid grid-cols-3 gap-4 mb-8">
            {mockLeaderboard.slice(0, 3).map((player, index) => (
              <div
                key={player.rank}
                className={`
                  ${index === 0 ? 'order-2 scale-110' : index === 1 ? 'order-1' : 'order-3'}
                  ${getRankBg(player.rank)}
                  border-2 rounded-lg p-6 text-center transition-transform hover:scale-105
                `}
              >
                <div className="flex justify-center mb-3">
                  {getRankIcon(player.rank)}
                </div>
                <h3 className="text-white mb-2">{player.name}</h3>
                <div className="text-zinc-400 text-sm space-y-1">
                  <div>{player.wins} 승</div>
                  <div className="text-red-500">{player.points.toLocaleString()} pts</div>
                </div>
              </div>
            ))}
          </div>

          {/* Leaderboard table */}
          <div className="bg-zinc-900/50 border border-zinc-800 rounded-lg overflow-hidden">
            {/* Header */}
            <div className="grid grid-cols-6 gap-4 p-4 bg-zinc-800/50 border-b border-zinc-700 text-zinc-400 text-sm">
              <div>순위</div>
              <div>플레이어</div>
              <div className="text-right">승리</div>
              <div className="text-right">전적</div>
              <div className="text-right">최고 기록</div>
              <div className="text-right">포인트</div>
            </div>

            {/* Rows */}
            <div className="divide-y divide-zinc-800">
              {mockLeaderboard.map((player) => (
                <div
                  key={player.rank}
                  className="grid grid-cols-6 gap-4 p-4 hover:bg-zinc-800/30 transition-colors"
                >
                  <div className="flex items-center gap-2">
                    {getRankIcon(player.rank)}
                  </div>
                  <div className="text-white">{player.name}</div>
                  <div className="text-zinc-400 text-right">{player.wins}</div>
                  <div className="text-zinc-400 text-right">{player.wins}/{player.races}</div>
                  <div className="text-zinc-400 text-right">{player.bestTime}</div>
                  <div className="text-right">
                    <span className="text-red-500">{player.points.toLocaleString()}</span>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Your rank card */}
          <div className="mt-6 bg-red-600/10 border-2 border-red-600/50 rounded-lg p-6">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-4">
                <TrendingUp className="w-6 h-6 text-red-600" />
                <div>
                  <h3 className="text-white">당신의 순위</h3>
                  <p className="text-zinc-400 text-sm">계속 도전하여 순위를 올리세요!</p>
                </div>
              </div>
              <div className="text-right">
                <div className="text-white mb-1">#42</div>
                <div className="text-red-500">6,890 pts</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
