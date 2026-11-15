import { EQ_BANDS, SynthRecipe } from '../data/novaLibrary';

/**
 * Renders a stereo offline WAV audio blob from a harmonic synth recipe so the
 * HTML5 <audio> element + Web Audio 10-band BiquadFilterNode DSP pipeline work
 * identically for built-in showcase tracks and real user-imported MP3/FLAC/WAV files.
 */
export function buildProceduralTrackWavUrl(recipe: SynthRecipe, durationSec = 28): string {
  const sampleRate = 22050;
  const numChannels = 2;
  const numSamples = Math.floor(sampleRate * durationSec);
  const bytesPerSample = 2;
  const blockAlign = numChannels * bytesPerSample;
  const dataSize = numSamples * blockAlign;
  const buffer = new ArrayBuffer(44 + dataSize);
  const view = new DataView(buffer);

  const writeStr = (offset: number, s: string) => {
    for (let i = 0; i < s.length; i++) {
      view.setUint8(offset + i, s.charCodeAt(i));
    }
  };

  writeStr(0, 'RIFF');
  view.setUint32(4, 36 + dataSize, true);
  writeStr(8, 'WAVE');
  writeStr(12, 'fmt ');
  view.setUint32(16, 16, true);
  view.setUint16(20, 1, true); // PCM
  view.setUint16(22, numChannels, true);
  view.setUint32(24, sampleRate, true);
  view.setUint32(28, sampleRate * blockAlign, true);
  view.setUint16(32, blockAlign, true);
  view.setUint16(34, 16, true);
  writeStr(36, 'data');
  view.setUint32(40, dataSize, true);

  const beatSec = 60 / recipe.bpm;
  const barSec = beatSec * 4;
  const twoPi = Math.PI * 2;

  let offset = 44;
  for (let i = 0; i < numSamples; i++) {
    const t = i / sampleRate;
    const barIndex = Math.floor(t / barSec) % recipe.chords.length;
    const barProgress = (t % barSec) / barSec;
    const beatProgress = (t % beatSec) / beatSec;
    const chord = recipe.chords[barIndex];

    // Smooth fade-in and fade-out at track edges
    const edgeEnv = Math.min(1, t / 0.35) * Math.min(1, (durationSec - t) / 0.6);

    // Sub-bass warm sine pulse
    const bassFreq = chord[0] * 0.5;
    const bassEnv = Math.exp(-beatProgress * 2.2) * 0.65 + 0.35;
    const subBass = Math.sin(twoPi * bassFreq * t) * 0.32 * bassEnv;

    // Warm chord pad with gentle harmonic overtones
    let padL = 0;
    let padR = 0;
    const padEnv = Math.sin(Math.min(1, barProgress * 1.15) * Math.PI * 0.5) * (1 - Math.pow(barProgress, 4) * 0.35);
    for (let c = 0; c < chord.length; c++) {
      const f = chord[c];
      const detune = (c % 2 === 0 ? 1 : -1) * 0.0025;
      const toneL = Math.sin(twoPi * f * (1 + detune) * t) + 0.25 * recipe.brightness * Math.sin(twoPi * f * 2 * t);
      const toneR = Math.sin(twoPi * f * (1 - detune) * t) + 0.25 * recipe.brightness * Math.sin(twoPi * f * 2 * t);
      padL += toneL;
      padR += toneR;
    }
    padL = (padL / chord.length) * 0.28 * padEnv;
    padR = (padR / chord.length) * 0.28 * padEnv;

    // Delicate Rhodes/glass bell arpeggio
    const stepIdx = Math.floor(t / (beatSec * 0.5)) % chord.length;
    const stepPos = (t % (beatSec * 0.5)) / (beatSec * 0.5);
    const arpFreq = chord[stepIdx] * 2;
    const arpEnv = Math.exp(-stepPos * 4.5);
    const arpPan = stepIdx % 2 === 0 ? 0.82 : 0.45;
    const arpSig =
      (Math.sin(twoPi * arpFreq * t) + 0.3 * Math.sin(twoPi * arpFreq * 3 * t) * Math.exp(-stepPos * 8)) *
      0.16 *
      arpEnv;

    // Soft kick thump on beats 1 and 3
    const twoBeatPos = (t % (beatSec * 2)) / (beatSec * 2);
    const kickEnv = Math.exp(-twoBeatPos * 14);
    const kickFreq = 48 + 75 * Math.exp(-twoBeatPos * 28);
    const kick = Math.sin(twoPi * kickFreq * twoBeatPos) * 0.28 * kickEnv;

    const sampleL = Math.tanh((subBass + padL + arpSig * arpPan + kick) * edgeEnv * 0.9);
    const sampleR = Math.tanh((subBass + padR + arpSig * (1.25 - arpPan) + kick) * edgeEnv * 0.9);

    view.setInt16(offset, Math.max(-32767, Math.min(32767, Math.round(sampleL * 32767))), true);
    view.setInt16(offset + 2, Math.max(-32767, Math.min(32767, Math.round(sampleR * 32767))), true);
    offset += 4;
  }

  const blob = new Blob([buffer], { type: 'audio/wav' });
  return URL.createObjectURL(blob);
}

export class NovaWebAudioPipeline {
  private ctx: AudioContext | null = null;
  private source: MediaElementAudioSourceNode | null = null;
  private preampNode: GainNode | null = null;
  private eqNodes: BiquadFilterNode[] = [];
  private bassBoostNode: BiquadFilterNode | null = null;
  private loudnessNode: BiquadFilterNode | null = null;
  private analyser: AnalyserNode | null = null;
  private freqBuffer: Uint8Array<ArrayBuffer> | null = null;

  ensureConnected(audioEl: HTMLAudioElement) {
    if (this.ctx) {
      if (this.ctx.state === 'suspended') {
        void this.ctx.resume();
      }
      return;
    }

    const AudioCtx = window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext;
    if (!AudioCtx) return;

    const ctx = new AudioCtx();
    const source = ctx.createMediaElementSource(audioEl);
    const preamp = ctx.createGain();
    const bassBoost = ctx.createBiquadFilter();
    bassBoost.type = 'lowshelf';
    bassBoost.frequency.value = 95;
    bassBoost.gain.value = 0;

    const loudness = ctx.createBiquadFilter();
    loudness.type = 'highshelf';
    loudness.frequency.value = 3500;
    loudness.gain.value = 0;

    const eqNodes = EQ_BANDS.map((band, idx) => {
      const filter = ctx.createBiquadFilter();
      filter.type = idx === 0 ? 'lowshelf' : idx === EQ_BANDS.length - 1 ? 'highshelf' : 'peaking';
      filter.frequency.value = band.freq;
      filter.Q.value = 1.4;
      filter.gain.value = 0;
      return filter;
    });

    const analyser = ctx.createAnalyser();
    analyser.fftSize = 64;
    analyser.smoothingTimeConstant = 0.78;

    let current: AudioNode = source;
    current.connect(preamp);
    current = preamp;

    for (const node of eqNodes) {
      current.connect(node);
      current = node;
    }

    current.connect(bassBoost);
    bassBoost.connect(loudness);
    loudness.connect(analyser);
    analyser.connect(ctx.destination);

    this.ctx = ctx;
    this.source = source;
    this.preampNode = preamp;
    this.eqNodes = eqNodes;
    this.bassBoostNode = bassBoost;
    this.loudnessNode = loudness;
    this.analyser = analyser;
    this.freqBuffer = new Uint8Array(new ArrayBuffer(analyser.frequencyBinCount));
  }

  updateDsp(params: {
    enabled: boolean;
    bandGainsDb: number[];
    preampDb: number;
    bassBoostPct: number;
    loudnessDb: number;
  }) {
    if (!this.ctx || !this.preampNode || !this.bassBoostNode || !this.loudnessNode) return;
    const now = this.ctx.currentTime;

    if (!params.enabled) {
      this.preampNode.gain.setTargetAtTime(1, now, 0.02);
      this.bassBoostNode.gain.setTargetAtTime(0, now, 0.02);
      this.loudnessNode.gain.setTargetAtTime(0, now, 0.02);
      this.eqNodes.forEach((n) => n.gain.setTargetAtTime(0, now, 0.02));
      return;
    }

    const linearPreamp = Math.pow(10, params.preampDb / 20);
    this.preampNode.gain.setTargetAtTime(linearPreamp, now, 0.02);
    this.bassBoostNode.gain.setTargetAtTime((params.bassBoostPct / 100) * 9, now, 0.02);
    this.loudnessNode.gain.setTargetAtTime(params.loudnessDb * 0.45, now, 0.02);

    this.eqNodes.forEach((node, idx) => {
      const gain = params.bandGainsDb[idx] ?? 0;
      node.gain.setTargetAtTime(gain, now, 0.02);
    });
  }

  sampleSpectrum(barsCount = 8): number[] {
    if (!this.analyser || !this.freqBuffer) {
      return Array.from({ length: barsCount }, () => 0.15);
    }
    this.analyser.getByteFrequencyData(this.freqBuffer);
    const step = Math.max(1, Math.floor(this.freqBuffer.length / barsCount));
    const result: number[] = [];
    for (let i = 0; i < barsCount; i++) {
      const val = this.freqBuffer[Math.min(i * step, this.freqBuffer.length - 1)] ?? 0;
      result.push(Math.max(0.14, val / 255));
    }
    return result;
  }
}
