import React, { useState } from 'react';
import { User, Mail, Lock, Flag } from 'lucide-react';
import logoImage from 'figma:asset/d09a2e10cab4bfd1684a13e2075c97a08eb81967.png';

interface SignupScreenProps {
  onComplete: () => void;
}

export function SignupScreen({ onComplete }: SignupScreenProps) {
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
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

      {/* Form container */}
      <div className="relative z-10 w-full max-w-md mx-4">
        {/* Logo */}
        <div className="flex justify-center mb-8">
          <img 
            src={logoImage} 
            alt="Pixel Formula Racing" 
            className="w-[400px] max-w-full h-auto"
          />
        </div>

        {/* Form */}
        <div className="bg-zinc-900/80 backdrop-blur-lg border-2 border-red-600/30 rounded-lg p-8 shadow-2xl">
          <div className="flex items-center gap-2 mb-6">
            <Flag className="w-6 h-6 text-red-600" />
            <h2 className="text-white">레이서 등록</h2>
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

            {/* Email */}
            <div>
              <label className="block text-zinc-400 text-sm mb-2">이메일</label>
              <div className="relative">
                <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-zinc-500" />
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="이메일을 입력하세요"
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

          {/* Additional link */}
          <div className="mt-4 text-center">
            <button 
              onClick={onComplete}
              className="text-zinc-500 text-sm hover:text-red-600 transition-colors"
            >
              이미 계정이 있으신가요? 로그인
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
