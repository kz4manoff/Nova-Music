import React, { useMemo, useState } from 'react';
import { ArrowLeft, Library, Play, Search, X } from 'lucide-react';
import { AccentOption, Track } from '../data/novaLibrary';
import { RecitersIcon } from './GlassNavigationDock';
import { SongRowCard } from './LibraryView';
import { LiquidGlassButton, LiquidGlassCard, LiquidGlassIconButton } from './LiquidGlass';

interface RecitersViewProps {
  tracks: Track[];
  currentTrackId: string;
  isPlaying: boolean;
  favoriteIds: Set<string>;
  accent: AccentOption;
  onPlayTrack: (track: Track, list: Track[]) => void;
  onToggleFavorite: (id: string) => void;
  onAddToPlaylist: (track: Track) => void;
  onOpenFullLibrary: () => void;
}

export const RecitersView: React.FC<RecitersViewProps> = ({
  tracks,
  currentTrackId,
  isPlaying,
  favoriteIds,
  accent,
  onPlayTrack,
  onToggleFavorite,
  onAddToPlaylist,
  onOpenFullLibrary,
}) => {
  const [selectedReciterName, setSelectedReciterName] = useState<string | null>(null);

  const reciters = useMemo(() => {
    const map = new Map<
      string,
      { name: string; genre: string; artwork: string; songs: Track[] }
    >();
    for (const track of tracks) {
      const existing = map.get(track.artist);
      if (existing) {
        existing.songs.push(track);
      } else {
        map.set(track.artist, {
          name: track.artist,
          genre: track.genre,
          artwork: track.artwork,
          songs: [track],
        });
      }
    }
    return Array.from(map.values());
  }, [tracks]);

  const activeReciter = useMemo(
    () => reciters.find((r) => r.name === selectedReciterName) ?? null,
    [reciters, selectedReciterName]
  );

  if (activeReciter) {
    return (
      <div className="space-y-5">
        <LiquidGlassCard className="p-5">
          <div className="flex flex-wrap items-center gap-4">
            <LiquidGlassIconButton
              label="Back to Reciters"
              onClick={() => setSelectedReciterName(null)}
            >
              <ArrowLeft className="h-5 w-5" />
            </LiquidGlassIconButton>

            <img
              src={activeReciter.artwork}
              alt={activeReciter.name}
              referrerPolicy="no-referrer"
              className="h-16 w-16 rounded-full border border-white/25 object-cover shadow-lg sm:h-20 sm:w-20"
            />

            <div className="min-w-0 flex-1">
              <h2 className="truncate text-2xl font-bold text-slate-900 dark:text-white">
                {activeReciter.name}
              </h2>
              <p className="mt-0.5 truncate text-sm text-slate-600 dark:text-white/65">
                {activeReciter.genre} · {activeReciter.songs.length} tracks
              </p>
            </div>

            <LiquidGlassButton
              highlighted
              accent={accent}
              onClick={() => {
                if (activeReciter.songs.length > 0) {
                  onPlayTrack(activeReciter.songs[0], activeReciter.songs);
                }
              }}
            >
              <Play className="h-4 w-4 fill-current" />
              <span>Play All</span>
            </LiquidGlassButton>
          </div>
        </LiquidGlassCard>

        <div className="space-y-2.5">
          {activeReciter.songs.map((song) => (
            <SongRowCard
              key={song.id}
              song={song}
              isCurrent={song.id === currentTrackId}
              isPlaying={isPlaying}
              isFavorite={favoriteIds.has(song.id)}
              accent={accent}
              onPlay={() => onPlayTrack(song, activeReciter.songs)}
              onToggleFavorite={() => onToggleFavorite(song.id)}
              onAddToPlaylist={() => onAddToPlaylist(song)}
            />
          ))}
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-5">
      <LiquidGlassCard className="p-5">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div className="flex items-center gap-3.5">
            <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full border border-white/20 bg-white/10 text-slate-900 dark:text-white">
              <RecitersIcon className="h-5 w-5" />
            </div>
            <div>
              <h2 className="text-xl font-bold tracking-tight text-slate-900 dark:text-white">
                Reciters & Artists
              </h2>
              <p className="mt-0.5 text-sm text-slate-600 dark:text-white/65">
                {reciters.length} voices · {tracks.length} studio tracks
              </p>
            </div>
          </div>

          <LiquidGlassButton onClick={onOpenFullLibrary}>
            <Library className="h-4 w-4" />
            <span>Full Library & Import</span>
          </LiquidGlassButton>
        </div>
      </LiquidGlassCard>

      <div className="grid grid-cols-1 gap-3.5 sm:grid-cols-2 lg:grid-cols-3">
        {reciters.map((reciter) => {
          const isCurrentReciter = reciter.songs.some((s) => s.id === currentTrackId);
          return (
            <LiquidGlassCard
              key={reciter.name}
              highlighted={isCurrentReciter}
              accentHex={accent.hex}
              className="p-4"
              onClick={() => setSelectedReciterName(reciter.name)}
            >
              <div className="flex items-center gap-3.5">
                <img
                  src={reciter.artwork}
                  alt={reciter.name}
                  referrerPolicy="no-referrer"
                  className="h-14 w-14 shrink-0 rounded-full border border-white/25 object-cover shadow"
                />
                <div className="min-w-0 flex-1">
                  <p className="truncate text-base font-bold text-slate-900 dark:text-white">
                    {reciter.name}
                  </p>
                  <p className="mt-0.5 truncate text-xs text-slate-600 dark:text-white/60">
                    {reciter.genre} · {reciter.songs.length} tracks
                  </p>
                </div>
                <button
                  type="button"
                  aria-label={`Play ${reciter.name}`}
                  onClick={(e) => {
                    e.stopPropagation();
                    onPlayTrack(reciter.songs[0], reciter.songs);
                  }}
                  className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full border border-white/20 bg-white/10 text-slate-900 transition-transform hover:bg-white/20 active:scale-90 dark:text-white"
                >
                  <Play className="ml-0.5 h-4 w-4 fill-current" />
                </button>
              </div>
            </LiquidGlassCard>
          );
        })}
      </div>
    </div>
  );
};

interface SearchViewProps {
  tracks: Track[];
  currentTrackId: string;
  isPlaying: boolean;
  favoriteIds: Set<string>;
  searchQuery: string;
  accent: AccentOption;
  onSearchChange: (q: string) => void;
  onPlayTrack: (track: Track, list: Track[]) => void;
  onToggleFavorite: (id: string) => void;
  onAddToPlaylist: (track: Track) => void;
}

export const SearchView: React.FC<SearchViewProps> = ({
  tracks,
  currentTrackId,
  isPlaying,
  favoriteIds,
  searchQuery,
  accent,
  onSearchChange,
  onPlayTrack,
  onToggleFavorite,
  onAddToPlaylist,
}) => {
  const [selectedGenre, setSelectedGenre] = useState<string>('ALL');

  const genres = useMemo(() => {
    const set = new Set<string>(['ALL']);
    tracks.forEach((t) => set.add(t.genre));
    return Array.from(set);
  }, [tracks]);

  const filteredTracks = useMemo(() => {
    const q = searchQuery.trim().toLowerCase();
    return tracks.filter((t) => {
      const matchesGenre = selectedGenre === 'ALL' || t.genre === selectedGenre;
      const matchesQuery =
        !q ||
        t.title.toLowerCase().includes(q) ||
        t.artist.toLowerCase().includes(q) ||
        t.album.toLowerCase().includes(q) ||
        t.genre.toLowerCase().includes(q);
      return matchesGenre && matchesQuery;
    });
  }, [tracks, searchQuery, selectedGenre]);

  return (
    <div className="space-y-5">
      <LiquidGlassCard className="p-5">
        <h2 className="text-xl font-bold tracking-tight text-slate-900 dark:text-white">
          Search Audio
        </h2>
        <p className="mt-0.5 text-sm text-slate-600 dark:text-white/65">
          Filter tracks, reciters, albums, and genres
        </p>

        <div className="relative mt-4">
          <Search
            className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-500 dark:text-white/70"
          />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => onSearchChange(e.target.value)}
            placeholder="Search tracks, reciters, albums..."
            className="h-12 w-full rounded-2xl border border-slate-900/10 bg-slate-900/[0.04] pl-10 pr-11 text-sm text-slate-900 placeholder-slate-500 focus:outline-none dark:border-white/[0.16] dark:bg-white/[0.06] dark:text-white dark:placeholder-white/45"
          />
          {searchQuery && (
            <button
              type="button"
              aria-label="Clear search"
              onClick={() => onSearchChange('')}
              className="absolute right-2 top-1/2 flex h-8 w-8 -translate-y-1/2 items-center justify-center rounded-full text-slate-500 hover:bg-slate-900/10 dark:text-white/70 dark:hover:bg-white/10"
            >
              <X className="h-4 w-4" />
            </button>
          )}
        </div>

        {/* Genre Quick Filters */}
        <div className="nova-scroll mt-3.5 flex items-center gap-2 overflow-x-auto pb-1">
          {genres.map((genre) => {
            const active = selectedGenre === genre;
            return (
              <button
                key={genre}
                type="button"
                onClick={() => setSelectedGenre(genre)}
                className={`min-h-[38px] whitespace-nowrap shrink-0 rounded-xl border px-3.5 py-1.5 text-xs font-semibold transition-colors ${
                  active
                    ? 'border-white/30 bg-white/15 text-slate-950 dark:text-white'
                    : 'border-slate-900/10 bg-slate-900/[0.04] text-slate-700 hover:bg-slate-900/10 dark:border-white/10 dark:bg-white/[0.04] dark:text-white/65 dark:hover:bg-white/10 dark:hover:text-white'
                }`}
              >
                {genre === 'ALL' ? 'All Genres' : genre}
              </button>
            );
          })}
        </div>
      </LiquidGlassCard>

      <div className="space-y-2.5">
        {filteredTracks.length === 0 ? (
          <LiquidGlassCard className="p-8 text-center">
            <p className="text-base font-semibold text-slate-900 dark:text-white">
              No audio tracks match "{searchQuery}"
            </p>
            <p className="mt-1 text-sm text-slate-500 dark:text-white/55">
              Try a different keyword or reset your genre filter.
            </p>
          </LiquidGlassCard>
        ) : (
          filteredTracks.map((song) => (
            <SongRowCard
              key={`search-${song.id}`}
              song={song}
              isCurrent={song.id === currentTrackId}
              isPlaying={isPlaying}
              isFavorite={favoriteIds.has(song.id)}
              accent={accent}
              onPlay={() => onPlayTrack(song, filteredTracks)}
              onToggleFavorite={() => onToggleFavorite(song.id)}
              onAddToPlaylist={() => onAddToPlaylist(song)}
            />
          ))
        )}
      </div>
    </div>
  );
};
