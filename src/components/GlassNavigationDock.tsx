import React from 'react';
import { Home, LayoutGrid, Pause, Play, Search, Settings, SkipForward } from 'lucide-react';
import { AccentOption, AppTab, Track } from '../data/novaLibrary';

export const RecitersIcon: React.FC<{ className?: string; strokeWidth?: number }> = ({
  className = 'h-5 w-5',
  strokeWidth = 1.75,
}) => (
  <svg
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth={strokeWidth}
    strokeLinecap="round"
    strokeLinejoin="round"
    className={className}
    aria-hidden="true"
  >
    {/* Person head */}
    <circle cx="9" cy="7.5" r="3.2" />
    {/* Person shoulders */}
    <path d="M3.5 19.5c0-3 2.4-5 5.5-5s5.5 2 5.5 5" />
    {/* Sound waves */}
    <path d="M16.2 8.2a3.8 3.8 0 0 1 0 5.6" />
    <path d="M18.9 5.8a7.2 7.2 0 0 1 0 10.4" />
  </svg>
);

interface GlassNavigationDockProps {
  activeTab: AppTab;
  currentTrack: Track;
  isPlaying: boolean;
  progressFraction: number;
  accent: AccentOption;
  theme: 'dark' | 'light';
  onSelectTab: (tab: AppTab) => void;
  onOpenNowPlaying: () => void;
  onTogglePlay: () => void;
  onNext: () => void;
}

export const GlassNavigationDock: React.FC<GlassNavigationDockProps> = ({
  activeTab,
  currentTrack,
  isPlaying,
  progressFraction,
  accent,
  theme,
  onSelectTab,
  onOpenNowPlaying,
  onTogglePlay,
  onNext,
}) => {
  const isLight = theme === 'light';
  const clampedPct = Math.max(0, Math.min(100, progressFraction * 100));

  const navItems: {
    id: AppTab;
    label: string;
    renderIcon: (active: boolean) => React.ReactNode;
  }[] = [
    {
      id: 'home',
      label: 'Home',
      renderIcon: (active) => (
        <Home className="h-[19px] w-[19px]" strokeWidth={active ? 2.1 : 1.75} />
      ),
    },
    {
      id: 'reciters',
      label: 'Reciters',
      renderIcon: (active) => (
        <RecitersIcon className="h-[19px] w-[19px]" strokeWidth={active ? 2.1 : 1.75} />
      ),
    },
    {
      id: 'playlists',
      label: 'Playlists',
      renderIcon: (active) => (
        <LayoutGrid className="h-[19px] w-[19px]" strokeWidth={active ? 2.1 : 1.75} />
      ),
    },
    {
      id: 'settings',
      label: 'Settings',
      renderIcon: (active) => (
        <Settings className="h-[19px] w-[19px]" strokeWidth={active ? 2.1 : 1.75} />
      ),
    },
    {
      id: 'search',
      label: 'Search',
      renderIcon: (active) => (
        <Search className="h-[19px] w-[19px]" strokeWidth={active ? 2.1 : 1.75} />
      ),
    },
  ];

  // Keep 'library' highlighted under 'reciters' and 'equalizer' under 'settings' when viewing sub-routes
  const resolvedTab: AppTab =
    activeTab === 'library'
      ? 'reciters'
      : activeTab === 'equalizer'
        ? 'settings'
        : activeTab;

  return (
    <div className="pointer-events-none fixed inset-x-0 bottom-0 z-40 px-3 pb-3 sm:px-6 sm:pb-4">
      <div className="mx-auto flex w-full max-w-[440px] flex-col gap-2.5 sm:max-w-[520px]">
        {/* 1. Floating Pill-Shaped Mini-Player Bar */}
        <div
          className={`pointer-events-auto relative flex h-[58px] items-center justify-between gap-2 overflow-hidden rounded-full border pl-2 pr-2 backdrop-blur-2xl backdrop-saturate-150 transition-all duration-200 ${
            isLight
              ? 'border-slate-900/12 bg-white/80 text-slate-900'
              : 'border-white/[0.22] bg-[#0B0B0E]/68 text-white'
          }`}
          style={{
            boxShadow: isLight
              ? '0 14px 32px -10px rgba(15,23,42,0.18), inset 0 1px 0 0 rgba(255,255,255,0.85)'
              : '0 18px 40px -10px rgba(0,0,0,0.9), inset 0 1px 0 0 rgba(255,255,255,0.24), inset 0 -1px 0 0 rgba(255,255,255,0.06)',
          }}
        >
          {/* Subtle Top Glass Specular Reflection */}
          <div
            aria-hidden="true"
            className="pointer-events-none absolute inset-x-4 top-0 h-1/2 rounded-full bg-gradient-to-b from-white/[0.12] to-transparent"
          />

          {/* Track Info Trigger (Opens Full Now Playing) */}
          <button
            type="button"
            onClick={onOpenNowPlaying}
            className="relative z-10 flex min-w-0 flex-1 items-center gap-3 rounded-full py-1 pl-0.5 pr-2 text-left transition-opacity hover:opacity-90 focus-visible:outline-none"
          >
            <img
              src={currentTrack.artwork}
              alt={currentTrack.album}
              referrerPolicy="no-referrer"
              className="h-10 w-10 shrink-0 rounded-full border border-white/25 object-cover shadow-md"
            />
            <div className="min-w-0 flex-1">
              <p className="truncate text-[13px] font-semibold leading-tight tracking-tight">
                {currentTrack.title}
              </p>
              <p
                className={`mt-0.5 truncate text-[11px] leading-tight ${
                  isLight ? 'text-slate-600' : 'text-white/65'
                }`}
              >
                {currentTrack.artist}
              </p>
            </div>
          </button>

          {/* Play & Next Controls Aligned to Right */}
          <div className="relative z-10 flex shrink-0 items-center gap-1 pr-0.5">
            <button
              type="button"
              aria-label={isPlaying ? 'Pause' : 'Play'}
              onClick={onTogglePlay}
              className={`flex h-11 w-11 items-center justify-center rounded-full border transition-all duration-150 active:scale-90 focus-visible:outline-none ${
                isLight
                  ? 'border-slate-900/12 bg-slate-900/8 text-slate-900 hover:bg-slate-900/15'
                  : 'border-white/[0.16] bg-white/[0.08] text-white hover:bg-white/[0.16]'
              }`}
              style={{
                boxShadow: isLight
                  ? undefined
                  : 'inset 0 1px 0 0 rgba(255,255,255,0.2)',
              }}
            >
              {isPlaying ? (
                <Pause className="h-[18px] w-[18px] fill-current" />
              ) : (
                <Play className="ml-0.5 h-[18px] w-[18px] fill-current" />
              )}
            </button>

            <button
              type="button"
              aria-label="Next Track"
              onClick={onNext}
              className={`flex h-11 w-11 items-center justify-center rounded-full transition-all duration-150 active:scale-90 focus-visible:outline-none ${
                isLight
                  ? 'text-slate-900 hover:bg-slate-900/10'
                  : 'text-white hover:bg-white/[0.12]'
              }`}
            >
              <SkipForward className="h-[18px] w-[18px] fill-current" />
            </button>
          </div>

          {/* Hairline Bottom Progress Trace */}
          <div
            aria-hidden="true"
            className="pointer-events-none absolute inset-x-5 bottom-0 h-[1.5px] overflow-hidden rounded-full bg-white/10"
          >
            <div
              className="h-full transition-[width] duration-150"
              style={{
                width: `${clampedPct}%`,
                background: isLight
                  ? accent.hex
                  : `linear-gradient(90deg, rgba(255,255,255,0.45), ${accent.glow}, #ffffff)`,
              }}
            />
          </div>
        </div>

        {/* 2. Floating Rounded-Rectangle Bottom Navigation Bar */}
        <nav
          aria-label="Primary navigation"
          className={`pointer-events-auto relative overflow-hidden rounded-[24px] border px-1.5 py-1.5 backdrop-blur-2xl backdrop-saturate-150 transition-all duration-200 ${
            isLight
              ? 'border-slate-900/12 bg-white/82 text-slate-900'
              : 'border-white/[0.18] bg-[#0B0B0E]/74 text-white'
          }`}
          style={{
            boxShadow: isLight
              ? '0 16px 36px -12px rgba(15,23,42,0.2), inset 0 1px 0 0 rgba(255,255,255,0.85)'
              : '0 22px 48px -12px rgba(0,0,0,0.95), inset 0 1px 0 0 rgba(255,255,255,0.2)',
          }}
        >
          {/* Subtle Frosted Specular Top Reflection */}
          <div
            aria-hidden="true"
            className="pointer-events-none absolute inset-x-3 top-0 h-1/2 rounded-t-[20px] bg-gradient-to-b from-white/[0.09] to-transparent"
          />

          <div className="relative z-10 grid grid-cols-5 items-center">
            {navItems.map((item) => {
              const isActive = resolvedTab === item.id;
              return (
                <button
                  key={item.id}
                  type="button"
                  onClick={() => onSelectTab(item.id)}
                  aria-current={isActive ? 'page' : undefined}
                  className="group flex min-h-[54px] w-full items-center justify-center focus-visible:outline-none"
                >
                  <span
                    className={`flex h-[52px] w-[54px] flex-col items-center justify-center gap-1 rounded-full transition-all duration-200 ${
                      isActive
                        ? isLight
                          ? 'border border-slate-900/12 bg-slate-900/[0.08] text-slate-950'
                          : 'border border-white/[0.16] bg-white/[0.14] text-white'
                        : isLight
                          ? 'text-slate-600 group-hover:bg-slate-900/[0.05] group-hover:text-slate-950'
                          : 'text-white/80 group-hover:bg-white/[0.07] group-hover:text-white'
                    }`}
                    style={
                      isActive && !isLight
                        ? {
                            boxShadow:
                              '0 6px 16px -6px rgba(0,0,0,0.65), inset 0 1px 0 0 rgba(255,255,255,0.22)',
                          }
                        : undefined
                    }
                  >
                    {item.renderIcon(isActive)}
                    <span className="whitespace-nowrap text-[10px] font-medium leading-none tracking-tight">
                      {item.label}
                    </span>
                  </span>
                </button>
              );
            })}
          </div>
        </nav>
      </div>
    </div>
  );
};
