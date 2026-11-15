import React, { useState } from 'react';
import { BookmarkPlus, Power, RotateCcw, Trash2 } from 'lucide-react';
import { AccentOption, BUILT_IN_EQ_PRESETS, EQ_BANDS, EqPreset } from '../data/novaLibrary';
import { LiquidGlassButton, LiquidGlassCard, LiquidGlassIconButton } from './LiquidGlass';

interface EqualizerViewProps {
  enabled: boolean;
  bandsDb: number[];
  preampDb: number;
  bassBoostPct: number;
  loudnessDb: number;
  activePresetName: string;
  customPresets: EqPreset[];
  spectrumBars: number[];
  isPlaying: boolean;
  accent: AccentOption;
  onToggleEnabled: () => void;
  onChangeBand: (index: number, db: number) => void;
  onChangePreamp: (db: number) => void;
  onChangeBassBoost: (pct: number) => void;
  onChangeLoudness: (db: number) => void;
  onApplyPreset: (preset: EqPreset) => void;
  onSaveCustomPreset: (name: string) => void;
  onDeleteCustomPreset: (name: string) => void;
  onReset: () => void;
}

export const EqualizerView: React.FC<EqualizerViewProps> = ({
  enabled,
  bandsDb,
  preampDb,
  bassBoostPct,
  loudnessDb,
  activePresetName,
  customPresets,
  spectrumBars,
  isPlaying,
  accent,
  onToggleEnabled,
  onChangeBand,
  onChangePreamp,
  onChangeBassBoost,
  onChangeLoudness,
  onApplyPreset,
  onSaveCustomPreset,
  onDeleteCustomPreset,
  onReset,
}) => {
  const [showSavePrompt, setShowSavePrompt] = useState(false);
  const [presetDraft, setPresetDraft] = useState('');

  // SVG frequency response spline
  const svgWidth = 600;
  const svgHeight = 120;
  const padX = 24;
  const padY = 16;
  const usableW = svgWidth - padX * 2;
  const usableH = svgHeight - padY * 2;

  const points = bandsDb.map((db, idx) => {
    const x = padX + (idx / Math.max(1, bandsDb.length - 1)) * usableW;
    const norm = (db + 12) / 24; // 0..1
    const y = padY + (1 - norm) * usableH;
    return { x, y };
  });

  const linePath = points.reduce((acc, pt, idx) => {
    if (idx === 0) return `M ${pt.x} ${pt.y}`;
    const prev = points[idx - 1];
    const cx = (prev.x + pt.x) / 2;
    return `${acc} C ${cx} ${prev.y}, ${cx} ${pt.y}, ${pt.x} ${pt.y}`;
  }, '');

  const midY = padY + usableH / 2;
  const areaPath =
    points.length > 0 ? `${linePath} L ${points[points.length - 1].x} ${midY} L ${points[0].x} ${midY} Z` : '';

  return (
    <div className="space-y-5">
      {/* Header Card */}
      <LiquidGlassCard highlighted={enabled} accentHex={accent.hex} className="p-5">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <h2 className="text-xl font-bold tracking-tight text-slate-900 dark:text-white">
              10-Band Liquid DSP Equalizer
            </h2>
            <p className="mt-0.5 text-sm text-slate-600 dark:text-slate-300">
              Web Audio BiquadFilterNode rack · Active preset: {activePresetName}
            </p>
          </div>

          <div className="flex items-center gap-2">
            <LiquidGlassIconButton label="Save Custom Preset" onClick={() => setShowSavePrompt(true)}>
              <BookmarkPlus className="h-4 w-4" />
            </LiquidGlassIconButton>
            <LiquidGlassIconButton label="Reset Equalizer" onClick={onReset}>
              <RotateCcw className="h-4 w-4" />
            </LiquidGlassIconButton>
            <LiquidGlassButton highlighted={enabled} accent={accent} onClick={onToggleEnabled}>
              <Power className="h-4 w-4" />
              <span>{enabled ? 'DSP On' : 'DSP Off'}</span>
            </LiquidGlassButton>
          </div>
        </div>

        {/* Presets Bar */}
        <div className="nova-scroll mt-4 flex items-center gap-2 overflow-x-auto pb-1">
          {BUILT_IN_EQ_PRESETS.map((preset) => {
            const active = activePresetName === preset.name;
            return (
              <button
                key={preset.name}
                type="button"
                onClick={() => onApplyPreset(preset)}
                style={
                  active
                    ? {
                        borderColor: accent.hex,
                        backgroundColor: `${accent.hex}24`,
                        color: accent.hex,
                      }
                    : undefined
                }
                className={`min-h-[40px] whitespace-nowrap shrink-0 rounded-xl border px-3.5 py-2 text-xs font-semibold transition-colors ${
                  active
                    ? ''
                    : 'border-slate-900/10 bg-slate-900/5 text-slate-700 hover:bg-slate-900/10 dark:border-white/10 dark:bg-white/[0.06] dark:text-slate-300 dark:hover:bg-white/10'
                }`}
              >
                {preset.name}
              </button>
            );
          })}
          {customPresets.map((preset) => {
            const active = activePresetName === preset.name;
            return (
              <div
                key={`custom-${preset.name}`}
                style={active ? { borderColor: accent.hex, backgroundColor: `${accent.hex}24` } : undefined}
                className="flex min-h-[40px] shrink-0 items-center gap-1 rounded-xl border border-slate-900/10 bg-slate-900/5 px-3 dark:border-white/10 dark:bg-white/[0.06]"
              >
                <button
                  type="button"
                  onClick={() => onApplyPreset(preset)}
                  style={active ? { color: accent.hex } : undefined}
                  className="whitespace-nowrap py-2 text-xs font-semibold text-slate-700 dark:text-slate-200"
                >
                  {preset.name}
                </button>
                <button
                  type="button"
                  aria-label={`Delete ${preset.name}`}
                  onClick={() => onDeleteCustomPreset(preset.name)}
                  className="p-1 text-slate-400 hover:text-rose-500"
                >
                  <Trash2 className="h-3.5 w-3.5" />
                </button>
              </div>
            );
          })}
        </div>
      </LiquidGlassCard>

      {/* Frequency Curve & 10-Band Vertical Sliders */}
      <LiquidGlassCard className="p-5">
        <div className="mb-4 flex items-center justify-between gap-4">
          <span className="text-sm font-semibold text-slate-900 dark:text-white">
            Frequency Response Curve
          </span>

          {/* Live Spectrum Meter */}
          <div className="flex h-7 items-end gap-1 rounded-xl border border-slate-900/10 bg-slate-900/5 px-2.5 py-1.5 dark:border-white/10 dark:bg-white/[0.05]">
            {spectrumBars.map((level, i) => (
              <div
                key={i}
                className="w-1.5 rounded-full transition-all duration-100"
                style={{
                  height: `${Math.round((isPlaying && enabled ? level : 0.2) * 18)}px`,
                  backgroundColor: enabled ? accent.hex : '#64748B',
                }}
              />
            ))}
          </div>
        </div>

        {/* SVG Curve */}
        <div className="overflow-hidden rounded-2xl border border-slate-900/10 bg-slate-950/5 p-2 dark:border-white/10 dark:bg-black/30">
          <svg viewBox={`0 0 ${svgWidth} ${svgHeight}`} className="h-28 w-full">
            <defs>
              <linearGradient id="novaEqFill" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stopColor={enabled ? accent.hex : '#64748B'} stopOpacity="0.4" />
                <stop offset="100%" stopColor={enabled ? accent.hex : '#64748B'} stopOpacity="0.02" />
              </linearGradient>
            </defs>
            <line
              x1={padX}
              y1={midY}
              x2={svgWidth - padX}
              y2={midY}
              stroke="currentColor"
              className="text-slate-400/40 dark:text-white/15"
              strokeDasharray="4 4"
            />
            {areaPath && <path d={areaPath} fill="url(#novaEqFill)" />}
            {linePath && (
              <path
                d={linePath}
                fill="none"
                stroke={enabled ? accent.hex : '#64748B'}
                strokeWidth="3"
                strokeLinecap="round"
              />
            )}
            {points.map((pt, i) => (
              <circle
                key={i}
                cx={pt.x}
                cy={pt.y}
                r="4"
                fill="#FFFFFF"
                stroke={enabled ? accent.hex : '#64748B'}
                strokeWidth="2.5"
              />
            ))}
          </svg>
        </div>

        {/* 10 Vertical Band Faders */}
        <div className="nova-scroll mt-5 grid grid-cols-5 gap-2.5 sm:grid-cols-10">
          {EQ_BANDS.map((band) => {
            const db = bandsDb[band.index] ?? 0;
            return (
              <div
                key={band.freq}
                className="flex flex-col items-center rounded-2xl border border-slate-900/10 bg-slate-900/[0.04] px-2 py-3 dark:border-white/10 dark:bg-white/[0.04]"
              >
                <span
                  style={enabled ? { color: accent.hex } : undefined}
                  className="font-mono text-[11px] font-bold tabular-nums text-slate-600 dark:text-slate-300"
                >
                  {db >= 0 ? `+${db.toFixed(1)}` : db.toFixed(1)}
                </span>

                <div className="my-3 flex h-36 items-center justify-center">
                  <input
                    type="range"
                    min={-12}
                    max={12}
                    step={0.5}
                    value={db}
                    disabled={!enabled}
                    aria-label={`${band.label} gain`}
                    onChange={(e) => onChangeBand(band.index, parseFloat(e.target.value))}
                    style={{
                      writingMode: 'vertical-lr',
                      direction: 'rtl',
                      accentColor: accent.hex,
                    }}
                    className="h-32 w-6 cursor-pointer accent-current disabled:opacity-40"
                  />
                </div>

                <span className="font-mono text-[10px] font-medium text-slate-500 dark:text-slate-400">
                  {band.label}
                </span>
              </div>
            );
          })}
        </div>
      </LiquidGlassCard>

      {/* Tone & Spatial FX Sliders */}
      <LiquidGlassCard className="p-5">
        <h3 className="text-base font-semibold text-slate-900 dark:text-white">Tone & Gain Stage</h3>
        <div className="mt-4 grid grid-cols-1 gap-5 md:grid-cols-3">
          <div className="rounded-2xl border border-slate-900/10 bg-slate-900/[0.04] p-4 dark:border-white/10 dark:bg-white/[0.04]">
            <div className="flex items-center justify-between text-sm">
              <span className="font-medium text-slate-800 dark:text-slate-200">Preamp Gain</span>
              <span style={{ color: accent.hex }} className="font-mono text-xs font-bold tabular-nums">
                {preampDb >= 0 ? `+${preampDb.toFixed(1)} dB` : `${preampDb.toFixed(1)} dB`}
              </span>
            </div>
            <input
              type="range"
              min={-12}
              max={12}
              step={0.5}
              value={preampDb}
              disabled={!enabled}
              onChange={(e) => onChangePreamp(parseFloat(e.target.value))}
              style={{ accentColor: accent.hex }}
              className="mt-3 w-full cursor-pointer"
            />
          </div>

          <div className="rounded-2xl border border-slate-900/10 bg-slate-900/[0.04] p-4 dark:border-white/10 dark:bg-white/[0.04]">
            <div className="flex items-center justify-between text-sm">
              <span className="font-medium text-slate-800 dark:text-slate-200">Sub-Bass Boost</span>
              <span style={{ color: accent.hex }} className="font-mono text-xs font-bold tabular-nums">
                {bassBoostPct}%
              </span>
            </div>
            <input
              type="range"
              min={0}
              max={100}
              step={5}
              value={bassBoostPct}
              disabled={!enabled}
              onChange={(e) => onChangeBassBoost(parseInt(e.target.value, 10))}
              style={{ accentColor: accent.hex }}
              className="mt-3 w-full cursor-pointer"
            />
          </div>

          <div className="rounded-2xl border border-slate-900/10 bg-slate-900/[0.04] p-4 dark:border-white/10 dark:bg-white/[0.04]">
            <div className="flex items-center justify-between text-sm">
              <span className="font-medium text-slate-800 dark:text-slate-200">Loudness Sparkle</span>
              <span style={{ color: accent.hex }} className="font-mono text-xs font-bold tabular-nums">
                +{loudnessDb.toFixed(1)} dB
              </span>
            </div>
            <input
              type="range"
              min={0}
              max={12}
              step={0.5}
              value={loudnessDb}
              disabled={!enabled}
              onChange={(e) => onChangeLoudness(parseFloat(e.target.value))}
              style={{ accentColor: accent.hex }}
              className="mt-3 w-full cursor-pointer"
            />
          </div>
        </div>
      </LiquidGlassCard>

      {showSavePrompt && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-md">
          <div className="w-full max-w-sm rounded-3xl border border-white/20 bg-[#14161F]/95 p-6 text-slate-100 shadow-2xl">
            <h3 className="text-lg font-bold">Save Custom EQ Preset</h3>
            <input
              type="text"
              value={presetDraft}
              onChange={(e) => setPresetDraft(e.target.value)}
              placeholder="e.g. Studio Headphones"
              className="mt-4 w-full rounded-xl border border-white/15 bg-white/10 px-3.5 py-2.5 text-sm text-white placeholder-slate-400 focus:outline-none"
            />
            <div className="mt-4 flex justify-end gap-2">
              <LiquidGlassButton onClick={() => setShowSavePrompt(false)}>Cancel</LiquidGlassButton>
              <LiquidGlassButton
                highlighted
                accent={accent}
                onClick={() => {
                  if (presetDraft.trim()) {
                    onSaveCustomPreset(presetDraft.trim());
                    setPresetDraft('');
                    setShowSavePrompt(false);
                  }
                }}
              >
                Save Preset
              </LiquidGlassButton>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
