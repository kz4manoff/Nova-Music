/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useEffect, useMemo, useRef, useState } from 'react';
import {
  ArrowLeft,
  Heart,
  Library,
  Pause,
  Play,
  Plus,
  Search,
  Shuffle,
  SkipBack,
  SlidersHorizontal,
  Trash2,
  X,
} from 'lucide-react';
import { buildProceduralTrackWavUrl, NovaWebAudioPipeline } from './audio/webAudioEngine';
import { EqualizerView } from './components/EqualizerView';
import { GlassNavigationDock } from './components/GlassNavigationDock';
import { LibraryView, SongRowCard } from './components/LibraryView';
import {
  LiquidGlassBackdrop,
  LiquidGlassButton,
  LiquidGlassCard,
  LiquidGlassIconButton,
} from './components/LiquidGlass';
import { NowPlayingOverlay } from './components/NowPlayingOverlay';
import { RecitersView, SearchView } from './components/RecitersAndSearchViews';
import { SettingsView } from './components/SettingsView';
import {
  ACCENT_OPTIONS,
  AccentOption,
  AppTab,
  ArtworkStyle,
  BUILT_IN_EQ_PRESETS,
  EqPreset,
  INITIAL_PLAYLISTS,
  INITIAL_TRACKS,
  Playlist,
  RepeatMode,
  ThemeMode,
  Track,
  formatTime,
} from './data/novaLibrary';

export default function App() {
  // Theme & Visual State
  const [theme, setTheme] = useState<ThemeMode>('dark');
  const [accent, setAccent] = useState<AccentOption>(ACCENT_OPTIONS[0]);
  const [artworkStyle, setArtworkStyle] = useState<ArtworkStyle>('ROUNDED_CARD');
  const [activeTab, setActiveTab] = useState<AppTab>('home');
  const [showNowPlaying, setShowNowPlaying] = useState(false);

  // Music Library & Playlists State
  const [tracks, setTracks] = useState<Track[]>(INITIAL_TRACKS);
  const [favoriteIds, setFavoriteIds] = useState<Set<string>>(
    () => new Set(['track-obsidian-1', 'track-midnight-1'])
  );
  const [playlists, setPlaylists] = useState<Playlist[]>(INITIAL_PLAYLISTS);
  const [selectedPlaylistId, setSelectedPlaylistId] = useState<string | null>(null);
  const [recentIds, setRecentIds] = useState<string[]>([
    'track-obsidian-1',
    'track-midnight-1',
    'track-solar-1',
    'track-velvet-1',
  ]);
  const [searchQuery, setSearchQuery] = useState('');

  // Playback Queue & Player State
  const [queueIds, setQueueIds] = useState<string[]>(() => INITIAL_TRACKS.map((t) => t.id));
  const [currentTrackId, setCurrentTrackId] = useState<string>(INITIAL_TRACKS[0].id);
  const [isPlaying, setIsPlaying] = useState(false);
  const [currentTime, setCurrentTime] = useState(0);
  const [duration, setDuration] = useState(INITIAL_TRACKS[0].durationSec);
  const [shuffle, setShuffle] = useState(false);
  const [repeatMode, setRepeatMode] = useState<RepeatMode>('ALL');
  const [gapless, setGapless] = useState(true);
  const [sleepMinutes, setSleepMinutes] = useState<number | null>(null);

  // Equalizer DSP State
  const [eqEnabled, setEqEnabled] = useState(true);
  const [bandsDb, setBandsDb] = useState<number[]>(BUILT_IN_EQ_PRESETS[0].gainsDb);
  const [preampDb, setPreampDb] = useState(0);
  const [bassBoostPct, setBassBoostPct] = useState(20);
  const [loudnessDb, setLoudnessDb] = useState(1.5);
  const [activePresetName, setActivePresetName] = useState('Flat');
  const [customPresets, setCustomPresets] = useState<EqPreset[]>([]);
  const [spectrumBars, setSpectrumBars] = useState<number[]>([0.2, 0.3, 0.4, 0.35, 0.3, 0.25, 0.22, 0.18]);

  // Modals
  const [showNewPlaylistModal, setShowNewPlaylistModal] = useState(false);
  const [newPlaylistName, setNewPlaylistName] = useState('');
  const [playlistTargetTrack, setPlaylistTargetTrack] = useState<Track | null>(null);

  // Audio Element + Web Audio DSP Pipeline
  const audioRef = useRef<HTMLAudioElement | null>(null);
  const pipelineRef = useRef<NovaWebAudioPipeline>(new NovaWebAudioPipeline());
  const proceduralUrlCacheRef = useRef<Map<string, string>>(new Map());
  const loadedTrackIdRef = useRef<string | null>(null);

  // Sync Tailwind .dark class on <html>
  useEffect(() => {
    const root = document.documentElement;
    if (theme === 'dark') {
      root.classList.add('dark');
    } else {
      root.classList.remove('dark');
    }
  }, [theme]);

  const currentTrack = useMemo(
    () => tracks.find((t) => t.id === currentTrackId) ?? tracks[0],
    [tracks, currentTrackId]
  );

  const queueTracks = useMemo(
    () => queueIds.map((id) => tracks.find((t) => t.id === id)).filter((t): t is Track => Boolean(t)),
    [queueIds, tracks]
  );

  // Resolve playable URL (user local audio blob or offline procedural WAV blob)
  const getAudioUrlForTrack = (track: Track): string => {
    if (track.localUrl) return track.localUrl;
    const cached = proceduralUrlCacheRef.current.get(track.id);
    if (cached) return cached;
    if (track.synth) {
      const url = buildProceduralTrackWavUrl(track.synth, track.durationSec);
      proceduralUrlCacheRef.current.set(track.id, url);
      return url;
    }
    return '';
  };

  // Sync DSP parameters into live Web Audio nodes
  useEffect(() => {
    pipelineRef.current.updateDsp({
      enabled: eqEnabled,
      bandGainsDb: bandsDb,
      preampDb,
      bassBoostPct,
      loudnessDb,
    });
  }, [eqEnabled, bandsDb, preampDb, bassBoostPct, loudnessDb]);

  // Sleep Timer Effect
  useEffect(() => {
    if (sleepMinutes === null) return;
    const timeout = window.setTimeout(() => {
      if (audioRef.current) {
        audioRef.current.pause();
      }
      setIsPlaying(false);
      setSleepMinutes(null);
    }, sleepMinutes * 60_000);
    return () => window.clearTimeout(timeout);
  }, [sleepMinutes]);

  // Live Analyser Spectrum Loop while playing
  useEffect(() => {
    if (!isPlaying) return;
    let rafId = 0;
    const tick = () => {
      setSpectrumBars(pipelineRef.current.sampleSpectrum(8));
      rafId = requestAnimationFrame(tick);
    };
    rafId = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(rafId);
  }, [isPlaying]);

  const startPlaybackForTrack = (track: Track, newQueueList?: Track[]) => {
    if (newQueueList && newQueueList.length > 0) {
      setQueueIds(newQueueList.map((t) => t.id));
    }
    setCurrentTrackId(track.id);
    setRecentIds((prev) => [track.id, ...prev.filter((id) => id !== track.id)].slice(0, 12));
    setTracks((prev) =>
      prev.map((t) => (t.id === track.id ? { ...t, playCount: t.playCount + 1 } : t))
    );

    const audioEl = audioRef.current;
    if (!audioEl) return;

    pipelineRef.current.ensureConnected(audioEl);
    pipelineRef.current.updateDsp({
      enabled: eqEnabled,
      bandGainsDb: bandsDb,
      preampDb,
      bassBoostPct,
      loudnessDb,
    });

    const url = getAudioUrlForTrack(track);
    if (loadedTrackIdRef.current !== track.id || audioEl.src !== url) {
      audioEl.src = url;
      loadedTrackIdRef.current = track.id;
    }
    audioEl.currentTime = 0;
    setCurrentTime(0);
    setDuration(track.durationSec);
    void audioEl
      .play()
      .then(() => setIsPlaying(true))
      .catch(() => setIsPlaying(false));
  };

  const handleTogglePlayPause = () => {
    const audioEl = audioRef.current;
    if (!audioEl || !currentTrack) return;

    pipelineRef.current.ensureConnected(audioEl);
    pipelineRef.current.updateDsp({
      enabled: eqEnabled,
      bandGainsDb: bandsDb,
      preampDb,
      bassBoostPct,
      loudnessDb,
    });

    if (isPlaying) {
      audioEl.pause();
      setIsPlaying(false);
    } else {
      if (loadedTrackIdRef.current !== currentTrack.id || !audioEl.src) {
        audioEl.src = getAudioUrlForTrack(currentTrack);
        loadedTrackIdRef.current = currentTrack.id;
      }
      void audioEl
        .play()
        .then(() => setIsPlaying(true))
        .catch(() => setIsPlaying(false));
    }
  };

  const handleNext = () => {
    if (queueTracks.length === 0) return;
    if (shuffle && queueTracks.length > 1) {
      const candidates = queueTracks.filter((t) => t.id !== currentTrack.id);
      const next = candidates[Math.floor(Math.random() * candidates.length)];
      startPlaybackForTrack(next);
      return;
    }
    const idx = queueTracks.findIndex((t) => t.id === currentTrack.id);
    const nextIdx = (idx + 1) % queueTracks.length;
    startPlaybackForTrack(queueTracks[nextIdx]);
  };

  const handlePrev = () => {
    const audioEl = audioRef.current;
    if (audioEl && audioEl.currentTime > 3) {
      audioEl.currentTime = 0;
      setCurrentTime(0);
      return;
    }
    if (queueTracks.length === 0) return;
    const idx = queueTracks.findIndex((t) => t.id === currentTrack.id);
    const prevIdx = (idx - 1 + queueTracks.length) % queueTracks.length;
    startPlaybackForTrack(queueTracks[prevIdx]);
  };

  const handleSeek = (sec: number) => {
    const audioEl = audioRef.current;
    const safeSec = Math.max(0, Math.min(duration || currentTrack.durationSec, sec));
    if (audioEl) {
      if (loadedTrackIdRef.current !== currentTrack.id || !audioEl.src) {
        audioEl.src = getAudioUrlForTrack(currentTrack);
        loadedTrackIdRef.current = currentTrack.id;
      }
      audioEl.currentTime = safeSec;
    }
    setCurrentTime(safeSec);
  };

  const handleTrackEnded = () => {
    if (repeatMode === 'ONE') {
      handleSeek(0);
      void audioRef.current?.play();
      return;
    }
    const idx = queueTracks.findIndex((t) => t.id === currentTrack.id);
    const isLast = idx >= queueTracks.length - 1;
    if (!isLast || repeatMode === 'ALL' || shuffle) {
      handleNext();
    } else {
      setIsPlaying(false);
    }
  };

  const handleToggleFavorite = (trackId: string) => {
    setFavoriteIds((prev) => {
      const next = new Set(prev);
      if (next.has(trackId)) next.delete(trackId);
      else next.add(trackId);
      return next;
    });
  };

  const handleShuffleAll = () => {
    if (tracks.length === 0) return;
    const shuffled = [...tracks].sort(() => Math.random() - 0.5);
    setShuffle(true);
    startPlaybackForTrack(shuffled[0], shuffled);
  };

  const handleImportLocalFiles = (files: FileList) => {
    const imported: Track[] = [];
    Array.from(files).forEach((file, idx) => {
      const url = URL.createObjectURL(file);
      const ext = file.name.split('.').pop()?.toUpperCase() ?? 'MP3';
      const format = (['FLAC', 'WAV', 'OPUS', 'M4A', 'MP3'].includes(ext) ? ext : 'MP3') as Track['format'];
      const cleanTitle = file.name.replace(/\.[^/.]+$/, '');
      const fallbackArt = INITIAL_TRACKS[idx % INITIAL_TRACKS.length].artwork;
      const newTrack: Track = {
        id: `local-${Date.now()}-${idx}`,
        title: cleanTitle,
        artist: 'Local Device Track',
        album: 'Imported Audio',
        genre: 'Local File',
        folder: '/Local/Imported',
        year: new Date().getFullYear(),
        durationSec: 180,
        artwork: fallbackArt,
        format,
        sampleRateHz: 48000,
        bitrateKbps: 320,
        dateAdded: Math.floor(Date.now() / 1000),
        playCount: 0,
        lyrics: [],
        localUrl: url,
      };
      // Probe actual duration cleanly
      const probe = new Audio(url);
      probe.addEventListener('loadedmetadata', () => {
        if (Number.isFinite(probe.duration) && probe.duration > 0) {
          setTracks((prev) =>
            prev.map((t) => (t.id === newTrack.id ? { ...t, durationSec: Math.round(probe.duration) } : t))
          );
        }
      });
      imported.push(newTrack);
    });

    if (imported.length > 0) {
      setTracks((prev) => [...imported, ...prev]);
      startPlaybackForTrack(imported[0], [...imported, ...tracks]);
    }
  };

  const handleAddToPlaylist = (playlistId: string, trackId: string) => {
    setPlaylists((prev) =>
      prev.map((pl) =>
        pl.id === playlistId && !pl.songIds.includes(trackId)
          ? { ...pl, songIds: [...pl.songIds, trackId] }
          : pl
      )
    );
  };

  const albums = useMemo(() => {
    const seen = new Map<string, { album: string; artist: string; artwork: string; year: number; songs: Track[] }>();
    for (const t of tracks) {
      const existing = seen.get(t.album);
      if (existing) existing.songs.push(t);
      else seen.set(t.album, { album: t.album, artist: t.artist, artwork: t.artwork, year: t.year, songs: [t] });
    }
    return Array.from(seen.values());
  }, [tracks]);

  const recentlyPlayedTracks = useMemo(
    () => recentIds.map((id) => tracks.find((t) => t.id === id)).filter((t): t is Track => Boolean(t)),
    [recentIds, tracks]
  );

  const mostPlayedTracks = useMemo(
    () => [...tracks].sort((a, b) => b.playCount - a.playCount).slice(0, 6),
    [tracks]
  );

  const favoriteTracks = useMemo(
    () => tracks.filter((t) => favoriteIds.has(t.id)),
    [tracks, favoriteIds]
  );

  const safeDuration = Math.max(duration || currentTrack.durationSec, 1);
  const miniProgressFraction = Math.min(1, Math.max(0, currentTime / safeDuration));

  return (
    <div className="relative min-h-screen text-slate-900 transition-colors duration-300 dark:text-[#F5F7FA]">
      {/* Hidden Audio Element Feeding Real Web Audio DSP Pipeline */}
      <audio
        ref={audioRef}
        preload="auto"
        onTimeUpdate={(e) => setCurrentTime(e.currentTarget.currentTime)}
        onLoadedMetadata={(e) => {
          if (Number.isFinite(e.currentTarget.duration) && e.currentTarget.duration > 0) {
            setDuration(e.currentTarget.duration);
          }
        }}
        onEnded={handleTrackEnded}
      />

      {/* Dynamic Live Blurred Album Artwork + Pure Black Glassmorphism Backdrop */}
      <LiquidGlassBackdrop
        artwork={currentTrack.artwork}
        isPlaying={isPlaying}
        accent={accent}
        theme={theme}
      />

      {/* Minimalist Top Bar (Brand — Quick Equalizer / Now Playing) */}
      <header className="mx-auto max-w-6xl px-4 pt-3.5 pb-1 sm:px-6">
        <div className="flex items-center justify-between gap-4 rounded-2xl border border-slate-900/10 bg-white/75 px-4 py-2.5 backdrop-blur-2xl dark:border-white/[0.15] dark:bg-white/[0.04]">
          <button
            type="button"
            onClick={() => setActiveTab('home')}
            className="whitespace-nowrap shrink-0 text-left text-base font-bold tracking-tight text-slate-900 dark:text-white"
          >
            NOVA <span style={{ color: accent.hex }}>MUSIC</span>
          </button>

          <div className="flex items-center gap-2 shrink-0">
            <LiquidGlassIconButton
              label="Equalizer DSP"
              active={activeTab === 'equalizer'}
              accentHex={accent.hex}
              sizeClass="h-10 w-10"
              onClick={() => setActiveTab('equalizer')}
            >
              <SlidersHorizontal className="h-4 w-4" />
            </LiquidGlassIconButton>

            <button
              type="button"
              onClick={() => setShowNowPlaying(true)}
              className="inline-flex min-h-[40px] items-center gap-1.5 whitespace-nowrap shrink-0 rounded-full border border-white/20 bg-white/10 px-3.5 text-xs font-semibold text-slate-900 transition-colors hover:bg-white/20 dark:text-white"
            >
              <Play className="h-3.5 w-3.5 fill-current" />
              <span>Player</span>
            </button>
          </div>
        </div>
      </header>

      {/* Main Scrollable Viewport */}
      <main className="mx-auto max-w-6xl px-4 pt-4 pb-44 sm:px-6 sm:pb-48">
        {activeTab === 'home' && (
          <div className="space-y-6">
            {/* Search Launcher */}
            <LiquidGlassCard
              onClick={() => setActiveTab('search')}
              className="px-4 py-3.5"
            >
              <div className="flex items-center gap-3 text-sm text-slate-600 dark:text-slate-300">
                <Search className="h-4 w-4 shrink-0" style={{ color: accent.hex }} />
                <span>Search songs, albums, artists, genres...</span>
              </div>
            </LiquidGlassCard>

            {/* Hero Stage */}
            <LiquidGlassCard highlighted accentHex={accent.hex} className="p-5 sm:p-6">
              <div className="grid grid-cols-1 items-center gap-6 lg:grid-cols-12">
                <div className="space-y-4 lg:col-span-7">
                  <div>
                    <h1 className="text-2xl font-bold tracking-tight text-slate-900 sm:text-3xl dark:text-white">
                      {tracks.length} High-Resolution Tracks Ready
                    </h1>
                    <p className="mt-1.5 text-sm text-slate-600 dark:text-slate-300">
                      Offline studio audio playback with 10-band Web Audio DSP, live album blur refraction, and synced lyrics.
                    </p>
                  </div>

                  {/* Library Stats */}
                  <div className="grid grid-cols-3 gap-3 rounded-2xl border border-slate-900/10 bg-slate-900/[0.04] px-4 py-3 dark:border-white/10 dark:bg-white/[0.05]">
                    <div>
                      <p style={{ color: accent.hex }} className="font-mono text-lg font-bold tabular-nums">
                        {albums.length}
                      </p>
                      <p className="text-xs text-slate-500 dark:text-slate-400">Albums</p>
                    </div>
                    <div>
                      <p style={{ color: accent.hex }} className="font-mono text-lg font-bold tabular-nums">
                        {playlists.length}
                      </p>
                      <p className="text-xs text-slate-500 dark:text-slate-400">Playlists</p>
                    </div>
                    <div>
                      <p style={{ color: accent.hex }} className="font-mono text-lg font-bold tabular-nums">
                        {favoriteTracks.length}
                      </p>
                      <p className="text-xs text-slate-500 dark:text-slate-400">Favorites</p>
                    </div>
                  </div>

                  <div className="flex flex-wrap gap-3">
                    <LiquidGlassButton highlighted accent={accent} onClick={handleShuffleAll}>
                      <Shuffle className="h-4 w-4" />
                      <span>Shuffle All</span>
                    </LiquidGlassButton>
                    <LiquidGlassButton onClick={() => setActiveTab('library')}>
                      <Library className="h-4 w-4" />
                      <span>Explore Library</span>
                    </LiquidGlassButton>
                  </div>
                </div>

                {/* Active Track Spotlight */}
                <div className="lg:col-span-5">
                  <div
                    onClick={() => setShowNowPlaying(true)}
                    role="button"
                    tabIndex={0}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter' || e.key === ' ') {
                        e.preventDefault();
                        setShowNowPlaying(true);
                      }
                    }}
                    className="flex cursor-pointer items-center gap-4 rounded-2xl border border-white/60 bg-white/60 p-3.5 transition-all hover:bg-white/80 dark:border-white/15 dark:bg-white/[0.07] dark:hover:bg-white/[0.12]"
                  >
                    <img
                      src={currentTrack.artwork}
                      alt={currentTrack.album}
                      referrerPolicy="no-referrer"
                      className="h-20 w-20 shrink-0 rounded-2xl object-cover shadow-lg"
                    />
                    <div className="min-w-0 flex-1">
                      <p style={{ color: accent.hex }} className="text-xs font-semibold">
                        {isPlaying ? 'Now Playing' : 'Ready to Play'}
                      </p>
                      <p className="mt-0.5 truncate text-base font-bold text-slate-900 dark:text-white">
                        {currentTrack.title}
                      </p>
                      <p className="truncate text-xs text-slate-600 dark:text-slate-300">
                        {currentTrack.artist} · {currentTrack.format}
                      </p>
                    </div>
                    <LiquidGlassIconButton
                      label={isPlaying ? 'Pause' : 'Play'}
                      active
                      accentHex={accent.hex}
                      onClick={() => handleTogglePlayPause()}
                    >
                      {isPlaying ? <Pause className="h-5 w-5 fill-current" /> : <Play className="ml-0.5 h-5 w-5 fill-current" />}
                    </LiquidGlassIconButton>
                  </div>
                </div>
              </div>
            </LiquidGlassCard>

            {/* Featured Albums Carousel */}
            <section className="space-y-3">
              <div className="flex items-baseline justify-between">
                <h2 className="text-lg font-bold text-slate-900 dark:text-white">Featured Albums</h2>
                <span className="font-mono text-xs text-slate-500 dark:text-slate-400">
                  {albums.length} collections
                </span>
              </div>

              <div className="nova-scroll flex gap-4 overflow-x-auto pb-2">
                {albums.map((al) => (
                  <LiquidGlassCard
                    key={al.album}
                    onClick={() => startPlaybackForTrack(al.songs[0], al.songs)}
                    className="w-48 shrink-0 p-2 sm:w-52"
                  >
                    <div className="relative aspect-square overflow-hidden rounded-2xl">
                      <img
                        src={al.artwork}
                        alt={al.album}
                        referrerPolicy="no-referrer"
                        className="h-full w-full object-cover"
                      />
                      <div className="absolute inset-0 bg-gradient-to-t from-black/85 via-black/20 to-transparent" />
                      <div className="absolute inset-x-3 bottom-3">
                        <p className="truncate text-sm font-bold text-white">{al.album}</p>
                        <p className="truncate text-xs text-slate-200">
                          {al.artist} · {al.year}
                        </p>
                      </div>
                    </div>
                  </LiquidGlassCard>
                ))}
              </div>
            </section>

            {/* Recently Played */}
            <section className="space-y-3">
              <div className="flex items-baseline justify-between">
                <h2 className="text-lg font-bold text-slate-900 dark:text-white">Recently Played</h2>
                <span className="font-mono text-xs text-slate-500 dark:text-slate-400">
                  {recentlyPlayedTracks.length} tracks
                </span>
              </div>

              <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
                {recentlyPlayedTracks.map((song) => (
                  <SongRowCard
                    key={`recent-${song.id}`}
                    song={song}
                    isCurrent={song.id === currentTrack.id}
                    isPlaying={isPlaying}
                    isFavorite={favoriteIds.has(song.id)}
                    accent={accent}
                    onPlay={() => startPlaybackForTrack(song, recentlyPlayedTracks)}
                    onToggleFavorite={() => handleToggleFavorite(song.id)}
                    onAddToPlaylist={() => setPlaylistTargetTrack(song)}
                  />
                ))}
              </div>
            </section>

            {/* Most Played & Favorites */}
            <section className="space-y-3">
              <div className="flex items-baseline justify-between">
                <h2 className="text-lg font-bold text-slate-900 dark:text-white">Top Rotation</h2>
                <span className="font-mono text-xs text-slate-500 dark:text-slate-400">Most played</span>
              </div>
              <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
                {mostPlayedTracks.map((song) => (
                  <SongRowCard
                    key={`top-${song.id}`}
                    song={song}
                    isCurrent={song.id === currentTrack.id}
                    isPlaying={isPlaying}
                    isFavorite={favoriteIds.has(song.id)}
                    accent={accent}
                    onPlay={() => startPlaybackForTrack(song, mostPlayedTracks)}
                    onToggleFavorite={() => handleToggleFavorite(song.id)}
                    onAddToPlaylist={() => setPlaylistTargetTrack(song)}
                  />
                ))}
              </div>
            </section>
          </div>
        )}

        {activeTab === 'reciters' && (
          <RecitersView
            tracks={tracks}
            currentTrackId={currentTrack.id}
            isPlaying={isPlaying}
            favoriteIds={favoriteIds}
            accent={accent}
            onPlayTrack={startPlaybackForTrack}
            onToggleFavorite={handleToggleFavorite}
            onAddToPlaylist={(song) => setPlaylistTargetTrack(song)}
            onOpenFullLibrary={() => setActiveTab('library')}
          />
        )}

        {activeTab === 'search' && (
          <SearchView
            tracks={tracks}
            currentTrackId={currentTrack.id}
            isPlaying={isPlaying}
            favoriteIds={favoriteIds}
            searchQuery={searchQuery}
            accent={accent}
            onSearchChange={setSearchQuery}
            onPlayTrack={startPlaybackForTrack}
            onToggleFavorite={handleToggleFavorite}
            onAddToPlaylist={(song) => setPlaylistTargetTrack(song)}
          />
        )}

        {activeTab === 'library' && (
          <div className="space-y-4">
            <button
              type="button"
              onClick={() => setActiveTab('reciters')}
              className="inline-flex min-h-[40px] items-center gap-2 rounded-full border border-white/15 bg-white/[0.06] px-4 text-xs font-semibold text-slate-900 transition-colors hover:bg-white/15 dark:text-white"
            >
              <ArrowLeft className="h-4 w-4" />
              <span>Back to Reciters</span>
            </button>
            <LibraryView
              tracks={tracks}
              currentTrackId={currentTrack.id}
              isPlaying={isPlaying}
              favoriteIds={favoriteIds}
              searchQuery={searchQuery}
              accent={accent}
              onSearchChange={setSearchQuery}
              onPlayTrack={startPlaybackForTrack}
              onToggleFavorite={handleToggleFavorite}
              onImportFiles={handleImportLocalFiles}
              onAddTrackToPlaylistPrompt={(song) => setPlaylistTargetTrack(song)}
            />
          </div>
        )}

        {activeTab === 'playlists' && (
          <div className="space-y-5">
            {selectedPlaylistId ? (
              (() => {
                const isFavoritesView = selectedPlaylistId === 'favorites';
                const pl = playlists.find((p) => p.id === selectedPlaylistId);
                const listTracks = isFavoritesView
                  ? favoriteTracks
                  : (pl?.songIds
                      .map((id) => tracks.find((t) => t.id === id))
                      .filter((t): t is Track => Boolean(t)) ?? []);

                return (
                  <div className="space-y-4">
                    <LiquidGlassCard className="p-5">
                      <div className="flex flex-wrap items-center justify-between gap-4">
                        <div className="flex items-center gap-3.5">
                          <LiquidGlassIconButton
                            label="Back to Playlists"
                            onClick={() => setSelectedPlaylistId(null)}
                          >
                            <SkipBack className="h-4 w-4" />
                          </LiquidGlassIconButton>
                          <div>
                            <h2 className="text-xl font-bold text-slate-900 dark:text-white">
                              {isFavoritesView ? 'Favorite Tracks' : pl?.name}
                            </h2>
                            <p className="text-xs text-slate-600 dark:text-slate-400">
                              {listTracks.length} tracks
                            </p>
                          </div>
                        </div>

                        <LiquidGlassButton
                          highlighted
                          accent={accent}
                          disabled={listTracks.length === 0}
                          onClick={() => {
                            if (listTracks.length > 0) startPlaybackForTrack(listTracks[0], listTracks);
                          }}
                        >
                          <Play className="h-4 w-4 fill-current" />
                          <span>Play All</span>
                        </LiquidGlassButton>
                      </div>
                    </LiquidGlassCard>

                    <div className="space-y-2.5">
                      {listTracks.map((song) => (
                        <SongRowCard
                          key={`pl-song-${song.id}`}
                          song={song}
                          isCurrent={song.id === currentTrack.id}
                          isPlaying={isPlaying}
                          isFavorite={favoriteIds.has(song.id)}
                          accent={accent}
                          onPlay={() => startPlaybackForTrack(song, listTracks)}
                          onToggleFavorite={() => handleToggleFavorite(song.id)}
                          onAddToPlaylist={() => setPlaylistTargetTrack(song)}
                        />
                      ))}
                    </div>
                  </div>
                );
              })()
            ) : (
              <>
                <LiquidGlassCard className="p-5">
                  <div className="flex items-center justify-between gap-4">
                    <div>
                      <h2 className="text-xl font-bold tracking-tight text-slate-900 dark:text-white">
                        Playlists
                      </h2>
                      <p className="mt-0.5 text-sm text-slate-600 dark:text-slate-300">
                        {playlists.length} custom collections
                      </p>
                    </div>
                    <LiquidGlassButton
                      highlighted
                      accent={accent}
                      onClick={() => setShowNewPlaylistModal(true)}
                    >
                      <Plus className="h-4 w-4" />
                      <span>New Playlist</span>
                    </LiquidGlassButton>
                  </div>
                </LiquidGlassCard>

                {/* Favorites Card */}
                <LiquidGlassCard
                  highlighted
                  accentHex={accent.hex}
                  onClick={() => setSelectedPlaylistId('favorites')}
                  className="p-5"
                >
                  <div className="flex items-center justify-between gap-4">
                    <div className="flex items-center gap-4">
                      <div
                        className="flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl"
                        style={{ backgroundColor: `${accent.hex}24`, color: accent.hex }}
                      >
                        <Heart className="h-6 w-6 fill-current" />
                      </div>
                      <div>
                        <h3 className="text-base font-bold text-slate-900 dark:text-white">Favorite Tracks</h3>
                        <p className="text-xs text-slate-600 dark:text-slate-300">
                          {favoriteTracks.length} loved tracks
                        </p>
                      </div>
                    </div>
                    <Play className="h-5 w-5" style={{ color: accent.hex }} />
                  </div>
                </LiquidGlassCard>

                <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
                  {playlists.map((pl) => {
                    const plTracks = pl.songIds
                      .map((id) => tracks.find((t) => t.id === id))
                      .filter((t): t is Track => Boolean(t));
                    const cover = plTracks[0]?.artwork ?? INITIAL_TRACKS[0].artwork;
                    return (
                      <LiquidGlassCard
                        key={pl.id}
                        className="p-4"
                        onClick={() => setSelectedPlaylistId(pl.id)}
                      >
                        <div className="flex items-center gap-3.5">
                          <img
                            src={cover}
                            alt={pl.name}
                            referrerPolicy="no-referrer"
                            className="h-16 w-16 shrink-0 rounded-2xl object-cover shadow"
                          />
                          <div className="min-w-0 flex-1">
                            <p className="truncate text-base font-bold text-slate-900 dark:text-white">{pl.name}</p>
                            <p className="text-xs text-slate-600 dark:text-slate-400">
                              {plTracks.length} tracks
                            </p>
                          </div>
                          <button
                            type="button"
                            aria-label={`Delete ${pl.name}`}
                            onClick={(e) => {
                              e.stopPropagation();
                              setPlaylists((prev) => prev.filter((item) => item.id !== pl.id));
                            }}
                            className="flex h-9 w-9 items-center justify-center rounded-full text-slate-400 hover:bg-rose-500/15 hover:text-rose-500"
                          >
                            <Trash2 className="h-4 w-4" />
                          </button>
                        </div>
                      </LiquidGlassCard>
                    );
                  })}
                </div>
              </>
            )}
          </div>
        )}

        {activeTab === 'equalizer' && (
          <div className="space-y-4">
            <button
              type="button"
              onClick={() => setActiveTab('settings')}
              className="inline-flex min-h-[40px] items-center gap-2 rounded-full border border-white/15 bg-white/[0.06] px-4 text-xs font-semibold text-slate-900 transition-colors hover:bg-white/15 dark:text-white"
            >
              <ArrowLeft className="h-4 w-4" />
              <span>Back to Settings</span>
            </button>
            <EqualizerView
              enabled={eqEnabled}
              bandsDb={bandsDb}
              preampDb={preampDb}
              bassBoostPct={bassBoostPct}
              loudnessDb={loudnessDb}
              activePresetName={activePresetName}
              customPresets={customPresets}
              spectrumBars={spectrumBars}
              isPlaying={isPlaying}
              accent={accent}
              onToggleEnabled={() => setEqEnabled((v) => !v)}
              onChangeBand={(idx, db) => {
                setBandsDb((prev) => prev.map((val, i) => (i === idx ? db : val)));
                setActivePresetName('Custom');
              }}
              onChangePreamp={setPreampDb}
              onChangeBassBoost={setBassBoostPct}
              onChangeLoudness={setLoudnessDb}
              onApplyPreset={(preset) => {
                setBandsDb(preset.gainsDb);
                setActivePresetName(preset.name);
              }}
              onSaveCustomPreset={(name) => {
                setCustomPresets((prev) => [
                  ...prev.filter((p) => p.name !== name),
                  { name, gainsDb: [...bandsDb] },
                ]);
                setActivePresetName(name);
              }}
              onDeleteCustomPreset={(name) => {
                setCustomPresets((prev) => prev.filter((p) => p.name !== name));
              }}
              onReset={() => {
                setBandsDb(BUILT_IN_EQ_PRESETS[0].gainsDb);
                setPreampDb(0);
                setBassBoostPct(0);
                setLoudnessDb(0);
                setActivePresetName('Flat');
              }}
            />
          </div>
        )}

        {activeTab === 'settings' && (
          <SettingsView
            theme={theme}
            accent={accent}
            artworkStyle={artworkStyle}
            gapless={gapless}
            sleepMinutes={sleepMinutes}
            onChangeTheme={setTheme}
            onChangeAccent={setAccent}
            onChangeArtworkStyle={setArtworkStyle}
            onToggleGapless={() => setGapless((v) => !v)}
            onSetSleepTimer={setSleepMinutes}
            onOpenEqualizer={() => setActiveTab('equalizer')}
          />
        )}
      </main>

      {/* Floating Pill Mini-Player + 5-Tab Frosted Glass Bottom Navigation Bar */}
      <GlassNavigationDock
        activeTab={activeTab}
        currentTrack={currentTrack}
        isPlaying={isPlaying}
        progressFraction={miniProgressFraction}
        accent={accent}
        theme={theme}
        onSelectTab={(tab) => {
          setActiveTab(tab);
          if (tab !== 'playlists') setSelectedPlaylistId(null);
        }}
        onOpenNowPlaying={() => setShowNowPlaying(true)}
        onTogglePlay={handleTogglePlayPause}
        onNext={handleNext}
      />

      {/* Full-Screen Now Playing Overlay */}
      {showNowPlaying && (
        <NowPlayingOverlay
          track={currentTrack}
          isPlaying={isPlaying}
          currentTime={currentTime}
          duration={safeDuration}
          shuffle={shuffle}
          repeatMode={repeatMode}
          isFavorite={favoriteIds.has(currentTrack.id)}
          isEqEnabled={eqEnabled}
          artworkStyle={artworkStyle}
          accent={accent}
          queue={queueTracks}
          playlists={playlists}
          onClose={() => setShowNowPlaying(false)}
          onTogglePlay={handleTogglePlayPause}
          onPrev={handlePrev}
          onNext={handleNext}
          onSeek={handleSeek}
          onToggleShuffle={() => setShuffle((v) => !v)}
          onCycleRepeat={() =>
            setRepeatMode((m) => (m === 'OFF' ? 'ALL' : m === 'ALL' ? 'ONE' : 'OFF'))
          }
          onToggleFavorite={() => handleToggleFavorite(currentTrack.id)}
          onChangeArtworkStyle={setArtworkStyle}
          onOpenEqualizer={() => setActiveTab('equalizer')}
          onSelectQueueTrack={(id) => {
            const target = tracks.find((t) => t.id === id);
            if (target) startPlaybackForTrack(target);
          }}
          onRemoveFromQueue={(id) => {
            if (queueIds.length <= 1) return;
            setQueueIds((prev) => prev.filter((item) => item !== id));
          }}
          onAddToPlaylist={handleAddToPlaylist}
        />
      )}

      {/* New Playlist Modal */}
      {showNewPlaylistModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-md">
          <div className="w-full max-w-sm rounded-3xl border border-white/20 bg-[#14161F]/95 p-6 text-slate-100 shadow-2xl">
            <div className="flex items-center justify-between">
              <h3 className="text-lg font-bold">New Playlist</h3>
              <button
                type="button"
                aria-label="Close"
                onClick={() => setShowNewPlaylistModal(false)}
                className="flex h-9 w-9 items-center justify-center rounded-full hover:bg-white/10"
              >
                <X className="h-4 w-4" />
              </button>
            </div>
            <input
              type="text"
              value={newPlaylistName}
              onChange={(e) => setNewPlaylistName(e.target.value)}
              placeholder="Playlist name"
              className="mt-4 w-full rounded-xl border border-white/15 bg-white/10 px-3.5 py-2.5 text-sm text-white placeholder-slate-400 focus:outline-none"
            />
            <div className="mt-4 flex justify-end gap-2">
              <LiquidGlassButton onClick={() => setShowNewPlaylistModal(false)}>Cancel</LiquidGlassButton>
              <LiquidGlassButton
                highlighted
                accent={accent}
                onClick={() => {
                  const trimmed = newPlaylistName.trim();
                  if (!trimmed) return;
                  setPlaylists((prev) => [
                    ...prev,
                    { id: `pl-${Date.now()}`, name: trimmed, songIds: [] },
                  ]);
                  setNewPlaylistName('');
                  setShowNewPlaylistModal(false);
                }}
              >
                Create
              </LiquidGlassButton>
            </div>
          </div>
        </div>
      )}

      {/* Quick Add Track to Playlist Modal */}
      {playlistTargetTrack && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-md">
          <div className="w-full max-w-sm rounded-3xl border border-white/20 bg-[#14161F]/95 p-6 text-slate-100 shadow-2xl">
            <div className="flex items-center justify-between">
              <h3 className="truncate text-lg font-bold">Add "{playlistTargetTrack.title}"</h3>
              <button
                type="button"
                aria-label="Close"
                onClick={() => setPlaylistTargetTrack(null)}
                className="flex h-9 w-9 items-center justify-center rounded-full hover:bg-white/10"
              >
                <X className="h-4 w-4" />
              </button>
            </div>
            <div className="mt-4 space-y-2">
              {playlists.map((pl) => {
                const alreadyIn = pl.songIds.includes(playlistTargetTrack.id);
                return (
                  <LiquidGlassButton
                    key={pl.id}
                    accent={accent}
                    highlighted={alreadyIn}
                    onClick={() => {
                      handleAddToPlaylist(pl.id, playlistTargetTrack.id);
                      setPlaylistTargetTrack(null);
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
}
