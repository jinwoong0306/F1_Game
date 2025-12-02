import React from 'react';
import logoImage from 'figma:asset/d09a2e10cab4bfd1684a13e2075c97a08eb81967.png';

interface PixelLogoProps {
  size?: 'small' | 'medium' | 'large';
}

export function PixelLogo({ size = 'medium' }: PixelLogoProps) {
  const sizes = {
    small: 'w-[300px]',
    medium: 'w-[500px]',
    large: 'w-[600px]',
  };

  return (
    <div className={`${sizes[size]} max-w-full mx-auto`}>
      <img 
        src={logoImage} 
        alt="Pixel Formula Racing" 
        className="w-full h-auto"
      />
    </div>
  );
}