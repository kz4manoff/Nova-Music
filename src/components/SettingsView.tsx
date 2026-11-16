import React from 'react';
import {
  Check,
  Moon,
  Palette,
  SlidersHorizontal,
  Sun,
  Timer,
} from 'lucide-react';
import { ACCENT_OPTIONS, AccentOption, ArtworkStyle, ThemeMode } from '../data/novaLibrary';
import { LiquidGlassButton, LiquidGlassCard } from './LiquidGlass';

interface SettingsViewProps {
  theme: ThemeMode;
  accent: AccentOption;
  artworkStyle: ArtworkStyle;
  gapless: boolean;
  sleepMinutes: number | null;
  onChangeTheme: (mode: ThemeMode) => void;
  onChangeAccent: (option: AccentOption) => void;
  onChangeArtworkStyle: (style: ArtworkStyle) => void;
  onToggleGapless: () => void;
  onSetSleepTimer: (minutes: number | null) => void;
  onOpenEqualizer: () => void;
}

export const SettingsView: React.FC<SettingsViewProps> = ({
  theme,
  accent,
  artworkStyle,
  gapless,
  sleepMinutes,
  onChangeTheme,
  onChangeAccent,
  onChangeArtworkStyle,
  onToggleGapless,
  onSetSleepTimer,
  onOpenEqualizer,
}) => {
  const artworkOptions: { id: ArtworkStyle; label: string }[] = [
    { id: 'ROUNDED_CARD', label: 'Rounded Glass Card' },
    { id: 'ROTATING_VINYL', label: 'Rotating Vinyl Disc' },
    { id: 'IMMERSIVE_GLOW', label: 'Immersive Aura Glow' },
    { id: 'EDITORIAL_SQUARE', label: 'Editorial Studio Frame' },
  ];

  const sleepOptions: (number | null)[] = [null, 15, 30, 60];

  return (
    <div className="space-y-5">
      <LiquidGlassCard className="p-5">
        <h2 className="text-xl font-bold tracking-tight text-slate-900 dark:text-white">Settings</h2>
        <p className="mt-0.5 text-sm text-slate-600 dark:text-slate-300">
          {theme === 'dark' ? 'Dark Liquid Glass' : 'Light Liquid Glass'} · {accent.label}
        </p>
      </LiquidGlassCard>

      {/* Theme & Accent */}
      <LiquidGlassCard className="space-y-5 p-5">
        <div className="flex items-center gap-2.5 text-sm font-semibold text-slate-900 dark:text-white">
          <Palette className="h-4 w-4" style={{ color: accent.hex }} />
          <span>Liquid Glass Theme & Refraction Tint</span>
        </div>

        <div className="grid grid-cols-2 gap-3">
          <button
            type="button"
            onClick={() => onChangeTheme('dark')}
            style={
              theme === 'dark'
                ? { borderColor: accent.hex, backgroundColor: `${accent.hex}22`, color: accent.hex }
                : undefined
            }
            className={`flex min-h-[48px] items-center justify-center gap-2 rounded-2xl border px-4 py-3 text-sm font-semibold transition-colors ${
              theme === 'dark'
                ? ''
                : 'border-slate-900/10 bg-slate-900/5 text-slate-700 hover:bg-slate-900/10 dark:border-white/10 dark:bg-white/[0.05] dark:text-slate-300'
            }`}
          >
            <Moon className="h-4 w-4" />
            <span>Dark Liquid Glass</span>
          </button>

          <button
            type="button"
            onClick={() => onChangeTheme('light')}
            style={
              theme === 'light'
                ? { borderColor: accent.hex, backgroundColor: `${accent.hex}22`, color: accent.hex }
                : undefined
            }
            className={`flex min-h-[48px] items-center justify-center gap-2 rounded-2xl border px-4 py-3 text-sm font-semibold transition-colors ${
              theme === 'light'
                ? ''
                : 'border-slate-900/10 bg-slate-900/5 text-slate-700 hover:bg-slate-900/10 dark:border-white/10 dark:bg-white/[0.05] dark:text-slate-300'
            }`}
          >
            <Sun className="h-4 w-4" />
            <span>Light Liquid Glass</span>
          </button>
        </div>

        <div>
          <p className="mb-3 text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">
            Accent Color
          </p>
          <div className="flex flex-wrap items-center gap-3">
            {ACCENT_OPTIONS.map((option) => {
              const selected = accent.id === option.id;
              return (
                <button
                  key={option.id}
                  type="button"
                  aria-label={option.label}
                  onClick={() => onChangeAccent(option)}
                  style={{
                    backgroundColor: option.hex,
                    boxShadow: selected ? `0 0 0 3px ${option.hex}44, 0 8px 18px -4px ${option.hex}` : undefined,
                  }}
                  className="flex h-11 w-11 items-center justify-center rounded-full border-2 border-white/60 transition-transform hover:scale-105 active:scale-95"
                >
                  {selected && <Check className="h-5 w-5 text-[#08090C]" />}
                </button>
              );
            })}
          </div>
        </div>
      </LiquidGlassCard>

      {/* Artwork Style */}
      <LiquidGlassCard className="space-y-4 p-5">
        <p className="text-sm font-semibold text-slate-900 dark:text-white">Now Playing Artwork Style</p>
        <div className="grid grid-cols-1 gap-2.5 sm:grid-cols-2">
          {artworkOptions.map((opt) => {
            const selected = artworkStyle === opt.id;
            return (
              <button
                key={opt.id}
                type="button"
                onClick={() => onChangeArtworkStyle(opt.id)}
                style={
                  selected
                    ? { borderColor: accent.hex, backgroundColor: `${accent.hex}20`, color: accent.hex }
                    : undefined
                }
                className={`flex min-h-[46px] items-center justify-between rounded-2xl border px-4 py-2.5 text-sm font-medium transition-colors ${
                  selected
                    ? 'font-semibold'
                    : 'border-slate-900/10 bg-slate-900/[0.04] text-slate-700 hover:bg-slate-900/10 dark:border-white/10 dark:bg-white/[0.04] dark:text-slate-300'
                }`}
              >
                <span>{opt.label}</span>
                {selected && <Check className="h-4 w-4" />}
              </button>
            );
          })}
        </div>
      </LiquidGlassCard>

      {/* Playback & DSP */}
      <LiquidGlassCard className="space-y-4 p-5">
        <div className="flex items-center justify-between gap-4">
          <div>
            <p className="text-sm font-semibold text-slate-900 dark:text-white">Gapless Playback</p>
            <p className="text-xs text-slate-600 dark:text-slate-400">
              Smooth transitions between consecutive queue tracks
            </p>
          </div>
          <button
            type="button"
            role="switch"
            aria-checked={gapless}
            onClick={onToggleGapless}
            style={gapless ? { backgroundColor: accent.hex } : undefined}
            className={`relative h-7 w-12 shrink-0 rounded-full transition-colors ${
              gapless ? '' : 'bg-slate-300 dark:bg-white/20'
            }`}
          >
            <span
              className={`block h-5 w-5 translate-y-1 rounded-full bg-white shadow transition-transform ${
                gapless ? 'translate-x-6' : 'translate-x-1'
              }`}
            />
          </button>
        </div>

        <div className="border-t border-slate-900/10 pt-4 dark:border-white/10">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div className="flex items-center gap-2">
              <Timer className="h-4 w-4" style={{ color: accent.hex }} />
              <span className="text-sm font-semibold text-slate-900 dark:text-white">Sleep Timer</span>
            </div>
            <div className="flex items-center gap-1.5">
              {sleepOptions.map((mins) => {
                const active = sleepMinutes === mins;
                return (
                  <button
                    key={String(mins)}
                    type="button"
                    onClick={() => onSetSleepTimer(mins)}
                    style={
                      active
                        ? { borderColor: accent.hex, backgroundColor: `${accent.hex}22`, color: accent.hex }
                        : undefined
                    }
                    className={`min-h-[38px] rounded-xl border px-3 py-1.5 text-xs font-semibold transition-colors ${
                      active
                        ? ''
                        : 'border-slate-900/10 bg-slate-900/5 text-slate-600 dark:border-white/10 dark:bg-white/[0.05] dark:text-slate-300'
                    }`}
                  >
                    {mins === null ? 'Off' : `${mins}m`}
                  </button>
                );
              })}
            </div>
          </div>
        </div>

        <div className="border-t border-slate-900/10 pt-4 dark:border-white/10">
          <LiquidGlassButton highlighted accent={accent} onClick={onOpenEqualizer} className="w-full sm:w-auto">
            <SlidersHorizontal className="h-4 w-4" />
            <span>Open 10-Band Equalizer DSP</span>
          </LiquidGlassButton>
        </div>
      </LiquidGlassCard>
    </div>
  );
};
