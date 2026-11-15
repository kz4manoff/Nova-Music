import albumObsidianPulse from '../assets/images/album_obsidian_pulse_1794700877583.jpg';
import albumSolarDrift from '../assets/images/album_solar_drift_1794700878870.jpg';
import albumMidnightVinyl from '../assets/images/album_midnight_vinyl_1794700878928.jpg';
import albumCrystalReverb from '../assets/images/album_crystal_reverb_1794700879385.jpg';
import albumVelvetHorizon from '../assets/images/album_velvet_horizon_1794700879100.jpg';

export type ThemeMode = 'dark' | 'light';
export type ArtworkStyle = 'ROUNDED_CARD' | 'ROTATING_VINYL' | 'IMMERSIVE_GLOW' | 'EDITORIAL_SQUARE';
export type RepeatMode = 'OFF' | 'ALL' | 'ONE';
export type LibraryTab = 'SONGS' | 'ALBUMS' | 'ARTISTS' | 'FOLDERS' | 'GENRES';
export type SortField = 'NAME' | 'ARTIST' | 'ALBUM' | 'DURATION' | 'DATE_ADDED';
export type AppTab = 'home' | 'library' | 'playlists' | 'equalizer' | 'settings';

export interface AccentOption {
  id: string;
  label: string;
  hex: string;
  glow: string;
  deep: string;
}

export const ACCENT_OPTIONS: AccentOption[] = [
  { id: 'orange', label: 'Electric Orange', hex: '#FF8A32', glow: '#FFB36B', deep: '#D95F0E' },
  { id: 'cyan', label: 'Aurora Cyan', hex: '#28C7E8', glow: '#74E3F8', deep: '#0E95B5' },
  { id: 'gold', label: 'Solar Gold', hex: '#F5B82E', glow: '#FAD473', deep: '#C68A0A' },
  { id: 'crimson', label: 'Crimson Pulse', hex: '#F45B69', glow: '#FF8E99', deep: '#C43241' },
  { id: 'mint', label: 'Emerald Mint', hex: '#2CE59B', glow: '#79F2C1', deep: '#11AE70' },
];

export interface LyricLine {
  timeSec: number;
  text: string;
}

export interface SynthRecipe {
  rootHz: number;
  chords: number[][];
  bpm: number;
  brightness: number;
}

export interface Track {
  id: string;
  title: string;
  artist: string;
  album: string;
  genre: string;
  folder: string;
  year: number;
  durationSec: number;
  artwork: string;
  format: 'FLAC' | 'WAV' | 'OPUS' | 'M4A' | 'MP3';
  sampleRateHz: number;
  bitrateKbps: number;
  dateAdded: number;
  playCount: number;
  lyrics: LyricLine[];
  synth?: SynthRecipe;
  localUrl?: string;
}

export interface Playlist {
  id: string;
  name: string;
  songIds: string[];
}

export const EQ_BANDS = [
  { index: 0, freq: 31, label: '31Hz' },
  { index: 1, freq: 62, label: '62Hz' },
  { index: 2, freq: 125, label: '125Hz' },
  { index: 3, freq: 250, label: '250Hz' },
  { index: 4, freq: 500, label: '500Hz' },
  { index: 5, freq: 1000, label: '1kHz' },
  { index: 6, freq: 2000, label: '2kHz' },
  { index: 7, freq: 4000, label: '4kHz' },
  { index: 8, freq: 8000, label: '8kHz' },
  { index: 9, freq: 16000, label: '16kHz' },
];

export interface EqPreset {
  name: string;
  gainsDb: number[]; // 10 bands, -12 to +12
}

export const BUILT_IN_EQ_PRESETS: EqPreset[] = [
  { name: 'Flat', gainsDb: [0, 0, 0, 0, 0, 0, 0, 0, 0, 0] },
  { name: 'Bass Boost', gainsDb: [6.5, 5.5, 4.0, 2.0, 0, -0.5, 0, 1.0, 1.5, 2.0] },
  { name: 'Vocal Clarity', gainsDb: [-2.0, -1.5, 0, 1.5, 3.5, 4.5, 4.0, 2.5, 1.0, 0] },
  { name: 'Electronic', gainsDb: [5.0, 4.0, 1.5, -1.0, -2.0, 0.5, 2.5, 3.5, 4.5, 5.0] },
  { name: 'Studio Warmth', gainsDb: [2.5, 3.0, 2.0, 1.0, 0.5, 0, 0.5, 1.5, 2.0, 1.0] },
  { name: 'Acoustic', gainsDb: [1.5, 2.0, 1.5, 0.5, 1.0, 2.0, 3.0, 2.5, 2.0, 1.5] },
  { name: 'Night Drive', gainsDb: [4.0, 3.5, 2.0, 0, -1.5, 0, 2.0, 3.5, 3.0, 2.5] },
];

export const INITIAL_TRACKS: Track[] = [
  {
    id: 'track-obsidian-1',
    title: 'Obsidian Refraction',
    artist: 'Kaelen Vance',
    album: 'Obsidian Pulse',
    genre: 'Electronic',
    folder: '/Music/Lossless',
    year: 2026,
    durationSec: 28,
    artwork: albumObsidianPulse,
    format: 'FLAC',
    sampleRateHz: 96000,
    bitrateKbps: 2840,
    dateAdded: 1794690000,
    playCount: 24,
    synth: {
      rootHz: 110,
      chords: [
        [220, 261.63, 329.63, 392.0],
        [174.61, 220, 261.63, 349.23],
        [196.0, 246.94, 293.66, 392.0],
        [164.81, 196.0, 246.94, 329.63],
      ],
      bpm: 108,
      brightness: 1.0,
    },
    lyrics: [
      { timeSec: 0, text: 'Light bends slowly through obsidian glass' },
      { timeSec: 5, text: 'Amber echoes on the dark studio floor' },
      { timeSec: 10, text: 'Sub-bass ripples where the frequencies pass' },
      { timeSec: 15, text: 'Hold the resonance inside the quiet core' },
      { timeSec: 21, text: 'Every transient dissolves in golden haze' },
    ],
  },
  {
    id: 'track-solar-1',
    title: 'Amber Dunes',
    artist: 'Soraia & Sol',
    album: 'Solar Drift',
    genre: 'Ambient',
    folder: '/Music/Ambient',
    year: 2025,
    durationSec: 28,
    artwork: albumSolarDrift,
    format: 'FLAC',
    sampleRateHz: 48000,
    bitrateKbps: 1620,
    dateAdded: 1794680000,
    playCount: 18,
    synth: {
      rootHz: 98,
      chords: [
        [196.0, 246.94, 293.66, 369.99],
        [220.0, 261.63, 329.63, 440.0],
        [164.81, 207.65, 246.94, 329.63],
        [146.83, 220.0, 261.63, 329.63],
      ],
      bpm: 84,
      brightness: 0.78,
    },
    lyrics: [
      { timeSec: 0, text: 'Copper shadows climb the evening ridge' },
      { timeSec: 6, text: 'Warm wind carrying an analog tone' },
      { timeSec: 12, text: 'Tape saturation across the silent bridge' },
      { timeSec: 18, text: 'Horizon glowing where the sun has gone' },
    ],
  },
  {
    id: 'track-midnight-1',
    title: 'Titanium Grooves',
    artist: 'Nocturne Quartet',
    album: 'Midnight Vinyl',
    genre: 'Nu-Jazz',
    folder: '/Music/Lossless',
    year: 2026,
    durationSec: 28,
    artwork: albumMidnightVinyl,
    format: 'WAV',
    sampleRateHz: 96000,
    bitrateKbps: 4608,
    dateAdded: 1794670000,
    playCount: 31,
    synth: {
      rootHz: 130.81,
      chords: [
        [261.63, 311.13, 392.0, 466.16],
        [233.08, 293.66, 349.23, 440.0],
        [196.0, 246.94, 293.66, 369.99],
        [207.65, 261.63, 311.13, 392.0],
      ],
      bpm: 96,
      brightness: 1.15,
    },
    lyrics: [
      { timeSec: 0, text: 'Silver needle traces concentric steel' },
      { timeSec: 6, text: 'Midnight chords in brushed titanium spin' },
      { timeSec: 13, text: 'Every harmonic crisp and tactile to feel' },
      { timeSec: 20, text: 'Let the late-night session slowly begin' },
    ],
  },
  {
    id: 'track-crystal-1',
    title: 'Crystal Chamber',
    artist: 'Elena Maris',
    album: 'Crystal Reverb',
    genre: 'Neoclassical',
    folder: '/Music/Studio Masters',
    year: 2026,
    durationSec: 28,
    artwork: albumCrystalReverb,
    format: 'OPUS',
    sampleRateHz: 48000,
    bitrateKbps: 512,
    dateAdded: 1794660000,
    playCount: 15,
    synth: {
      rootHz: 146.83,
      chords: [
        [293.66, 349.23, 440.0, 523.25],
        [261.63, 329.63, 392.0, 493.88],
        [220.0, 261.63, 329.63, 440.0],
        [246.94, 293.66, 369.99, 440.0],
      ],
      bpm: 78,
      brightness: 0.9,
    },
    lyrics: [
      { timeSec: 0, text: 'Frosted glass catches a sliver of gold' },
      { timeSec: 7, text: 'Quiet chamber resonance fills the hall' },
      { timeSec: 14, text: 'Clear harmonic voices gently unfold' },
      { timeSec: 21, text: 'Soft reflections answer down the wall' },
    ],
  },
  {
    id: 'track-velvet-1',
    title: 'Fluted Horizon',
    artist: 'Milo & The Glass',
    album: 'Velvet Horizon',
    genre: 'Synthwave',
    folder: '/Music/Electronic',
    year: 2025,
    durationSec: 28,
    artwork: albumVelvetHorizon,
    format: 'M4A',
    sampleRateHz: 48000,
    bitrateKbps: 320,
    dateAdded: 1794650000,
    playCount: 19,
    synth: {
      rootHz: 123.47,
      chords: [
        [246.94, 293.66, 369.99, 440.0],
        [196.0, 246.94, 293.66, 392.0],
        [220.0, 277.18, 329.63, 440.0],
        [185.0, 220.0, 277.18, 369.99],
      ],
      bpm: 112,
      brightness: 1.05,
    },
    lyrics: [
      { timeSec: 0, text: 'Coral dawn diffused behind fluted glass' },
      { timeSec: 6, text: 'Analog warmth humming along the skyline' },
      { timeSec: 12, text: 'Drifting through the boulevard as hours pass' },
      { timeSec: 19, text: 'Chasing every sunset frequency line' },
    ],
  },
  {
    id: 'track-obsidian-2',
    title: 'Specular Echo',
    artist: 'Kaelen Vance',
    album: 'Obsidian Pulse',
    genre: 'Electronic',
    folder: '/Music/Lossless',
    year: 2026,
    durationSec: 28,
    artwork: albumObsidianPulse,
    format: 'FLAC',
    sampleRateHz: 96000,
    bitrateKbps: 2690,
    dateAdded: 1794640000,
    playCount: 11,
    synth: {
      rootHz: 110,
      chords: [
        [220.0, 277.18, 329.63, 415.3],
        [196.0, 246.94, 293.66, 392.0],
        [174.61, 220.0, 261.63, 349.23],
        [164.81, 207.65, 246.94, 329.63],
      ],
      bpm: 102,
      brightness: 0.95,
    },
    lyrics: [
      { timeSec: 0, text: 'Dark mirror catching the pulse of the room' },
      { timeSec: 8, text: 'Low-end clarity cutting through the night' },
      { timeSec: 16, text: 'Warm orange transients flicker and bloom' },
    ],
  },
];

export const INITIAL_PLAYLISTS: Playlist[] = [
  {
    id: 'pl-late-night',
    name: 'Late Night Glass',
    songIds: ['track-obsidian-1', 'track-midnight-1', 'track-velvet-1'],
  },
  {
    id: 'pl-lossless',
    name: 'Hi-Res Studio Test',
    songIds: ['track-midnight-1', 'track-obsidian-1', 'track-crystal-1', 'track-solar-1'],
  },
];

export function formatTime(seconds: number): string {
  const safe = Math.max(0, Math.floor(seconds));
  const mins = Math.floor(safe / 60);
  const secs = safe % 60;
  return `${mins}:${secs.toString().padStart(2, '0')}`;
}
