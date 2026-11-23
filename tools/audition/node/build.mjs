// Prompt 04, part 2a: extracts the vanilla wind candidates (local only), generates three wind beds, analyses every candidate, writes ../results.json and ../index.html.
// Run:  cd tools/audition/node && npm install && node build.mjs
import fs from 'fs';
import path from 'path';
import os from 'os';
import { createRequire } from 'module';
import { OggVorbisDecoder } from '@wasm-audio-decoders/ogg-vorbis';

const require = createRequire(import.meta.url);
const Encoder = require('vorbis-encoder-js').encoder;
const HERE = path.dirname(new URL(import.meta.url).pathname.replace(/^\/([A-Za-z]:)/, '$1'));
const OUT = path.resolve(HERE, '..');
const REPO = path.resolve(OUT, '..', '..');
const UP_DIR = path.join(REPO, 'src/main/resources/assets/islandcraft_weatherofarrakis/sounds/weather');
const ASSETS = path.join(os.homedir(), '.gradle/caches/neoformruntime/assets');

// ---------- vanilla candidates (event ids; the audio is extracted for local listening only) ----------
const VANILLA_EVENTS = [
  'ambient.basalt_deltas.loop', 'ambient.nether_wastes.loop', 'ambient.soul_sand_valley.loop', 'ambient.crimson_forest.loop', 'ambient.warped_forest.loop',
  'ambient.underwater.loop', 'ambient.cave', 'block.portal.ambient', 'block.beacon.ambient', 'block.lava.ambient', 'block.respawn_anchor.ambient',
  'item.elytra.flying', 'weather.rain', 'weather.rain.above', 'entity.breeze.idle_air', 'entity.breeze.whirl', 'entity.breeze.wind_burst',
  'entity.breeze.slide', 'entity.breeze.charge', 'entity.breeze.inhale', 'entity.wind_charge.wind_burst', 'entity.wind_charge.throw',
  'entity.parrot.imitate.breeze', 'entity.ender_dragon.flap', 'entity.phantom.swoop', 'block.bubble_column.whirlpool_ambient', 'entity.vex.charge',
];
const idx = JSON.parse(fs.readFileSync(path.join(ASSETS, 'indexes/30.json'))).objects;
const objPath = (h) => path.join(ASSETS, 'objects', h.slice(0, 2), h);
const soundsJson = JSON.parse(fs.readFileSync(objPath(idx['minecraft/sounds.json'].hash)));

const candidates = []; // {id, group, label, licence, file (relative to OUT), event?}
const VAN_DIR = path.join(OUT, 'vanilla');
fs.mkdirSync(VAN_DIR, { recursive: true });
for (const ev of VANILLA_EVENTS) {
  const def = soundsJson[ev];
  if (!def) { console.log('no event', ev); continue; }
  let k = 0;
  for (const s of def.sounds) {
    const name = typeof s === 'string' ? s : s.name;
    if (typeof s === 'object' && s.type === 'event') continue;
    const key = 'minecraft/sounds/' + name.replace(/^minecraft:/, '') + '.ogg';
    const e = idx[key];
    if (!e) { console.log('missing asset', key); continue; }
    k++;
    const rel = 'vanilla/' + ev + '/' + k + '.ogg';
    fs.mkdirSync(path.join(OUT, 'vanilla', ev), { recursive: true });
    fs.copyFileSync(objPath(e.hash), path.join(OUT, rel));
    candidates.push({ id: 'vanilla:' + ev + '#' + k, group: 'Vanilla (reference by id, usable at release)', label: ev + ' #' + k, event: 'minecraft:' + ev, licence: 'Mojang asset: reference the sound event id only; never copy or ship the file', file: rel, local: true });
  }
}

// ---------- upstream (Pixabay) ----------
for (const sub of ['wind', 'sandstorm']) {
  for (const f of fs.readdirSync(path.join(UP_DIR, sub)).sort()) {
    candidates.push({ id: 'upstream:' + sub + '/' + f, group: 'Upstream Immersive Storms (Pixabay, RELEASE BLOCKER)', label: sub + '/' + f, event: null, licence: 'Pixabay licence terms unconfirmed: release blocker', file: path.relative(OUT, path.join(UP_DIR, sub, f)).replace(/\\/g, '/') });
  }
}

// ---------- generated beds ----------
const SR = 44100, L = 12 * SR, F = SR; // 12 s loop, 1 s equal-power crossfade
function rng(seed) { let s = seed >>> 0; return () => ((s = (Math.imul(s, 1664525) + 1013904223) >>> 0) / 4294967296) * 2 - 1; }
function svf(fc, q, st, x, mode) { // Chamberlin state-variable filter; st = {lp, bp}
  const f = 2 * Math.sin(Math.PI * Math.min(fc, SR * 0.45) / SR);
  st.lp += f * st.bp;
  const hp = x - st.lp - q * st.bp;
  st.bp += f * hp;
  return mode === 'lp' ? st.lp : mode === 'bp' ? st.bp : hp;
}
const TAU = Math.PI * 2;
const per = (n, ph = 0) => (i) => Math.sin(TAU * (i / L) * n + ph); // exactly n cycles per loop: seamless
function render(fn) {
  const n = L + F, x = new Float32Array(n);
  // warm the filters over one extra loop worth of samples so the first samples are in steady state
  fn(x, n, true);
  const y = new Float32Array(L);
  for (let i = 0; i < L; i++) {
    if (i < F) {
      const t = i / F;
      y[i] = x[i] * Math.sin(t * Math.PI / 2) + x[L + i] * Math.cos(t * Math.PI / 2);
    } else y[i] = x[i];
  }
  let peak = 0; for (const v of y) peak = Math.max(peak, Math.abs(v));
  const g = 0.89 / peak; for (let i = 0; i < L; i++) y[i] *= g; // peak -1 dBFS
  return y;
}
const generators = {
  'gen_roar': { label: 'Generated roar: low rumble, steady', f: render((x, n) => {
    const r = rng(1), a = { lp: 0, bp: 0 }, b = { lp: 0, bp: 0 }, c = { lp: 0, bp: 0 }, s1 = per(2), s2 = per(5, 1), s3 = per(3, 2);
    for (let i = -SR * 2; i < n; i++) {
      const w = r();
      const cut = 160 + 50 * s1(Math.max(i, 0)) + 30 * s2(Math.max(i, 0));
      let v = svf(cut, 1.0, a, w, 'lp'); v = svf(cut * 1.6, 1.0, b, v, 'lp');
      const rumble = svf(60, 0.8, c, w, 'lp') * 2.2;
      const amp = 1 + 0.18 * s3(Math.max(i, 0)) + 0.08 * s2(Math.max(i, 0));
      if (i >= 0) x[i] = (v * 3.0 + rumble) * amp;
    }
  }) },
  'gen_howl': { label: 'Generated howl: mid band with slow pitch and level sweep', f: render((x, n) => {
    const r = rng(2), a = { lp: 0, bp: 0 }, b = { lp: 0, bp: 0 }, s1 = per(3), s2 = per(4, 0.5), s3 = per(7, 2), s4 = per(2, 1);
    for (let i = -SR * 2; i < n; i++) {
      const j = Math.max(i, 0), w = r();
      const fc = 520 + 220 * s1(j) + 120 * s2(j);
      const v = svf(fc, 0.22, a, w, 'bp') + 0.6 * svf(fc * 1.9 + 60 * s3(j), 0.3, b, r(), 'bp');
      const amp = 1 + 0.3 * s4(j) + 0.12 * s3(j);
      if (i >= 0) x[i] = v * amp;
    }
  }) },
  'gen_hiss': { label: 'Generated hiss: gritty high noise, blown sand', f: render((x, n) => {
    const r = rng(3), a = { lp: 0, bp: 0 }, g = { lp: 0, bp: 0 }, s1 = per(4), s2 = per(9, 1), s3 = per(6, 2);
    for (let i = -SR * 2; i < n; i++) {
      const j = Math.max(i, 0), w = r();
      const grit = 0.55 + 0.45 * svf(45, 0.9, g, r(), 'lp') * 6; // fast random level flutter = grit
      const v = svf(4300 + 700 * s1(j), 0.55, a, w, 'bp');
      const amp = (1 + 0.25 * s2(j) + 0.15 * s3(j)) * Math.max(0.2, grit);
      if (i >= 0) x[i] = v * amp;
    }
  }) },
};
async function encodeOgg(samples) {
  const e = new Encoder(SR, 1, 0.45, { TITLE: 'generated wind bed' });
  e.encodeFrom({ sampleRate: SR, numberOfChannels: 1, length: samples.length, getChannelData: () => samples });
  const blob = e.finish();
  return Buffer.from(await blob.arrayBuffer());
}
fs.mkdirSync(path.join(OUT, 'generated'), { recursive: true });
for (const [id, g] of Object.entries(generators)) {
  const ogg = await encodeOgg(g.f);
  fs.writeFileSync(path.join(OUT, 'generated', id + '.ogg'), ogg);
  candidates.push({ id: 'generated:' + id, group: 'Generated by tools/audition/node/build.mjs (no third-party licence)', label: g.label, event: null, licence: 'Ours: generated from noise, no licence problem', file: 'generated/' + id + '.ogg' });
}

// one-shot gusts: swelling band noise (not loops), three variants
function gust(seed, seconds, f0, f1) {
  const n = Math.round(seconds * SR), x = new Float32Array(n), r = rng(seed), a = { lp: 0, bp: 0 }, b = { lp: 0, bp: 0 };
  for (let i = 0; i < n; i++) {
    const t = i / n, swell = Math.pow(Math.sin(Math.PI * Math.pow(t, 0.7)), 2); // quick rise, slow fall
    const fc = f0 + (f1 - f0) * Math.sin(Math.PI * t);
    const w = r();
    x[i] = (svf(fc, 0.45, a, w, 'bp') * 2 + svf(fc * 0.5, 0.8, b, r(), 'lp') * 2.5) * swell;
  }
  let peak = 0; for (const v of x) peak = Math.max(peak, Math.abs(v));
  for (let i = 0; i < n; i++) x[i] *= 0.84 / peak;
  return x;
}
const gusts = { gen_gust1: gust(11, 3.2, 350, 900), gen_gust2: gust(12, 4.0, 250, 750), gen_gust3: gust(13, 5.0, 400, 1100) };
for (const [id, x] of Object.entries(gusts)) {
  fs.writeFileSync(path.join(OUT, 'generated', id + '.ogg'), await encodeOgg(x));
  candidates.push({ id: 'generated:' + id, group: 'Generated by tools/audition/node/build.mjs (no third-party licence)', label: 'Generated gust ' + id.slice(-1) + ': one-shot swell', event: null, licence: 'Ours: generated from noise, no licence problem', file: 'generated/' + id + '.ogg' });
}

// ---------- analysis ----------
function fft(re, im) { // in place radix-2
  const n = re.length;
  for (let i = 1, j = 0; i < n; i++) { let bit = n >> 1; for (; j & bit; bit >>= 1) j ^= bit; j ^= bit; if (i < j) { [re[i], re[j]] = [re[j], re[i]]; [im[i], im[j]] = [im[j], im[i]]; } }
  for (let len = 2; len <= n; len <<= 1) {
    const ang = -TAU / len, wr = Math.cos(ang), wi = Math.sin(ang);
    for (let i = 0; i < n; i += len) {
      let cr = 1, ci = 0;
      for (let k = 0; k < len / 2; k++) {
        const ur = re[i + k], ui = im[i + k], vr = re[i + k + len / 2] * cr - im[i + k + len / 2] * ci, vi = re[i + k + len / 2] * ci + im[i + k + len / 2] * cr;
        re[i + k] = ur + vr; im[i + k] = ui + vi; re[i + k + len / 2] = ur - vr; im[i + k + len / 2] = ui - vi;
        const t = cr * wr - ci * wi; ci = cr * wi + ci * wr; cr = t;
      }
    }
  }
}
const dB = (v) => 20 * Math.log10(Math.max(v, 1e-9));
const dec = new OggVorbisDecoder(); await dec.ready;
async function analyse(file) {
  await dec.reset();
  const r = await dec.decodeFile(new Uint8Array(fs.readFileSync(path.join(OUT, file))));
  const sr = r.sampleRate, nch = r.channelData.length, n = r.samplesDecoded;
  const x = new Float32Array(n);
  for (let c = 0; c < nch; c++) for (let i = 0; i < n; i++) x[i] += r.channelData[c][i] / nch;
  let peak = 0, sq = 0; for (let i = 0; i < n; i++) { peak = Math.max(peak, Math.abs(x[i])); sq += x[i] * x[i]; }
  const rms = Math.sqrt(sq / n);
  // spectral centroid
  const N = 4096, re = new Float64Array(N), im = new Float64Array(N);
  let num = 0, den = 0;
  for (let s = 0; s + N <= n; s += N * 2) {
    for (let i = 0; i < N; i++) { re[i] = x[s + i] * (0.5 - 0.5 * Math.cos(TAU * i / N)); im[i] = 0; }
    fft(re, im);
    for (let k = 1; k < N / 2; k++) { const p = re[k] * re[k] + im[k] * im[k]; num += p * k * sr / N; den += p; }
  }
  const centroid = den > 0 ? num / den : 0;
  // steadiness: std dev (dB) of 100 ms RMS windows
  const W = Math.round(sr * 0.1), lv = [];
  for (let s = 0; s + W <= n; s += W) { let q = 0; for (let i = 0; i < W; i++) q += x[s + i] * x[s + i]; lv.push(dB(Math.sqrt(q / W))); }
  const act = lv.filter((v) => v > -75), mean = act.reduce((a, b) => a + b, 0) / Math.max(1, act.length);
  const std = Math.sqrt(act.reduce((a, b) => a + (b - mean) ** 2, 0) / Math.max(1, act.length));
  // loop seam: level of the last and first 200 ms, and the sample step across the seam against the mean sample step
  const E = Math.min(Math.round(sr * 0.2), Math.floor(n / 4));
  let q0 = 0, q1 = 0; for (let i = 0; i < E; i++) { q0 += x[i] * x[i]; q1 += x[n - 1 - i] * x[n - 1 - i]; }
  const seamLevel = Math.abs(dB(Math.sqrt(q0 / E)) - dB(Math.sqrt(q1 / E)));
  let md = 0; for (let i = 1; i < n; i++) md += Math.abs(x[i] - x[i - 1]); md /= n - 1;
  const seamJump = Math.abs(x[0] - x[n - 1]) / Math.max(md, 1e-9);
  const dur = n / sr;
  return { duration: +dur.toFixed(2), sampleRate: sr, channels: nch, peakDb: +dB(peak).toFixed(1), rmsDb: +dB(rms).toFixed(1), centroidHz: Math.round(centroid), steadyStdDb: +std.toFixed(2), seamLevelDb: +seamLevel.toFixed(2), seamJump: +seamJump.toFixed(2) };
}
for (const c of candidates) Object.assign(c, await analyse(c.file));

// scores
const clamp = (v, a = 0, b = 1) => Math.max(a, Math.min(b, v));
for (const c of candidates) {
  const dur = clamp(c.duration / 6);
  const steady = clamp(1 - c.steadyStdDb / 5);
  const seam = (c.seamLevelDb < 2 && c.seamJump < 4) ? 1 : (c.seamLevelDb < 4 && c.seamJump < 8 ? 0.55 : 0.25);
  const low = clamp(1 - (c.centroidHz - 300) / 3500);
  const loud = clamp(1 - Math.abs(c.rmsDb + 18) / 24);
  c.bedScore = Math.round(100 * dur * steady * seam * (0.5 + 0.5 * low) * (0.5 + 0.5 * loud));
  const gdur = c.duration >= 0.6 && c.duration <= 8 ? 1 : c.duration < 0.6 ? c.duration / 0.6 : clamp(1 - (c.duration - 8) / 12);
  const vary = clamp(c.steadyStdDb / 6);
  const mid = clamp(1 - Math.abs(c.centroidHz - 1500) / 3500);
  c.gustScore = Math.round(100 * gdur * (0.4 + 0.6 * vary) * (0.4 + 0.6 * mid) * (0.5 + 0.5 * clamp(1 - Math.abs(c.rmsDb + 16) / 24)));
}
fs.writeFileSync(path.join(OUT, 'results.json'), JSON.stringify(candidates, null, 1));
const top = (k) => [...candidates].sort((a, b) => b[k] - a[k]).slice(0, 10).map((c) => `${c.bedScore}/${c.gustScore} ${c.label} dur=${c.duration} rms=${c.rmsDb} cen=${c.centroidHz} std=${c.steadyStdDb} seam=${c.seamLevelDb}/${c.seamJump}`);
console.log('TOP BEDS\n' + top('bedScore').join('\n') + '\nTOP GUSTS\n' + top('gustScore').join('\n'));
console.log(candidates.length + ' candidates');
