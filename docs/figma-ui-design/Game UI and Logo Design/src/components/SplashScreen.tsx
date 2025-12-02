import React, { useEffect } from "react";
import { PixelLogo } from "./PixelLogo";

interface SplashScreenProps {
  onComplete: () => void;
}

export function SplashScreen({
  onComplete,
}: SplashScreenProps) {
  useEffect(() => {
    const timer = setTimeout(() => {
      onComplete();
    }, 3000);

    return () => clearTimeout(timer);
  }, [onComplete]);

  return (
    <div className="w-full h-full flex items-center justify-center bg-gradient-to-b from-zinc-950 via-black to-zinc-950 relative overflow-hidden">
      {/* Animated background grid */}
      <div className="absolute inset-0 opacity-10">
        <div
          className="absolute inset-0"
          style={{
            backgroundImage: `
            linear-gradient(to right, rgba(239, 68, 68, 0.3) 1px, transparent 1px),
            linear-gradient(to bottom, rgba(239, 68, 68, 0.3) 1px, transparent 1px)
          `,
            backgroundSize: "50px 50px",
            animation: "slideGrid 2s linear infinite",
          }}
        />
      </div>

      {/* Racing track lines */}
      <div className="absolute inset-0 opacity-20">
        <div className="absolute top-1/4 left-0 w-full h-px bg-gradient-to-r from-transparent via-red-600 to-transparent animate-pulse" />
        <div
          className="absolute top-2/4 left-0 w-full h-px bg-gradient-to-r from-transparent via-red-600 to-transparent animate-pulse"
          style={{ animationDelay: "0.3s" }}
        />
        <div
          className="absolute top-3/4 left-0 w-full h-px bg-gradient-to-r from-transparent via-red-600 to-transparent animate-pulse"
          style={{ animationDelay: "0.6s" }}
        />
      </div>

      {/* Logo */}
      <div className="relative z-10 animate-fadeIn">
        <PixelLogo size="large" />

        {/* Loading indicator */}
        <div className="mt-12 flex justify-center">
          <div className="flex gap-2">
            <div
              className="w-3 h-3 bg-red-600 rounded-sm animate-bounce"
              style={{ animationDelay: "0ms" }}
            />
            <div
              className="w-3 h-3 bg-red-600 rounded-sm animate-bounce"
              style={{ animationDelay: "150ms" }}
            />
            <div
              className="w-3 h-3 bg-red-600 rounded-sm animate-bounce"
              style={{ animationDelay: "300ms" }}
            />
          </div>
        </div>

        {/* Loading text */}
        <div
          className="mt-4 text-center text-zinc-500 tracking-widest"
          style={{ fontFamily: "monospace" }}
        >
          LOADING...
        </div>
      </div>

      <style>{`
        @keyframes slideGrid {
          0% {
            transform: translateY(0);
          }
          100% {
            transform: translateY(50px);
          }
        }
      `}</style>
    </div>
  );
}