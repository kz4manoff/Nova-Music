import React, { useMemo, useState } from 'react';
import {
  ArrowLeft,
  ArrowUpDown,
  Disc,
  Folder,
  Heart,
  Music2,
  Pause,
  Play,
  Plus,
  Search,
  Tag,
  Upload,
  User,
  X,
} from 'lucide-react';
import { AccentOption, LibraryTab, SortField, Track, formatTime } from '../data/novaLibrary';
import { LiquidGlassButton, LiquidGlassCard, LiquidGlassIconButton } from './LiquidGlass';

interface LibraryViewProps {
  tracks: Track[];
  currentTrackId: string;
  isPlaying: boolean;
  favoriteIds: Set<string>;
  searchQuery: string;
  accent: AccentOption;
  onSearchChange: (q: string) => void;
  onPlayTrack: (track: Track, list: Track[]) => void;
  onToggleFavorite: (id: string) => void;
  onImportFiles: (files: FileList) => void;
  onAddTrackToPlaylistPrompt: (track: Track) => void;
}

export const LibraryView: React.FC<LibraryViewProps> = ({
  tracks,
  currentTrackId,
  isPlaying,
  favoriteIds,
  searchQuery,
  accent,
  onSearchChange,
  onPlayTrack,
  onToggleFavorite,
  onImportFiles,
  onAddTrackToPlaylistPrompt,
}) => {
  const [activeTab, setActiveTab] = useState<LibraryTab>('SONGS');
  const [sortBy, setSortBy] = useState<SortField>('NAME');
  const [selectedCollection, setSelectedCollection] = useState<{
    title: string;
    subtitle: string;
    artwork: string;
    songs: Track[];
  } | null>(null);

  const filteredSongs = useMemo(() => {
    const q = searchQuery.trim().toLowerCase();
    const list = q
      ? tracks.filter(
          (t) =>
            t.title.toLowerCase().includes(q) ||
            t.artist.toLowerCase().includes(q) ||
            t.album.toLowerCase().includes(q) ||
            t.genre.toLowerCase().includes(q) ||
            t.folder.toLowerCase().includes(q)
        )
      : [...tracks];

    list.sort((a, b) => {
      switch (sortBy) {
        case 'ARTIST':
          return a.artist.localeCompare(b.artist);
        case 'ALBUM':
          return a.album.localeCompare(b.album);
        case 'DURATION':
          return b.durationSec - a.durationSec;
        case 'DATE_ADDED':
          return b.dateAdded - a.dateAdded;
        case 'NAME':
        default:
          return a.title.localeCompare(b.title);
      }
    });
    return list;
  }, [tracks, searchQuery, sortBy]);

  const albums = useMemo(() => {
    const map = new Map<string, { name: string; artist: string; artwork: string; year: number; songs: Track[] }>();
    for (const t of filteredSongs) {
      const existing = map.get(t.album);
      if (existing) {
        existing.songs.push(t);
      } else {
        map.set(t.album, { name: t.album, artist: t.artist, artwork: t.artwork, year: t.year, songs: [t] });
      }
    }
    return Array.from(map.values());
  }, [filteredSongs]);

  const artists = useMemo(() => {
    const map = new Map<string, { name: string; artwork: string; songs: Track[] }>();
    for (const t of filteredSongs) {
      const existing = map.get(t.artist);
      if (existing) {
        existing.songs.push(t);
      } else {
        map.set(t.artist, { name: t.artist, artwork: t.artwork, songs: [t] });
      }
    }
    return Array.from(map.values());
  }, [filteredSongs]);

  const folders = useMemo(() => {
    const map = new Map<string, { path: string; artwork: string; songs: Track[] }>();
    for (const t of filteredSongs) {
      const existing = map.get(t.folder);
      if (existing) {
        existing.songs.push(t);
      } else {
        map.set(t.folder, { path: t.folder, artwork: t.artwork, songs: [t] });
      }
    }
    return Array.from(map.values());
  }, [filteredSongs]);

  const genres = useMemo(() => {
    const map = new Map<string, { name: string; artwork: string; songs: Track[] }>();
    for (const t of filteredSongs) {
      const existing = map.get(t.genre);
      if (existing) {
        existing.songs.push(t);
      } else {
        map.set(t.genre, { name: t.genre, artwork: t.artwork, songs: [t] });
      }
    }
    return Array.from(map.values());
  }, [filteredSongs]);

  if (selectedCollection) {
    return (
      <div className="space-y-5">
        <LiquidGlassCard className="p-5">
          <div className="flex flex-wrap items-center gap-5">
            <LiquidGlassIconButton label="Back to Library" onClick={() => setSelectedCollection(null)}>
              <ArrowLeft className="h-5 w-5" />
            </LiquidGlassIconButton>

            <img
              src={selectedCollection.artwork}
              alt={selectedCollection.title}
              referrerPolicy="no-referrer"
              className="h-20 w-20 rounded-2xl object-cover shadow-lg sm:h-24 sm:w-24"
            />

            <div className="min-w-0 flex-1">
              <h2 className="truncate text-2xl font-bold text-slate-900 dark:text-white">
                {selectedCollection.title}
              </h2>
              <p className="mt-1 truncate text-sm text-slate-600 dark:text-slate-300">
                {selectedCollection.subtitle} · {selectedCollection.songs.length} tracks
              </p>
            </div>

            <LiquidGlassButton
              highlighted
              accent={accent}
              onClick={() => {
                if (selectedCollection.songs.length > 0) {
                  onPlayTrack(selectedCollection.songs[0], selectedCollection.songs);
                }
              }}
            >
              <Play className="h-4 w-4 fill-current" />
              <span>Play Collection</span>
            </LiquidGlassButton>
          </div>
        </LiquidGlassCard>

        <div className="space-y-2.5">
          {selectedCollection.songs.map((song) => (
            <SongRowCard
              key={song.id}
              song={song}
              isCurrent={song.id === currentTrackId}
              isPlaying={isPlaying}
              isFavorite={favoriteIds.has(song.id)}
              accent={accent}
              onPlay={() => onPlayTrack(song, selectedCollection.songs)}
              onToggleFavorite={() => onToggleFavorite(song.id)}
              onAddToPlaylist={() => onAddTrackToPlaylistPrompt(song)}
            />
          ))}
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-5">
      {/* Header + Search + Import */}
      <LiquidGlassCard className="p-5">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <h2 className="text-xl font-bold tracking-tight text-slate-900 dark:text-white">Music Library</h2>
            <p className="mt-0.5 text-sm text-slate-600 dark:text-slate-300">
              {filteredSongs.length} tracks · MP3, FLAC, WAV, AAC, M4A, OGG, OPUS
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-2.5">
            <label className="inline-flex min-h-[44px] cursor-pointer items-center gap-2 whitespace-nowrap shrink-0 rounded-2xl border border-white/80 bg-white/75 px-4 py-2 text-sm font-semibold text-slate-900 shadow-sm backdrop-blur-xl transition-transform hover:bg-white active:scale-95 dark:border-white/15 dark:bg-[#1C1E29]/80 dark:text-slate-100 dark:hover:bg-[#252836]">
              <Upload className="h-4 w-4" style={{ color: accent.hex }} />
              <span>Add Audio Files</span>
              <input
                type="file"
                multiple
                accept="audio/*,.flac,.mp3,.wav,.aac,.m4a,.ogg,.opus"
                className="hidden"
                onChange={(e) => {
                  if (e.target.files && e.target.files.length > 0) {
                    onImportFiles(e.target.files);
                    e.target.value = '';
                  }
                }}
              />
            </label>
          </div>
        </div>

        {/* Search & Sort Controls */}
        <div className="mt-4 flex flex-col gap-3 sm:flex-row sm:items-center">
          <div className="relative flex-1">
            <Search
              className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2"
              style={{ color: accent.hex }}
            />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => onSearchChange(e.target.value)}
              placeholder="Search tracks, albums, artists, folders, genres..."
              className="h-11 w-full rounded-2xl border border-slate-900/10 bg-slate-900/[0.04] pl-10 pr-10 text-sm text-slate-900 placeholder-slate-500 focus:outline-none dark:border-white/15 dark:bg-white/[0.06] dark:text-white dark:placeholder-slate-400"
            />
            {searchQuery && (
              <button
                type="button"
                aria-label="Clear search"
                onClick={() => onSearchChange('')}
                className="absolute right-2 top-1/2 flex h-7 w-7 -translate-y-1/2 items-center justify-center rounded-full text-slate-500 hover:bg-slate-900/10 dark:text-slate-300 dark:hover:bg-white/10"
              >
                <X className="h-3.5 w-3.5" />
              </button>
            )}
          </div>

          <div className="flex items-center gap-2 rounded-2xl border border-slate-900/10 bg-slate-900/[0.04] px-3 dark:border-white/15 dark:bg-white/[0.06]">
            <ArrowUpDown className="h-4 w-4 text-slate-500 dark:text-slate-400" />
            <select
              aria-label="Sort tracks"
              value={sortBy}
              onChange={(e) => setSortBy(e.target.value as SortField)}
              className="h-11 bg-transparent text-xs font-semibold text-slate-900 focus:outline-none dark:text-slate-100"
            >
              <option value="NAME" className="bg-slate-900 text-white">Sort: Name</option>
              <option value="ARTIST" className="bg-slate-900 text-white">Sort: Artist</option>
              <option value="ALBUM" className="bg-slate-900 text-white">Sort: Album</option>
              <option value="DURATION" className="bg-slate-900 text-white">Sort: Duration</option>
              <option value="DATE_ADDED" className="bg-slate-900 text-white">Sort: Date Added</option>
            </select>
          </div>
        </div>

        {/* Category Filter Tabs */}
        <div className="nova-scroll mt-4 flex items-center gap-1.5 overflow-x-auto pb-1">
          {(
            [
              { id: 'SONGS', label: 'Songs', icon: Music2 },
              { id: 'ALBUMS', label: 'Albums', icon: Disc },
              { id: 'ARTISTS', label: 'Artists', icon: User },
              { id: 'FOLDERS', label: 'Folders', icon: Folder },
              { id: 'GENRES', label: 'Genres', icon: Tag },
            ] as const
          ).map((tab) => {
            const Icon = tab.icon;
            const active = activeTab === tab.id;
            return (
              <button
                key={tab.id}
                type="button"
                onClick={() => setActiveTab(tab.id)}
                style={
                  active
                    ? {
                        borderColor: accent.hex,
                        backgroundColor: `${accent.hex}24`,
                        color: accent.hex,
                      }
                    : undefined
                }
                className={`inline-flex min-h-[40px] items-center gap-1.5 whitespace-nowrap shrink-0 rounded-xl border px-3.5 py-2 text-xs font-semibold transition-colors ${
                  active
                    ? ''
                    : 'border-slate-900/10 bg-slate-900/[0.04] text-slate-700 hover:bg-slate-900/10 dark:border-white/10 dark:bg-white/[0.05] dark:text-slate-300 dark:hover:bg-white/10'
                }`}
              >
                <Icon className="h-3.5 w-3.5" />
                <span>{tab.label}</span>
              </button>
            );
          })}
        </div>
      </LiquidGlassCard>

      {/* Active Category View */}
      {activeTab === 'SONGS' && (
        <div className="space-y-2.5">
          {filteredSongs.length === 0 ? (
            <LiquidGlassCard className="p-8 text-center">
              <p className="text-base font-semibold text-slate-900 dark:text-white">No tracks match "{searchQuery}"</p>
              <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
                Try clearing your filter or adding local audio files.
              </p>
            </LiquidGlassCard>
          ) : (
            filteredSongs.map((song) => (
              <SongRowCard
                key={song.id}
                song={song}
                isCurrent={song.id === currentTrackId}
                isPlaying={isPlaying}
                isFavorite={favoriteIds.has(song.id)}
                accent={accent}
                onPlay={() => onPlayTrack(song, filteredSongs)}
                onToggleFavorite={() => onToggleFavorite(song.id)}
                onAddToPlaylist={() => onAddTrackToPlaylistPrompt(song)}
              />
            ))
          )}
        </div>
      )}

      {activeTab === 'ALBUMS' && (
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">
          {albums.map((album) => (
            <LiquidGlassCard
              key={album.name}
              className="p-3"
              onClick={() =>
                setSelectedCollection({
                  title: album.name,
                  subtitle: `${album.artist} · ${album.year}`,
                  artwork: album.artwork,
                  songs: album.songs,
                })
              }
            >
              <div className="aspect-square overflow-hidden rounded-2xl">
                <img
                  src={album.artwork}
                  alt={album.name}
                  referrerPolicy="no-referrer"
                  className="h-full w-full object-cover"
                />
              </div>
              <div className="mt-3 px-1">
                <p className="truncate text-sm font-bold text-slate-900 dark:text-white">{album.name}</p>
                <p className="truncate text-xs text-slate-600 dark:text-slate-400">
                  {album.artist} · {album.songs.length} tracks
                </p>
              </div>
            </LiquidGlassCard>
          ))}
        </div>
      )}

      {activeTab === 'ARTISTS' && (
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {artists.map((artist) => (
            <LiquidGlassCard
              key={artist.name}
              className="p-4"
              onClick={() =>
                setSelectedCollection({
                  title: artist.name,
                  subtitle: 'Artist Collection',
                  artwork: artist.artwork,
                  songs: artist.songs,
                })
              }
            >
              <div className="flex items-center gap-3.5">
                <img
                  src={artist.artwork}
                  alt={artist.name}
                  referrerPolicy="no-referrer"
                  className="h-14 w-14 shrink-0 rounded-full border-2 object-cover"
                  style={{ borderColor: accent.hex }}
                />
                <div className="min-w-0">
                  <p className="truncate text-base font-bold text-slate-900 dark:text-white">{artist.name}</p>
                  <p className="truncate text-xs text-slate-600 dark:text-slate-400">
                    {artist.songs.length} tracks
                  </p>
                </div>
              </div>
            </LiquidGlassCard>
          ))}
        </div>
      )}

      {activeTab === 'FOLDERS' && (
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          {folders.map((folder) => (
            <LiquidGlassCard
              key={folder.path}
              className="p-4"
              onClick={() =>
                setSelectedCollection({
                  title: folder.path.split('/').pop() || folder.path,
                  subtitle: folder.path,
                  artwork: folder.artwork,
                  songs: folder.songs,
                })
              }
            >
              <div className="flex items-center gap-3.5">
                <div
                  className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl"
                  style={{ backgroundColor: `${accent.hex}22`, color: accent.hex }}
                >
                  <Folder className="h-5 w-5" />
                </div>
                <div className="min-w-0">
                  <p className="truncate text-base font-bold text-slate-900 dark:text-white">{folder.path}</p>
                  <p className="truncate text-xs text-slate-600 dark:text-slate-400">
                    {folder.songs.length} audio files
                  </p>
                </div>
              </div>
            </LiquidGlassCard>
          ))}
        </div>
      )}

      {activeTab === 'GENRES' && (
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
          {genres.map((genre) => (
            <LiquidGlassCard
              key={genre.name}
              className="p-4"
              onClick={() =>
                setSelectedCollection({
                  title: genre.name,
                  subtitle: 'Genre',
                  artwork: genre.artwork,
                  songs: genre.songs,
                })
              }
            >
              <div className="flex items-center gap-3">
                <div
                  className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl"
                  style={{ backgroundColor: `${accent.hex}22`, color: accent.hex }}
                >
                  <Tag className="h-4 w-4" />
                </div>
                <div className="min-w-0">
                  <p className="truncate text-sm font-bold text-slate-900 dark:text-white">{genre.name}</p>
                  <p className="text-xs text-slate-600 dark:text-slate-400">{genre.songs.length} tracks</p>
                </div>
              </div>
            </LiquidGlassCard>
          ))}
        </div>
      )}
    </div>
  );
};

export const SongRowCard: React.FC<{
  song: Track;
  isCurrent: boolean;
  isPlaying: boolean;
  isFavorite: boolean;
  accent: AccentOption;
  onPlay: () => void;
  onToggleFavorite: () => void;
  onAddToPlaylist: () => void;
}> = ({ song, isCurrent, isPlaying, isFavorite, accent, onPlay, onToggleFavorite, onAddToPlaylist }) => {
  return (
    <LiquidGlassCard
      highlighted={isCurrent}
      accentHex={accent.hex}
      className="px-3.5 py-2.5"
    >
      <div className="flex items-center gap-3">
        <button
          type="button"
          onClick={onPlay}
          className="flex min-w-0 flex-1 items-center gap-3.5 text-left"
        >
          <div className="relative h-12 w-12 shrink-0 overflow-hidden rounded-xl">
            <img
              src={song.artwork}
              alt={song.album}
              referrerPolicy="no-referrer"
              className="h-full w-full object-cover"
            />
            {isCurrent && (
              <div className="absolute inset-0 flex items-center justify-center bg-black/50">
                {isPlaying ? (
                  <Pause className="h-4 w-4 fill-current text-white" />
                ) : (
                  <Play className="h-4 w-4 fill-current text-white" />
                )}
              </div>
            )}
          </div>

          <div className="min-w-0 flex-1">
            <p
              style={isCurrent ? { color: accent.hex } : undefined}
              className="truncate text-sm font-semibold text-slate-900 dark:text-white"
            >
              {song.title}
            </p>
            <p className="truncate text-xs text-slate-600 dark:text-slate-400">
              {song.artist} · {song.album} · <span className="font-mono tabular-nums">{song.format}</span>
            </p>
          </div>
        </button>

        <span className="hidden font-mono text-xs tabular-nums text-slate-500 dark:text-slate-400 sm:inline">
          {formatTime(song.durationSec)}
        </span>

        <button
          type="button"
          aria-label={isFavorite ? 'Unfavorite' : 'Favorite'}
          onClick={onToggleFavorite}
          style={isFavorite ? { color: accent.hex } : undefined}
          className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full text-slate-500 transition-colors hover:bg-slate-900/10 dark:text-slate-400 dark:hover:bg-white/10"
        >
          <Heart className={`h-4 w-4 ${isFavorite ? 'fill-current' : ''}`} />
        </button>

        <button
          type="button"
          aria-label={`Add ${song.title} to playlist`}
          onClick={onAddToPlaylist}
          className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full text-slate-500 transition-colors hover:bg-slate-900/10 dark:text-slate-400 dark:hover:bg-white/10"
        >
          <Plus className="h-4 w-4" />
        </button>
      </div>
    </LiquidGlassCard>
  );
};
