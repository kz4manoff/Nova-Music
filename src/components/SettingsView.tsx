import React, { useEffect, useState } from 'react';
import {
  Check,
  Download,
  ExternalLink,
  FileJson,
  History,
  Moon,
  Palette,
  ShieldCheck,
  SlidersHorizontal,
  Sun,
  Timer,
} from 'lucide-react';
import { ACCENT_OPTIONS, AccentOption, ArtworkStyle, ThemeMode } from '../data/novaLibrary';
import { LiquidGlassButton, LiquidGlassCard } from './LiquidGlass';

interface UpdateHistoryItem {
  version: string;
  versionCode: number;
  date?: string;
  mandatory?: boolean;
  apkUrl?: string;
  sha256?: string;
  notes: string;
}

interface UpdateConfigManifest {
  latestVersion: string;
  versionCode: number;
  apkUrl: string;
  sha256?: string;
  releaseNotes: string;
  mandatoryUpdate: boolean;
  minimumSupportedVersion: number;
  publishedAt?: string;
  history?: UpdateHistoryItem[];
}

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

  const [updateManifest, setUpdateManifest] = useState<UpdateConfigManifest | null>(null);
  const [manifestError, setManifestError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    fetch('/update.json', { cache: 'no-store' })
      .then((res) => {
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        return res.json() as Promise<UpdateConfigManifest>;
      })
      .then((data) => {
        if (active) setUpdateManifest(data);
      })
      .catch(() => {
        if (active) setManifestError('Unable to load /update.json manifest.');
      });
    return () => {
      active = false;
    };
  }, []);

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
      {/* Android Releases & Update Feed */}
      <LiquidGlassCard highlighted accentHex={accent.hex} className="space-y-4 p-5">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-2.5">
            <ShieldCheck className="h-5 w-5" style={{ color: accent.hex }} />
            <div>
              <h3 className="text-base font-bold text-slate-900 dark:text-white">
                NOVA MUSIC for Android — Releases
              </h3>
              <p className="text-xs text-slate-600 dark:text-slate-400">
                Powered directly by <code className="font-mono">/update.json</code> &amp; GitHub Releases
              </p>
            </div>
          </div>

          <a
            href="/update.json"
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex min-h-[38px] items-center gap-1.5 rounded-xl border border-slate-900/10 bg-slate-900/5 px-3 py-1.5 font-mono text-xs font-semibold text-slate-700 transition-colors hover:bg-slate-900/10 dark:border-white/15 dark:bg-white/[0.06] dark:text-slate-200 dark:hover:bg-white/10"
          >
            <FileJson className="h-3.5 w-3.5" style={{ color: accent.hex }} />
            <span>update.json</span>
            <ExternalLink className="h-3 w-3 opacity-70" />
          </a>
        </div>

        {manifestError && (
          <p className="rounded-2xl border border-rose-500/30 bg-rose-500/10 px-4 py-3 text-xs text-rose-300">
            {manifestError}
          </p>
        )}

        {updateManifest && (
          <div className="space-y-4">
            <div className="flex flex-wrap items-center justify-between gap-4 rounded-2xl border border-slate-900/10 bg-slate-900/[0.04] p-4 dark:border-white/10 dark:bg-white/[0.04]">
              <div className="space-y-1">
                <div className="flex flex-wrap items-center gap-2">
                  <span className="font-mono text-base font-bold text-slate-900 dark:text-white">
                    v{updateManifest.latestVersion}
                  </span>
                  <span
                    style={{ backgroundColor: `${accent.hex}24`, color: accent.hex }}
                    className="rounded-lg px-2 py-0.5 font-mono text-[11px] font-bold"
                  >
                    BUILD {updateManifest.versionCode}
                  </span>
                  {updateManifest.mandatoryUpdate && (
                    <span className="rounded-lg bg-rose-500/20 px-2 py-0.5 font-mono text-[11px] font-bold text-rose-400">
                      REQUIRED
                    </span>
                  )}
                </div>
                {updateManifest.publishedAt && (
                  <p className="text-xs text-slate-500 dark:text-slate-400">
                    Released {updateManifest.publishedAt} · Min supported build {updateManifest.minimumSupportedVersion}
                  </p>
                )}
              </div>

              <a
                href={updateManifest.apkUrl}
                target="_blank"
                rel="noopener noreferrer"
                style={{
                  background: `linear-gradient(135deg, ${accent.glow}, ${accent.hex}, ${accent.deep})`,
                  boxShadow: `0 10px 26px -8px ${accent.hex}88, inset 0 1px 0 rgba(255,255,255,0.45)`,
                }}
                className="inline-flex min-h-[44px] items-center justify-center gap-2 rounded-2xl border border-white/40 px-4 py-2.5 text-sm font-semibold text-[#0B0C10] transition-transform active:scale-95"
              >
                <Download className="h-4 w-4" />
                <span>Download APK (v{updateManifest.latestVersion})</span>
              </a>
            </div>

            <div className="rounded-2xl border border-slate-900/10 bg-slate-900/[0.03] p-4 dark:border-white/10 dark:bg-white/[0.03]">
              <p className="text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">
                Release Notes
              </p>
              <p className="mt-1.5 whitespace-pre-line text-sm text-slate-700 dark:text-slate-200">
                {updateManifest.releaseNotes}
              </p>
              <div className="mt-3 border-t border-slate-900/10 pt-2.5 dark:border-white/10">
                <p className="font-mono text-[11px] text-slate-500 dark:text-slate-400 break-all">
                  SHA-256:{' '}
                  {updateManifest.sha256 && updateManifest.sha256.trim().length === 64
                    ? updateManifest.sha256
                    : 'Generated automatically on signed GitHub Release build'}
                </p>
              </div>
            </div>

            {updateManifest.history && updateManifest.history.length > 0 && (
              <div className="space-y-2.5">
                <div className="flex items-center gap-2 text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">
                  <History className="h-3.5 w-3.5" style={{ color: accent.hex }} />
                  <span>Update History</span>
                </div>
                <div className="space-y-2">
                  {updateManifest.history.map((entry) => (
                    <div
                      key={`${entry.version}-${entry.versionCode}`}
                      className="rounded-2xl border border-slate-900/10 bg-slate-900/[0.03] px-4 py-3 dark:border-white/10 dark:bg-white/[0.03]"
                    >
                      <div className="flex flex-wrap items-center justify-between gap-2">
                        <span className="font-mono text-xs font-bold text-slate-900 dark:text-white">
                          v{entry.version} (Build {entry.versionCode})
                        </span>
                        <div className="flex items-center gap-2">
                          {entry.date && (
                            <span className="font-mono text-[11px] text-slate-500 dark:text-slate-400">
                              {entry.date}
                            </span>
                          )}
                          {entry.apkUrl && (
                            <a
                              href={entry.apkUrl}
                              target="_blank"
                              rel="noopener noreferrer"
                              style={{ color: accent.hex }}
                              className="text-xs font-semibold hover:underline"
                            >
                              APK
                            </a>
                          )}
                        </div>
                      </div>
                      <p className="mt-1 text-xs text-slate-600 dark:text-slate-300">{entry.notes}</p>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}
      </LiquidGlassCard>
    </div>
  );
};
