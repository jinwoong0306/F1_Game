import React, { useState } from 'react';
import { ArrowLeft, Volume2, Music, Monitor, Vibrate, Bell, User, Globe } from 'lucide-react';

interface SettingsProps {
  onBack: () => void;
}

export function Settings({ onBack }: SettingsProps) {
  const [sfxVolume, setSfxVolume] = useState(75);
  const [musicVolume, setMusicVolume] = useState(60);
  const [graphics, setGraphics] = useState<'low' | 'medium' | 'high'>('high');
  const [vibration, setVibration] = useState(true);
  const [notifications, setNotifications] = useState(true);
  const [language, setLanguage] = useState('ko');

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
            설정
          </h1>
        </div>

        <div className="max-w-3xl mx-auto space-y-6">
          {/* Audio Settings */}
          <div className="bg-zinc-900/50 border border-zinc-800 rounded-lg p-6">
            <h2 className="text-white flex items-center gap-2 mb-6">
              <Volume2 className="w-6 h-6 text-red-600" />
              오디오
            </h2>

            <div className="space-y-6">
              {/* SFX Volume */}
              <div>
                <div className="flex items-center justify-between mb-3">
                  <label className="text-zinc-400">효과음</label>
                  <span className="text-white">{sfxVolume}%</span>
                </div>
                <input
                  type="range"
                  min="0"
                  max="100"
                  value={sfxVolume}
                  onChange={(e) => setSfxVolume(Number(e.target.value))}
                  className="w-full"
                />
              </div>

              {/* Music Volume */}
              <div>
                <div className="flex items-center justify-between mb-3">
                  <label className="text-zinc-400 flex items-center gap-2">
                    <Music className="w-4 h-4" />
                    배경음악
                  </label>
                  <span className="text-white">{musicVolume}%</span>
                </div>
                <input
                  type="range"
                  min="0"
                  max="100"
                  value={musicVolume}
                  onChange={(e) => setMusicVolume(Number(e.target.value))}
                  className="w-full"
                />
              </div>
            </div>
          </div>

          {/* Graphics Settings */}
          <div className="bg-zinc-900/50 border border-zinc-800 rounded-lg p-6">
            <h2 className="text-white flex items-center gap-2 mb-6">
              <Monitor className="w-6 h-6 text-red-600" />
              그래픽
            </h2>

            <div className="grid grid-cols-3 gap-3">
              <button
                onClick={() => setGraphics('low')}
                className={`
                  py-4 rounded-lg border-2 transition-all
                  ${graphics === 'low'
                    ? 'border-red-600 bg-red-600/10 text-white'
                    : 'border-zinc-700 bg-zinc-800/30 text-zinc-400 hover:bg-zinc-800'
                  }
                `}
              >
                낮음
              </button>
              <button
                onClick={() => setGraphics('medium')}
                className={`
                  py-4 rounded-lg border-2 transition-all
                  ${graphics === 'medium'
                    ? 'border-red-600 bg-red-600/10 text-white'
                    : 'border-zinc-700 bg-zinc-800/30 text-zinc-400 hover:bg-zinc-800'
                  }
                `}
              >
                보통
              </button>
              <button
                onClick={() => setGraphics('high')}
                className={`
                  py-4 rounded-lg border-2 transition-all
                  ${graphics === 'high'
                    ? 'border-red-600 bg-red-600/10 text-white'
                    : 'border-zinc-700 bg-zinc-800/30 text-zinc-400 hover:bg-zinc-800'
                  }
                `}
              >
                높음
              </button>
            </div>
          </div>

          {/* Game Settings */}
          <div className="bg-zinc-900/50 border border-zinc-800 rounded-lg p-6">
            <h2 className="text-white mb-6">게임</h2>

            <div className="space-y-4">
              {/* Vibration */}
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <Vibrate className="w-5 h-5 text-red-600" />
                  <label className="text-zinc-400">진동</label>
                </div>
                <button
                  onClick={() => setVibration(!vibration)}
                  className={`
                    relative w-14 h-8 rounded-full transition-colors
                    ${vibration ? 'bg-red-600' : 'bg-zinc-700'}
                  `}
                >
                  <div className={`
                    absolute top-1 w-6 h-6 bg-white rounded-full transition-transform
                    ${vibration ? 'right-1' : 'left-1'}
                  `} />
                </button>
              </div>

              {/* Notifications */}
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <Bell className="w-5 h-5 text-red-600" />
                  <label className="text-zinc-400">알림</label>
                </div>
                <button
                  onClick={() => setNotifications(!notifications)}
                  className={`
                    relative w-14 h-8 rounded-full transition-colors
                    ${notifications ? 'bg-red-600' : 'bg-zinc-700'}
                  `}
                >
                  <div className={`
                    absolute top-1 w-6 h-6 bg-white rounded-full transition-transform
                    ${notifications ? 'right-1' : 'left-1'}
                  `} />
                </button>
              </div>
            </div>
          </div>

          {/* Account Settings */}
          <div className="bg-zinc-900/50 border border-zinc-800 rounded-lg p-6">
            <h2 className="text-white flex items-center gap-2 mb-6">
              <User className="w-6 h-6 text-red-600" />
              계정
            </h2>

            <div className="space-y-4">
              <button className="w-full text-left px-4 py-3 bg-zinc-800/30 hover:bg-zinc-800/50 border border-zinc-700 rounded-lg text-zinc-400 transition-colors">
                프로필 편집
              </button>
              <button className="w-full text-left px-4 py-3 bg-zinc-800/30 hover:bg-zinc-800/50 border border-zinc-700 rounded-lg text-zinc-400 transition-colors">
                비밀번호 변경
              </button>
            </div>
          </div>

          {/* Language */}
          <div className="bg-zinc-900/50 border border-zinc-800 rounded-lg p-6">
            <h2 className="text-white flex items-center gap-2 mb-6">
              <Globe className="w-6 h-6 text-red-600" />
              언어
            </h2>

            <select
              value={language}
              onChange={(e) => setLanguage(e.target.value)}
              className="w-full bg-zinc-800/50 border border-zinc-700 text-white rounded px-4 py-3 focus:outline-none focus:border-red-600 transition-colors"
            >
              <option value="ko">한국어</option>
              <option value="en">English</option>
              <option value="ja">日本語</option>
              <option value="zh">中文</option>
            </select>
          </div>

          {/* Save button */}
          <button className="w-full bg-gradient-to-r from-red-600 to-red-700 hover:from-red-500 hover:to-red-600 text-white py-4 rounded-lg transition-all transform hover:scale-[1.02] active:scale-[0.98] shadow-lg shadow-red-600/30">
            설정 저장
          </button>
        </div>
      </div>
    </div>
  );
}
