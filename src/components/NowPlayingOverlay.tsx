import React, { useMemo, useState } from 'react';
import {
  ChevronDown,
  Heart,
  Info,
  ListMusic,
  Mic2,
  Pause,
  Play,
  Plus,
  Repeat,
  Repeat1,
  Shuffle,
  SkipBack,
  SkipForward,
  SlidersHorizontal,
  Trash2,
  X,
} from 'lucide-react';
import {
  AccentOption,
  ArtworkStyle,
  Playlist,
  RepeatMode,
  Track,
  formatTime,
} from '../data/novaLibrary';
import { LiquidGlassButton, LiquidGlassCard, LiquidGlassIconButton, LiquidGlassSlider } from './LiquidGlass';

interface NowPlayingOverlayProps {
  track: Track;
  isPlaying: boolean;
  currentTime: number;
  duration: number;
  shuffle: boolean;
  repeatMode: RepeatMode;
  isFavorite: boolean;
  isEqEnabled: boolean;
  artworkStyle: ArtworkStyle;
  accent: AccentOption;
  queue: Track[];
  playlists: Playlist[];
  onClose: () => void;
  onTogglePlay: () => void;
  onPrev: () => void;
  onNext: () => void;
  onSeek: (sec: number) => void;
  onToggleShuffle: () => void;
  onCycleRepeat: () => void;
  onToggleFavorite: () => void;
  onChangeArtworkStyle: (style: ArtworkStyle) => void;
  onOpenEqualizer: () => void;
  onSelectQueueTrack: (trackId: string) => void;
  onRemoveFromQueue: (trackId: string) => void;
  onAddToPlaylist: (playlistId: string, trackId: string) => void;
}

export const NowPlayingOverlay: React.FC<NowPlayingOverlayProps> = ({
  track,
  isPlaying,
  currentTime,
  duration,
  shuffle,
  repeatMode,
  isFavorite,
  isEqEnabled,
  artworkStyle,
  accent,
  queue,
  playlists,
  onClose,
  onTogglePlay,
  onPrev,
  onNext,
  onSeek,
  onToggleShuffle,
  onCycleRepeat,
  onToggleFavorite,
  onChangeArtworkStyle,
  onOpenEqualizer,
  onSelectQueueTrack,
  onRemoveFromQueue,
  onAddToPlaylist,
}) => {
  const [subView, setSubView] = useState<'player' | 'lyrics' | 'queue'>('player');
  const [showRemaining, setShowRemaining] = useState(false);
  const [showInfoModal, setShowInfoModal] = useState(false);
  const [showPlaylistModal, setShowPlaylistModal] = useState(false);

  const safeDuration = Math.max(duration || track.durationSec, 1);
  const progress = Math.min(1, Math.max(0, currentTime / safeDuration));

  const activeLyricIdx = useMemo(() => {
    let idx = -1;
    for (let i = 0; i < track.lyrics.length; i++) {
      if (track.lyrics[i].timeSec <= currentTime + 0.25) {
        idx = i;
      }
    }
    return idx;
  }, [track.lyrics, currentTime]);

  const artworkStyles: { id: ArtworkStyle; label: string }[] = [
    { id: 'ROUNDED_CARD', label: 'Glass Card' },
    { id: 'ROTATING_VINYL', label: 'Vinyl Disc' },
    { id: 'IMMERSIVE_GLOW', label: 'Aurora Glow' },
    { id: 'EDITORIAL_SQUARE', label: 'Studio Frame' },
  ];

  return (
    <div
      className="fixed inset-0 z-50 flex flex-col overflow-hidden bg-[#050507]/80 text-slate-100 backdrop-blur-3xl dark:bg-[#050507]/85"
      role="dialog"
      aria-modal="true"
      aria-label="Now Playing"
    >
      {/* Dynamic Track Blurred Background */}
      <div className="pointer-events-none absolute inset-0 -z-10 overflow-hidden">
        <img
          src={track.artwork}
          alt=""
          referrerPolicy="no-referrer"
          className="h-full w-full scale-125 object-cover transition-all duration-700"
          style={{ filter: 'blur(90px) saturate(1.5)', opacity: 0.55 }}
        />
        <div
          className="absolute inset-0"
          style={{
            background: `radial-gradient(circle at 25% 20%, ${accent.hex}36 0%, transparent 55%), linear-gradient(180deg, rgba(6,6,9,0.5) 0%, rgba(5,5,7,0.88) 100%)`,
          }}
        />
      </div>

      {/* Top Glass Header */}
      <header className="mx-auto flex w-full max-w-6xl items-center justify-between gap-4 px-4 pt-4 pb-2 sm:px-8">
        <LiquidGlassIconButton label="Minimize Now Playing" onClick={onClose}>
          <ChevronDown className="h-5 w-5" />
        </LiquidGlassIconButton>

        {/* Sub-view switcher (Player / Lyrics / Queue) */}
        <div className="flex items-center gap-1 rounded-full border border-white/15 bg-white/10 p-1 backdrop-blur-xl">
          {(
            [
              { id: 'player', label: 'Player' },
              { id: 'lyrics', label: 'Lyrics' },
              { id: 'queue', label: `Queue (${queue.length})` },
            ] as const
          ).map((tab) => {
            const active = subView === tab.id;
            return (
              <button
                key={tab.id}
                type="button"
                onClick={() => setSubView(tab.id)}
                style={active ? { backgroundColor: accent.hex, color: '#08090C' } : undefined}
                className={`min-h-[36px] whitespace-nowrap shrink-0 rounded-full px-3.5 py-1 text-xs font-semibold transition-all ${
                  active ? 'shadow-sm' : 'text-slate-300 hover:text-white'
                }`}
              >
                {tab.label}
              </button>
            );
          })}
        </div>

        <LiquidGlassIconButton label="Track File Info" onClick={() => setShowInfoModal(true)}>
          <Info className="h-5 w-5" />
        </LiquidGlassIconButton>
      </header>

      {/* Main Content Body */}
      <div className="mx-auto flex min-h-0 w-full max-w-6xl flex-1 flex-col justify-center overflow-y-auto px-4 py-4 sm:px-8">
        {subView === 'player' && (
          <div className="grid grid-cols-1 items-center gap-8 lg:grid-cols-12 lg:gap-12">
            {/* Artwork Stage */}
            <div className="flex flex-col items-center lg:col-span-6">
              <div className="relative flex aspect-square w-full max-w-[360px] items-center justify-center sm:max-w-[400px]">
                {artworkStyle === 'IMMERSIVE_GLOW' && isPlaying && (
                  <div
                    className="absolute inset-0 scale-105 rounded-[40px] opacity-60 blur-2xl transition-opacity duration-500"
                    style={{ background: `radial-gradient(circle, ${accent.hex}66, transparent 70%)` }}
                  />
                )}

                {artworkStyle === 'ROTATING_VINYL' ? (
                  <div
                    className={`relative flex aspect-square w-[92%] items-center justify-center rounded-full border border-white/25 bg-gradient-to-br from-[#232530] via-[#0B0C10] to-[#191B22] p-6 shadow-2xl ${
                      isPlaying ? 'animate-vinyl-spin' : 'animate-vinyl-paused'
                    }`}
                  >
                    {/* Vinyl concentric grooves */}
                    <div className="pointer-events-none absolute inset-4 rounded-full border border-white/10" />
                    <div className="pointer-events-none absolute inset-8 rounded-full border border-white/5" />
                    <div className="pointer-events-none absolute inset-12 rounded-full border border-white/10" />
                    <img
                      src={track.artwork}
                      alt={track.album}
                      referrerPolicy="no-referrer"
                      className="h-[64%] w-[64%] rounded-full border-2 border-white/30 object-cover shadow-xl"
                    />
                    <div
                      className="absolute h-8 w-8 rounded-full border-[3px] bg-[#09090B]"
                      style={{ borderColor: accent.hex }}
                    />
                  </div>
                ) : (
                  <div
                    className={`relative h-full w-full overflow-hidden border border-white/25 bg-white/10 p-2 shadow-2xl backdrop-blur-xl transition-transform duration-500 ${
                      artworkStyle === 'EDITORIAL_SQUARE' ? 'rounded-2xl' : 'rounded-[34px]'
                    } ${isPlaying ? 'scale-100' : 'scale-[0.95]'}`}
                    style={
                      artworkStyle === 'IMMERSIVE_GLOW'
                        ? { boxShadow: `0 24px 60px -12px ${accent.hex}66` }
                        : undefined
                    }
                  >
                    <img
                      src={track.artwork}
                      alt={track.album}
                      referrerPolicy="no-referrer"
                      className={`h-full w-full object-cover ${
                        artworkStyle === 'EDITORIAL_SQUARE' ? 'rounded-xl' : 'rounded-[26px]'
                      }`}
                    />
                  </div>
                )}
              </div>

              {/* Artwork Style Selector */}
              <div className="mt-5 flex flex-wrap items-center justify-center gap-1.5 rounded-2xl border border-white/15 bg-white/[0.07] p-1 backdrop-blur-xl">
                {artworkStyles.map((st) => {
                  const selected = artworkStyle === st.id;
                  return (
                    <button
                      key={st.id}
                      type="button"
                      onClick={() => onChangeArtworkStyle(st.id)}
                      style={selected ? { backgroundColor: `${accent.hex}33`, color: accent.glow } : undefined}
                      className={`whitespace-nowrap shrink-0 rounded-xl px-3 py-1.5 text-xs font-medium transition-colors ${
                        selected ? 'font-semibold' : 'text-slate-300 hover:text-white'
                      }`}
                    >
                      {st.label}
                    </button>
                  );
                })}
              </div>
            </div>

            {/* Track Info & Liquid Glass Controls */}
            <div className="flex flex-col space-y-5 lg:col-span-6">
              <div className="flex items-start justify-between gap-4">
                <div className="min-w-0">
                  <h2 className="truncate text-2xl font-bold tracking-tight text-white sm:text-3xl">
                    {track.title}
                  </h2>
                  <p className="mt-1 truncate text-base text-slate-300">
                    {track.artist} · {track.album}
                  </p>
                  <p className="mt-2 font-mono text-xs tabular-nums text-slate-400">
                    {track.format} · {(track.sampleRateHz / 1000).toFixed(1).replace(/\.0$/, '')} kHz ·{' '}
                    {track.bitrateKbps} kbps{isEqEnabled ? ' · DSP Active' : ''}
                  </p>
                </div>

                <LiquidGlassIconButton
                  label={isFavorite ? 'Remove from Favorites' : 'Add to Favorites'}
                  active={isFavorite}
                  accentHex={accent.hex}
                  sizeClass="h-12 w-12"
                  onClick={onToggleFavorite}
                >
                  <Heart className={`h-5 w-5 ${isFavorite ? 'fill-current' : ''}`} />
                </LiquidGlassIconButton>
              </div>

              {/* Interactive Liquid Glass Seek Bar */}
              <div>
                <LiquidGlassSlider
                  progress={progress}
                  accent={accent}
                  onSeekFraction={(f) => onSeek(f * safeDuration)}
                />
                <div className="mt-0.5 flex items-center justify-between font-mono text-xs tabular-nums text-slate-300">
                  <span>{formatTime(currentTime)}</span>
                  <button
                    type="button"
                    onClick={() => setShowRemaining((v) => !v)}
                    className="hover:text-white"
                  >
                    {showRemaining ? `-${formatTime(Math.max(0, safeDuration - currentTime))}` : formatTime(safeDuration)}
                  </button>
                </div>
              </div>

              {/* Primary Transport Controls */}
              <div className="flex items-center justify-between gap-2 pt-1">
                <LiquidGlassIconButton
                  label="Shuffle"
                  active={shuffle}
                  accentHex={accent.hex}
                  onClick={onToggleShuffle}
                >
                  <Shuffle className="h-5 w-5" />
                </LiquidGlassIconButton>

                <LiquidGlassIconButton label="Previous Track" sizeClass="h-13 w-13" onClick={onPrev}>
                  <SkipBack className="h-6 w-6" />
                </LiquidGlassIconButton>

                <button
                  type="button"
                  aria-label={isPlaying ? 'Pause' : 'Play'}
                  onClick={onTogglePlay}
                  style={{
                    background: `linear-gradient(135deg, ${accent.glow}, ${accent.hex}, ${accent.deep})`,
                    boxShadow: `0 16px 38px -8px ${accent.hex}AA, inset 0 1.5px 0 rgba(255,255,255,0.65)`,
                  }}
                  className="flex h-20 w-20 shrink-0 items-center justify-center rounded-full border border-white/60 text-[#07080B] transition-transform duration-150 hover:scale-105 active:scale-95"
                >
                  {isPlaying ? <Pause className="h-9 w-9 fill-current" /> : <Play className="ml-1 h-9 w-9 fill-current" />}
                </button>

                <LiquidGlassIconButton label="Next Track" sizeClass="h-13 w-13" onClick={onNext}>
                  <SkipForward className="h-6 w-6" />
                </LiquidGlassIconButton>

                <LiquidGlassIconButton
                  label={`Repeat (${repeatMode})`}
                  active={repeatMode !== 'OFF'}
                  accentHex={accent.hex}
                  onClick={onCycleRepeat}
                >
                  {repeatMode === 'ONE' ? <Repeat1 className="h-5 w-5" /> : <Repeat className="h-5 w-5" />}
                </LiquidGlassIconButton>
              </div>

              {/* Utility Glass Dock */}
              <div className="grid grid-cols-4 gap-2 rounded-2xl border border-white/15 bg-white/[0.08] p-2 backdrop-blur-2xl">
                <button
                  type="button"
                  onClick={() => setSubView('lyrics')}
                  className="flex min-h-[44px] flex-col items-center justify-center gap-1 rounded-xl px-2 py-2 text-xs font-medium text-slate-200 transition-colors hover:bg-white/10"
                >
                  <Mic2 className="h-4 w-4" />
                  <span className="whitespace-nowrap">Lyrics</span>
                </button>
                <button
                  type="button"
                  onClick={() => {
                    onClose();
                    onOpenEqualizer();
                  }}
                  style={isEqEnabled ? { color: accent.glow } : undefined}
                  className="flex min-h-[44px] flex-col items-center justify-center gap-1 rounded-xl px-2 py-2 text-xs font-medium text-slate-200 transition-colors hover:bg-white/10"
                >
                  <SlidersHorizontal className="h-4 w-4" />
                  <span className="whitespace-nowrap">EQ DSP</span>
                </button>
                <button
                  type="button"
                  onClick={() => setSubView('queue')}
                  className="flex min-h-[44px] flex-col items-center justify-center gap-1 rounded-xl px-2 py-2 text-xs font-medium text-slate-200 transition-colors hover:bg-white/10"
                >
                  <ListMusic className="h-4 w-4" />
                  <span className="whitespace-nowrap">Queue</span>
                </button>
                <button
                  type="button"
                  onClick={() => setShowPlaylistModal(true)}
                  className="flex min-h-[44px] flex-col items-center justify-center gap-1 rounded-xl px-2 py-2 text-xs font-medium text-slate-200 transition-colors hover:bg-white/10"
                >
                  <Plus className="h-4 w-4" />
                  <span className="whitespace-nowrap">Playlist</span>
                </button>
              </div>
            </div>
          </div>
        )}

        {subView === 'lyrics' && (
          <div className="mx-auto flex w-full max-w-2xl flex-col space-y-3 py-4">
            <div className="mb-2 flex items-center gap-3">
              <img
                src={track.artwork}
                alt={track.album}
                referrerPolicy="no-referrer"
                className="h-12 w-12 rounded-xl object-cover"
              />
              <div className="min-w-0">
                <h3 className="truncate text-lg font-bold text-white">{track.title}</h3>
                <p className="truncate text-xs text-slate-300">
                  {track.artist} · Tap any lyric line to seek
                </p>
              </div>
            </div>

            {track.lyrics.length === 0 ? (
              <LiquidGlassCard className="p-8 text-center">
                <p className="text-base font-semibold text-white">No synced lyrics embedded for this file</p>
                <p className="mt-1 text-sm text-slate-300">
                  Switch back to Player or import a track with embedded LRC lyrics.
                </p>
              </LiquidGlassCard>
            ) : (
              track.lyrics.map((line, idx) => {
                const isActive = idx === activeLyricIdx;
                return (
                  <button
                    key={`${line.timeSec}-${idx}`}
                    type="button"
                    onClick={() => onSeek(line.timeSec)}
                    style={
                      isActive
                        ? {
                            borderColor: `${accent.hex}88`,
                            backgroundColor: `${accent.hex}24`,
                          }
                        : undefined
                    }
                    className={`flex w-full items-center gap-4 rounded-2xl border px-5 py-4 text-left transition-all duration-200 ${
                      isActive
                        ? 'scale-[1.01] text-white shadow-lg'
                        : 'border-white/10 bg-white/[0.06] text-slate-300/75 hover:bg-white/10 hover:text-white'
                    }`}
                  >
                    <span
                      style={isActive ? { color: accent.glow } : undefined}
                      className="font-mono text-xs tabular-nums text-slate-400 shrink-0"
                    >
                      {formatTime(line.timeSec)}
                    </span>
                    <span className={`text-base sm:text-lg ${isActive ? 'font-bold' : 'font-medium'}`}>
                      {line.text}
                    </span>
                  </button>
                );
              })
            )}
          </div>
        )}

        {subView === 'queue' && (
          <div className="mx-auto w-full max-w-2xl space-y-2.5 py-4">
            <div className="mb-3 flex items-center justify-between">
              <h3 className="text-lg font-bold text-white">Up Next Queue ({queue.length})</h3>
              <span className="font-mono text-xs text-slate-300">Tap a track to play</span>
            </div>
            {queue.map((qTrack) => {
              const isCurrent = qTrack.id === track.id;
              return (
                <div
                  key={qTrack.id}
                  style={isCurrent ? { borderColor: `${accent.hex}88`, backgroundColor: `${accent.hex}1F` } : undefined}
                  className={`flex items-center justify-between gap-3 rounded-2xl border px-4 py-3 transition-colors ${
                    isCurrent ? '' : 'border-white/10 bg-white/[0.06] hover:bg-white/10'
                  }`}
                >
                  <button
                    type="button"
                    onClick={() => onSelectQueueTrack(qTrack.id)}
                    className="flex min-w-0 flex-1 items-center gap-3 text-left"
                  >
                    <img
                      src={qTrack.artwork}
                      alt={qTrack.album}
                      referrerPolicy="no-referrer"
                      className="h-11 w-11 shrink-0 rounded-xl object-cover"
                    />
                    <div className="min-w-0">
                      <p
                        style={isCurrent ? { color: accent.glow } : undefined}
                        className="truncate text-sm font-semibold text-white"
                      >
                        {qTrack.title}
                      </p>
                      <p className="truncate text-xs text-slate-300">
                        {qTrack.artist} · {formatTime(qTrack.durationSec)}
                      </p>
                    </div>
                  </button>
                  {queue.length > 1 && (
                    <button
                      type="button"
                      aria-label={`Remove ${qTrack.title} from queue`}
                      onClick={() => onRemoveFromQueue(qTrack.id)}
                      className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full text-slate-400 hover:bg-white/10 hover:text-white"
                    >
                      <Trash2 className="h-4 w-4" />
                    </button>
                  )}
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* Audio File Metadata Modal */}
      {showInfoModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-md">
          <div className="w-full max-w-md rounded-3xl border border-white/20 bg-[#14161F]/95 p-6 text-slate-100 shadow-2xl">
            <div className="flex items-center justify-between">
              <h3 className="text-lg font-bold">Audio File Information</h3>
              <button
                type="button"
                aria-label="Close"
                onClick={() => setShowInfoModal(false)}
                className="flex h-9 w-9 items-center justify-center rounded-full hover:bg-white/10"
              >
                <X className="h-4 w-4" />
              </button>
            </div>
            <div className="mt-4 divide-y divide-white/10 text-sm">
              {[
                ['Title', track.title],
                ['Artist', track.artist],
                ['Album', track.album],
                ['Codec / Format', track.format],
                ['Sample Rate', `${track.sampleRateHz} Hz`],
                ['Bitrate', `${track.bitrateKbps} kbps`],
                ['Duration', formatTime(safeDuration)],
                ['Folder Path', track.folder],
              ].map(([k, v]) => (
                <div key={k} className="flex items-center justify-between gap-4 py-2.5">
                  <span className="text-slate-400">{k}</span>
                  <span className="truncate font-mono text-xs text-slate-100">{v}</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* Add to Playlist Modal */}
      {showPlaylistModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-md">
          <div className="w-full max-w-md rounded-3xl border border-white/20 bg-[#14161F]/95 p-6 text-slate-100 shadow-2xl">
            <div className="flex items-center justify-between">
              <h3 className="text-lg font-bold">Add to Playlist</h3>
              <button
                type="button"
                aria-label="Close"
                onClick={() => setShowPlaylistModal(false)}
                className="flex h-9 w-9 items-center justify-center rounded-full hover:bg-white/10"
              >
                <X className="h-4 w-4" />
              </button>
            </div>
            <div className="mt-4 space-y-2">
              {playlists.map((pl) => {
                const alreadyIn = pl.songIds.includes(track.id);
                return (
                  <LiquidGlassButton
                    key={pl.id}
                    accent={accent}
                    highlighted={alreadyIn}
                    onClick={() => {
                      onAddToPlaylist(pl.id, track.id);
                      setShowPlaylistModal(false);
                    }}
                    className="w-full justify-between"
                  >
                    <span className="truncate">{pl.name}</span>
                    <span className="font-mono text-xs opacity-80">
                      {alreadyIn ? 'Added' : `${pl.songIds.length} tracks`}
                    </span>
                  </LiquidGlassButton>
                );
              })}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
