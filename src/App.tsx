/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import { useEffect, useMemo, useState } from 'react';
import {
  Check,
  Code2,
  Copy,
  Cpu,
  Database,
  FileCode2,
  FolderTree,
  HardDrive,
  Sliders,
  Smartphone,
  Terminal,
} from 'lucide-react';

const rawFiles = import.meta.glob(
  [
    '/app/src/main/java/**/*.kt',
    '/app/src/main/AndroidManifest.xml',
    '/app/build.gradle.kts',
    '/app/proguard-rules.pro',
    '/gradle/libs.versions.toml',
    '/settings.gradle.kts',
    '/keystore.properties.example',
  ],
  { query: '?raw', import: 'default' }
) as Record<string, () => Promise<string>>;

const ANDROID_MODULES = [
  {
    icon: Smartphone,
    title: 'Media3 ExoPlayer & Background Service',
    detail: 'MusicService.kt + AudioPlayerManager.kt with MediaSession, lock screen controls, Bluetooth AVRCP, audio focus, and headphone disconnect handling.',
  },
  {
    icon: Sliders,
    title: 'Hardware AudioEffect Equalizer',
    detail: 'EqualizerManager.kt binds to ExoPlayer audioSessionId using DynamicsProcessing (10-band + Preamp on API 28+), Equalizer fallback, BassBoost, Virtualizer, and LoudnessEnhancer.',
  },
  {
    icon: HardDrive,
    title: 'Offline MediaStore & SAF Browser',
    detail: 'MusicScanner.kt scans MP3, FLAC, WAV, AAC, M4A, OGG, and OPUS tracks offline with MediaMetadataRetriever audio info and SAF folder/file access.',
  },
  {
    icon: Database,
    title: 'Room Database & DataStore',
    detail: 'NovaDatabase.kt + PreferencesRepository.kt persist playlists, track order, favorites, playback history, custom EQ presets, lyrics, and resume position.',
  },
];

export default function App() {
  const filePaths = useMemo(() => Object.keys(rawFiles).sort(), []);
  const [selectedPath, setSelectedPath] = useState<string>(
    () => filePaths.find((p) => p.endsWith('MainActivity.kt')) ?? filePaths[0] ?? ''
  );
  const [fileContent, setFileContent] = useState<string>('');
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    if (!selectedPath || !rawFiles[selectedPath]) return;
    let cancelled = false;
    rawFiles[selectedPath]().then((text) => {
      if (!cancelled) {
        setFileContent(text);
      }
    });
    return () => {
      cancelled = true;
    };
  }, [selectedPath]);

  const handleCopy = async () => {
    if (!fileContent) return;
    await navigator.clipboard.writeText(fileContent);
    setCopied(true);
    setTimeout(() => setCopied(false), 1800);
  };

  return (
    <div className="min-h-screen bg-[#080808] text-[#F5F5F7] font-sans selection:bg-[#FF8A32]/30">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 py-8 space-y-6">
        {/* Header */}
        <header className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 bg-[#151515] border border-[#282828] rounded-2xl p-6">
          <div className="flex items-center gap-4">
            <div className="w-12 h-12 rounded-2xl bg-[#FF8A32]/15 border border-[#FF8A32]/40 flex items-center justify-center shrink-0">
              <Cpu className="w-6 h-6 text-[#FF8A32]" />
            </div>
            <div>
              <div className="flex items-center gap-2.5">
                <h1 className="text-xl sm:text-2xl font-bold tracking-tight">NOVA MUSIC</h1>
                <span className="text-[11px] font-semibold uppercase tracking-wider px-2 py-0.5 rounded bg-[#FF8A32]/15 text-[#FF8A32] border border-[#FF8A32]/30">
                  Native Android APK Project
                </span>
              </div>
              <p className="text-sm text-[#9E9EA8] mt-1">
                Built with Kotlin, Jetpack Compose, Material 3, Media3 ExoPlayer, Room, DataStore & Android AudioEffect APIs.
              </p>
            </div>
          </div>

          <div className="bg-[#080808] border border-[#282828] rounded-xl px-4 py-3 text-xs text-[#9E9EA8] max-w-md">
            <span className="text-[#FF8A32] font-semibold">Why no live player in this web preview?</span>{' '}
            This project is a real native Android Kotlin app (not a web simulation). Browser previews cannot run Android APK binaries or hardware DSP AudioEffects — open the workspace in Android Studio to compile & run on a device.
          </div>
        </header>

        {/* Architecture Overview */}
        <section className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          {ANDROID_MODULES.map((mod) => {
            const Icon = mod.icon;
            return (
              <div
                key={mod.title}
                className="bg-[#151515] border border-[#282828] rounded-2xl p-4 space-y-2"
              >
                <div className="flex items-center gap-2 text-[#FF8A32] font-semibold text-sm">
                  <Icon className="w-4 h-4 shrink-0" />
                  <span>{mod.title}</span>
                </div>
                <p className="text-xs text-[#9E9EA8] leading-relaxed">{mod.detail}</p>
              </div>
            );
          })}
        </section>

        {/* Build Instructions */}
        <section className="bg-[#151515] border border-[#282828] rounded-2xl p-5 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div className="flex items-start gap-3">
            <Terminal className="w-5 h-5 text-[#FF8A32] mt-0.5 shrink-0" />
            <div>
              <h2 className="text-sm font-semibold">Build & Install APK</h2>
              <p className="text-xs text-[#9E9EA8] mt-0.5">
                Export or clone the workspace, open the root folder in Android Studio (JDK 17 + Android SDK 35), or run Gradle directly:
              </p>
            </div>
          </div>
          <div className="flex flex-wrap gap-2 shrink-0">
            <code className="bg-[#080808] border border-[#282828] rounded-xl px-4 py-2.5 text-xs text-[#F5F5F7] font-mono">
              ./gradlew assembleDebug
            </code>
            <code className="bg-[#080808] border border-[#FF8A32]/40 rounded-xl px-4 py-2.5 text-xs text-[#FF8A32] font-mono">
              ./gradlew assembleRelease
            </code>
          </div>
        </section>

        {/* Real Kotlin & Gradle Source Inspector */}
        <section className="grid grid-cols-1 lg:grid-cols-12 gap-4">
          <div className="lg:col-span-4 bg-[#151515] border border-[#282828] rounded-2xl overflow-hidden flex flex-col max-h-[560px]">
            <div className="px-4 py-3 border-b border-[#282828] flex items-center justify-between">
              <div className="flex items-center gap-2 text-sm font-semibold">
                <FolderTree className="w-4 h-4 text-[#FF8A32]" />
                <span>Android Source Files ({filePaths.length})</span>
              </div>
            </div>
            <div className="overflow-y-auto p-2 space-y-1">
              {filePaths.map((path) => {
                const isSelected = path === selectedPath;
                const shortPath = path.replace('/app/src/main/java/com/novamusic/player/', '');
                return (
                  <button
                    key={path}
                    onClick={() => setSelectedPath(path)}
                    className={`w-full text-left px-3 py-2 rounded-xl text-xs font-mono flex items-center gap-2 transition-colors ${
                      isSelected
                        ? 'bg-[#FF8A32]/15 text-[#FF8A32] border border-[#FF8A32]/30'
                        : 'text-[#9E9EA8] hover:bg-[#1F1F1F] hover:text-[#F5F5F7]'
                    }`}
                  >
                    <FileCode2 className="w-3.5 h-3.5 shrink-0" />
                    <span className="truncate">{shortPath}</span>
                  </button>
                );
              })}
            </div>
          </div>

          <div className="lg:col-span-8 bg-[#151515] border border-[#282828] rounded-2xl overflow-hidden flex flex-col max-h-[560px]">
            <div className="px-4 py-3 border-b border-[#282828] flex items-center justify-between gap-2">
              <div className="flex items-center gap-2 min-w-0">
                <Code2 className="w-4 h-4 text-[#FF8A32] shrink-0" />
                <span className="text-xs font-mono text-[#F5F5F7] truncate">{selectedPath}</span>
              </div>
              <button
                onClick={handleCopy}
                className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-[#1F1F1F] hover:bg-[#282828] text-xs font-medium text-[#F5F5F7] shrink-0"
              >
                {copied ? <Check className="w-3.5 h-3.5 text-[#2CE59B]" /> : <Copy className="w-3.5 h-3.5" />}
                <span>{copied ? 'Copied' : 'Copy'}</span>
              </button>
            </div>
            <pre className="p-4 overflow-auto text-xs font-mono leading-relaxed text-[#E5E5EA] bg-[#080808] flex-1">
              <code>{fileContent || 'Loading source file...'}</code>
            </pre>
          </div>
        </section>
      </div>
    </div>
  );
}
