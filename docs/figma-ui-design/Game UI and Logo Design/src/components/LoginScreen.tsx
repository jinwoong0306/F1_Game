import React, { useState } from 'react';
import { User, Lock, Flag } from 'lucide-react';
import { PixelLogo } from './PixelLogo';

interface LoginScreenProps {
  onComplete: () => void;
}

export function LoginScreen({ onComplete }: LoginScreenProps) {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onComplete();
  };

  return (
    <div className="w-full h-full flex items-center justify-center bg-gradient-to-br from-zinc-950 via-black to-zinc-900 relative overflow-hidden">
      {/* Background pattern */}
      <div className="absolute inset-0 opacity-5">
        <div className="absolute inset-0" style={{
          backgroundImage: `repeating-linear-gradient(90deg, transparent, transparent 50px, rgba(239, 68, 68, 0.1) 50px, rgba(239, 68, 68, 0.1) 52px)`,
        }} />
      </div>

      {/* Checkered pattern corner */}
      <div className="absolute top-0 right-0 w-64 h-64 opacity-5">
        <div className="grid grid-cols-8 grid-rows-8 w-full h-full">
          {Array.from({ length: 64 }).map((_, i) => (
            <div
              key={i}
              className={`${
                (Math.floor(i / 8) + (i % 8)) % 2 === 0
                  ? 'bg-white'
                  : 'bg-transparent'
              }`}
            />
          ))}
        </div>
      </div>

      {/* Form container */}
      <div className="relative z-10 w-full max-w-md mx-4">
        {/* Logo */}
        <div className="flex justify-center mb-8">
          <PixelLogo size="medium" />
        </div>

        {/* Form */}
        <div className="bg-zinc-900/80 backdrop-blur-lg border-2 border-red-600/30 rounded-lg p-8 shadow-2xl">
          <div className="flex items-center gap-2 mb-6">
            <Flag className="w-6 h-6 text-red-600" />
            <h2 className="text-white">레이서 로그인</h2>
          </div>

          <form onSubmit={handleSubmit} className="space-y-5">
            {/* Username */}
            <div>
              <label className="block text-zinc-400 text-sm mb-2">사용자명</label>
              <div className="relative">
                <User className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-zinc-500" />
                <input
                  type="text"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  placeholder="레이서명을 입력하세요"
                  className="w-full bg-zinc-800/50 border border-zinc-700 rounded px-10 py-3 text-white placeholder-zinc-600 focus:outline-none focus:border-red-600 transition-colors"
                  required
                />
              </div>
            </div>

            {/* Password */}
            <div>
              <label className="block text-zinc-400 text-sm mb-2">비밀번호</label>
              <div className="relative">
                <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-zinc-500" />
                <input
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="비밀번호를 입력하세요"
                  className="w-full bg-zinc-800/50 border border-zinc-700 rounded px-10 py-3 text-white placeholder-zinc-600 focus:outline-none focus:border-red-600 transition-colors"
                  required
                />
              </div>
            </div>

            {/* Submit button */}
            <button
              type="submit"
              className="w-full bg-gradient-to-r from-red-600 to-red-700 hover:from-red-500 hover:to-red-600 text-white py-3 rounded transition-all transform hover:scale-[1.02] active:scale-[0.98] shadow-lg shadow-red-600/30"
            >
              게임 시작
            </button>
          </form>

          {/* Additional links */}
          <div className="mt-6 flex items-center justify-between text-sm">
            <button className="text-zinc-500 hover:text-red-600 transition-colors">
              비밀번호 찾기
            </button>
            <button className="text-zinc-500 hover:text-red-600 transition-colors">
              계정 만들기
            </button>
          </div>
        </div>

        {/* Version info */}
        <div className="mt-6 text-center text-zinc-600 text-xs" style={{ fontFamily: 'monospace' }}>
          VERSION 1.0.0 BETA
        </div>
      </div>
    </div>
  );
}