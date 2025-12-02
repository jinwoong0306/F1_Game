import React from 'react';
import { User, Users, Trophy, Settings as SettingsIcon, LogOut, Flag } from 'lucide-react';
import { PixelLogo } from './PixelLogo';
import { Screen } from '../App';

interface MainMenuProps {
  onNavigate: (screen: Screen) => void;
}

export function MainMenu({ onNavigate }: MainMenuProps) {
  const menuItems = [
    { icon: User, label: '싱글 플레이', screen: 'singlePlayer' as Screen, color: 'red' },
    { icon: Users, label: '멀티 플레이', screen: 'multiPlayer' as Screen, color: 'red' },
    { icon: Trophy, label: '리더보드', screen: 'leaderboard' as Screen, color: 'amber' },
    { icon: SettingsIcon, label: '설정', screen: 'settings' as Screen, color: 'zinc' },
  ];

  return (
    <div className="w-full h-full flex items-center justify-center bg-gradient-to-br from-zinc-950 via-black to-red-950/20 relative overflow-hidden">
      {/* Animated background grid */}
      <div className="absolute inset-0 opacity-5">
        <div className="absolute inset-0" style={{
          backgroundImage: `
            linear-gradient(to right, rgba(239, 68, 68, 0.3) 1px, transparent 1px),
            linear-gradient(to bottom, rgba(239, 68, 68, 0.3) 1px, transparent 1px)
          `,
          backgroundSize: '100px 100px',
        }} />
      </div>

      {/* Racing stripes */}
      <div className="absolute top-0 left-1/4 w-2 h-full bg-gradient-to-b from-transparent via-red-600/20 to-transparent" />
      <div className="absolute top-0 right-1/4 w-2 h-full bg-gradient-to-b from-transparent via-red-600/20 to-transparent" />

      {/* Menu container */}
      <div className="relative z-10 w-full max-w-2xl mx-4">
        {/* Logo */}
        <div className="flex justify-center mb-12">
          <PixelLogo size="large" />
        </div>

        {/* Menu items */}
        <div className="space-y-4 mb-6">
          {menuItems.map((item, index) => {
            const Icon = item.icon;
            const isMainAction = item.color === 'red';
            
            return (
              <button
                key={index}
                onClick={() => onNavigate(item.screen)}
                className={`
                  w-full group relative overflow-hidden
                  ${isMainAction 
                    ? 'bg-gradient-to-r from-red-600 to-red-700 hover:from-red-500 hover:to-red-600 shadow-lg shadow-red-600/20 border-2 border-red-500' 
                    : 'bg-zinc-800/50 hover:bg-zinc-700/50 border-2 border-zinc-700'
                  }
                  text-white py-5 px-6 rounded-lg
                  transition-all duration-200
                  transform hover:scale-[1.02] active:scale-[0.98]
                `}
              >
                {/* Pixel decoration */}
                <div className="absolute top-0 left-0 w-2 h-2 bg-white/20" />
                <div className="absolute top-0 right-0 w-2 h-2 bg-white/20" />
                <div className="absolute bottom-0 left-0 w-2 h-2 bg-white/20" />
                <div className="absolute bottom-0 right-0 w-2 h-2 bg-white/20" />
                
                {/* Shine effect */}
                {isMainAction && (
                  <div className="absolute inset-0 bg-gradient-to-r from-transparent via-white/10 to-transparent translate-x-[-200%] group-hover:translate-x-[200%] transition-transform duration-700" />
                )}
                
                <div className="flex items-center gap-4 relative z-10">
                  <div className={`
                    p-2 rounded
                    ${isMainAction ? 'bg-white/20' : 'bg-zinc-700/50'}
                  `}>
                    <Icon className="w-6 h-6" />
                  </div>
                  <span className="flex-1 text-left" style={{ fontFamily: 'monospace', letterSpacing: '0.1em' }}>
                    {item.label.toUpperCase()}
                  </span>
                  <Flag className={`w-5 h-5 ${isMainAction ? 'text-white/80' : 'text-zinc-500'}`} />
                </div>
              </button>
            );
          })}
        </div>

        {/* Exit button */}
        <button
          onClick={() => alert('게임 종료')}
          className="w-full bg-zinc-900/50 hover:bg-zinc-800/50 border-2 border-zinc-800 text-zinc-400 hover:text-red-500 py-4 px-6 rounded-lg transition-all flex items-center justify-center gap-3"
        >
          <LogOut className="w-5 h-5" />
          <span style={{ fontFamily: 'monospace', letterSpacing: '0.1em' }}>EXIT</span>
        </button>

        {/* Build info */}
        <div className="mt-8 text-center text-zinc-700 text-xs" style={{ fontFamily: 'monospace' }}>
          BUILD 2025.11.25 | ONLINE PLAYERS: 1,247
        </div>
      </div>
    </div>
  );
}