import React, { useRef, useState } from 'react';
import { AccentOption } from '../data/novaLibrary';

export const LiquidGlassBackdrop: React.FC<{
  artwork: string;
  isPlaying: boolean;
  accent: AccentOption;
  theme: 'dark' | 'light';
  intense?: boolean;
}> = ({ artwork, isPlaying, accent, theme, intense = false }) => {
  const isLight = theme === 'light';

  return (
    <div
      className={`pointer-events-none fixed inset-0 -z-10 overflow-hidden transition-colors duration-300 ${
        isLight ? 'bg-[#F1F4F9]' : 'bg-[#050507]'
      }`}
      aria-hidden="true"
    >
      {/* Live Blurred Album Artwork Layer */}
      <img
        src={artwork}
        alt=""
        referrerPolicy="no-referrer"
        className="absolute inset-0 h-full w-full scale-125 object-cover transition-all duration-700"
        style={{
          filter: `blur(${intense ? 78 : 68}px) saturate(${isPlaying ? 1.45 : 1.15})`,
          opacity: isLight ? (intense ? 0.34 : 0.24) : intense ? 0.48 : 0.34,
        }}
      />

      {/* Ambient Radial Light Refraction Orbs */}
      <div
        className="absolute -left-24 -top-24 h-[460px] w-[460px] rounded-full transition-transform duration-1000"
        style={{
          background: `radial-gradient(circle, ${accent.hex}${isLight ? '38' : '42'} 0%, transparent 70%)`,
          transform: isPlaying ? 'scale(1.12)' : 'scale(0.95)',
        }}
      />
      <div
        className="absolute -bottom-28 -right-20 h-[440px] w-[440px] rounded-full transition-transform duration-1000"
        style={{
          background: `radial-gradient(circle, ${accent.glow}${isLight ? '30' : '2A'} 0%, transparent 70%)`,
          transform: isPlaying ? 'scale(1.08)' : 'scale(0.92)',
        }}
      />

      {/* Atmospheric Readability Veil */}
      <div
        className="absolute inset-0"
        style={{
          background: isLight
            ? 'linear-gradient(180deg, rgba(255,255,255,0.72) 0%, rgba(241,244,249,0.68) 50%, rgba(241,244,249,0.92) 100%)'
            : 'linear-gradient(180deg, rgba(6,6,9,0.46) 0%, rgba(6,6,9,0.58) 52%, rgba(5,5,7,0.88) 100%)',
        }}
      />
    </div>
  );
};

export const LiquidGlassCard: React.FC<{
  children: React.ReactNode;
  className?: string;
  highlighted?: boolean;
  accentHex?: string;
  onClick?: () => void;
}> = ({ children, className = '', highlighted = false, accentHex = '#FF8A32', onClick }) => {
  const interactive = Boolean(onClick);
  const style: React.CSSProperties = highlighted
    ? {
        borderColor: `${accentHex}88`,
        boxShadow: `0 14px 36px -12px ${accentHex}40, inset 0 1px 0 0 rgba(255,255,255,0.28)`,
      }
    : {
        boxShadow: '0 14px 36px -14px rgba(0, 0, 0, 0.35), inset 0 1px 0 0 rgba(255, 255, 255, 0.22)',
      };

  const baseClasses = `relative overflow-hidden rounded-3xl border backdrop-blur-2xl transition-all duration-200 ${
    highlighted
      ? 'bg-white/85 dark:bg-[#1E202A]/78'
      : 'bg-white/75 border-white/80 dark:bg-[#14151D]/68 dark:border-white/15'
  } ${interactive ? 'cursor-pointer hover:bg-white/90 dark:hover:bg-[#1B1D27]/82 active:scale-[0.985]' : ''} ${className}`;

  if (onClick) {
    return (
      <div
        role="button"
        tabIndex={0}
        onClick={onClick}
        onKeyDown={(e) => {
          if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();
            onClick();
          }
        }}
        style={style}
        className={baseClasses}
      >
        {/* Subtle upper specular glass streak */}
        <div
          aria-hidden="true"
          className="pointer-events-none absolute inset-x-0 top-0 h-1/3 bg-gradient-to-b from-white/30 to-transparent dark:from-white/[0.08]"
        />
        <div className="relative z-10 h-full">{children}</div>
      </div>
    );
  }

  return (
    <div style={style} className={baseClasses}>
      <div
        aria-hidden="true"
        className="pointer-events-none absolute inset-x-0 top-0 h-1/3 bg-gradient-to-b from-white/30 to-transparent dark:from-white/[0.08]"
      />
      <div className="relative z-10 h-full">{children}</div>
    </div>
  );
};

export const LiquidGlassButton: React.FC<{
  children: React.ReactNode;
  onClick: () => void;
  highlighted?: boolean;
  accent?: AccentOption;
  disabled?: boolean;
  className?: string;
}> = ({ children, onClick, highlighted = false, accent, disabled = false, className = '' }) => {
  const accentHex = accent?.hex ?? '#FF8A32';
  const accentGlow = accent?.glow ?? '#FFB36B';
  const accentDeep = accent?.deep ?? '#D95F0E';

  return (
    <button
      type="button"
      disabled={disabled}
      onClick={onClick}
      style={
        highlighted
          ? {
              background: `linear-gradient(135deg, ${accentGlow}, ${accentHex}, ${accentDeep})`,
              boxShadow: `0 10px 26px -8px ${accentHex}88, inset 0 1px 0 rgba(255,255,255,0.45)`,
            }
          : {
              boxShadow: '0 8px 20px -10px rgba(0,0,0,0.3), inset 0 1px 0 rgba(255,255,255,0.28)',
            }
      }
      className={`inline-flex min-h-[44px] items-center justify-center gap-2 whitespace-nowrap shrink-0 rounded-2xl px-4 py-2.5 text-sm font-semibold transition-all duration-150 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 active:scale-95 disabled:opacity-45 disabled:pointer-events-none ${
        highlighted
          ? 'border border-white/40 text-[#0B0C10]'
          : 'border border-white/80 bg-white/75 text-slate-900 hover:bg-white/95 dark:border-white/15 dark:bg-[#1A1C26]/75 dark:text-slate-100 dark:hover:bg-[#232634]/90 backdrop-blur-xl'
      } ${className}`}
    >
      {children}
    </button>
  );
};

export const LiquidGlassIconButton: React.FC<{
  children: React.ReactNode;
  label: string;
  onClick: () => void;
  active?: boolean;
  accentHex?: string;
  sizeClass?: string;
  className?: string;
}> = ({
  children,
  label,
  onClick,
  active = false,
  accentHex = '#FF8A32',
  sizeClass = 'h-11 w-11',
  className = '',
}) => {
  return (
    <button
      type="button"
      aria-label={label}
      title={label}
      onClick={onClick}
      style={
        active
          ? {
              background: `linear-gradient(135deg, ${accentHex}44, ${accentHex}1F)`,
              borderColor: `${accentHex}BB`,
              color: accentHex,
              boxShadow: `0 8px 20px -6px ${accentHex}55, inset 0 1px 0 rgba(255,255,255,0.35)`,
            }
          : {
              boxShadow: 'inset 0 1px 0 rgba(255,255,255,0.28)',
            }
      }
      className={`inline-flex ${sizeClass} shrink-0 items-center justify-center rounded-full border backdrop-blur-xl transition-all duration-150 focus-visible:outline-none focus-visible:ring-2 active:scale-90 ${
        active
          ? ''
          : 'border-white/80 bg-white/75 text-slate-900 hover:bg-white dark:border-white/15 dark:bg-[#191B25]/75 dark:text-slate-100 dark:hover:bg-[#242736]'
      } ${className}`}
    >
      {children}
    </button>
  );
};

export const LiquidGlassSlider: React.FC<{
  progress: number; // 0..1
  accent: AccentOption;
  onSeekFraction: (fraction: number) => void;
  ariaLabel?: string;
}> = ({ progress, accent, onSeekFraction, ariaLabel = 'Track progress' }) => {
  const trackRef = useRef<HTMLDivElement>(null);
  const [dragging, setDragging] = useState(false);
  const clamped = Math.max(0, Math.min(1, progress));

  const computeFraction = (clientX: number) => {
    const rect = trackRef.current?.getBoundingClientRect();
    if (!rect || rect.width <= 0) return clamped;
    return Math.max(0, Math.min(1, (clientX - rect.left) / rect.width));
  };

  return (
    <div
      ref={trackRef}
      role="slider"
      tabIndex={0}
      aria-label={ariaLabel}
      aria-valuemin={0}
      aria-valuemax={100}
      aria-valuenow={Math.round(clamped * 100)}
      onPointerDown={(e) => {
        setDragging(true);
        e.currentTarget.setPointerCapture(e.pointerId);
        onSeekFraction(computeFraction(e.clientX));
      }}
      onPointerMove={(e) => {
        if (!dragging) return;
        onSeekFraction(computeFraction(e.clientX));
      }}
      onPointerUp={(e) => {
        if (dragging) {
          onSeekFraction(computeFraction(e.clientX));
          setDragging(false);
        }
      }}
      onKeyDown={(e) => {
        if (e.key === 'ArrowRight') onSeekFraction(Math.min(1, clamped + 0.05));
        if (e.key === 'ArrowLeft') onSeekFraction(Math.max(0, clamped - 0.05));
      }}
      className="group relative flex h-10 w-full cursor-pointer touch-none items-center select-none"
    >
      {/* Glass Recessed Track */}
      <div
        className={`relative w-full overflow-hidden rounded-full transition-all duration-150 ${
          dragging ? 'h-3' : 'h-2 group-hover:h-2.5'
        } bg-slate-900/12 dark:bg-white/15`}
      >
        <div
          className="h-full rounded-full transition-[width] duration-75"
          style={{
            width: `${clamped * 100}%`,
            background: `linear-gradient(90deg, ${accent.deep}, ${accent.glow}, ${accent.hex})`,
          }}
        />
      </div>

      {/* Luminous Glass Thumb Orb */}
      <div
        className={`pointer-events-none absolute top-1/2 -translate-x-1/2 -translate-y-1/2 rounded-full bg-white transition-transform duration-150 ${
          dragging ? 'h-5 w-5 scale-110' : 'h-4 w-4 group-hover:scale-110'
        }`}
        style={{
          left: `${clamped * 100}%`,
          boxShadow: `0 0 0 4px ${accent.hex}55, 0 4px 12px rgba(0,0,0,0.4)`,
        }}
      />
    </div>
  );
};
