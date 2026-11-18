"""Synthetic test music for the flash analysis (tools/flashscan): same for every preset, free of
rights, reproducible. Deliberately punchy (presets flash on the beat): the worst case.

120 BPM, 4/4: kick on every beat, clap on 2 and 4, closed hi-hats on eighths, a bass line on
eighths and a sustained minor chord for the mids. 14 s, 44.1 kHz stereo, peak -1 dBFS.

Usage:  python -I tools/flash_audio.py <output dir>
Writes flash-test.wav (for the calibration clips) and flash-test.f32 (raw float32 stereo, for
flashscan).
"""
import sys
import wave
from pathlib import Path

import numpy as np

SR = 44100
SECONDS = 14.0
BPM = 120
BEAT = 60 / BPM


def env(n, attack, decay):
    t = np.arange(n) / SR
    return np.minimum(t / attack, 1.0) * np.exp(-t / decay)


def place(track, sound, at):
    i = int(at * SR)
    j = min(len(track), i + len(sound))
    if i < len(track):
        track[i:j] += sound[: j - i]


def main(out: Path) -> None:
    rng = np.random.default_rng(1)
    n = int(SECONDS * SR)
    mix = np.zeros(n)

    # Kick: sine sweeping 120 -> 45 Hz.
    kn = int(0.35 * SR)
    t = np.arange(kn) / SR
    freq = 45 + 75 * np.exp(-t / 0.04)
    kick = np.sin(2 * np.pi * np.cumsum(freq) / SR) * env(kn, 0.002, 0.12)
    # Clap: band-limited noise burst.
    cn = int(0.2 * SR)
    clap = np.convolve(rng.standard_normal(cn), np.ones(8) / 8, "same") * env(cn, 0.001, 0.05) * 0.6
    # Closed hi-hat: high-passed noise.
    hn = int(0.05 * SR)
    noise = rng.standard_normal(hn)
    hat = (noise - np.convolve(noise, np.ones(4) / 4, "same")) * env(hn, 0.0005, 0.012) * 0.35

    bass_notes = [55.0, 55.0, 65.41, 49.0]  # A1 A1 C2 G1, one per bar
    beats = int(SECONDS / BEAT)
    for b in range(beats):
        at = b * BEAT
        place(mix, kick, at)
        if b % 4 in (1, 3):
            place(mix, clap, at)
        for half in (0, 0.5):
            place(mix, hat, at + half * BEAT)
            bn = int(0.22 * SR)
            tb = np.arange(bn) / SR
            f = bass_notes[(b // 4) % len(bass_notes)]
            saw = 2 * ((tb * f) % 1.0) - 1
            place(mix, saw * env(bn, 0.005, 0.12) * 0.35, at + half * BEAT)

    # Pad: A minor chord, slow tremolo, for the mid frequencies.
    tt = np.arange(n) / SR
    pad = sum(np.sin(2 * np.pi * f * tt) for f in (220.0, 261.63, 329.63)) / 3
    mix += pad * 0.12 * (0.75 + 0.25 * np.sin(2 * np.pi * 0.25 * tt))

    mix *= 10 ** (-1 / 20) / np.max(np.abs(mix))
    stereo = np.stack([mix, mix], axis=1).astype(np.float32)

    out.mkdir(parents=True, exist_ok=True)
    stereo.tofile(out / "flash-test.f32")
    with wave.open(str(out / "flash-test.wav"), "wb") as w:
        w.setnchannels(2)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes((stereo * 32767).astype("<i2").tobytes())
    print(f"{out / 'flash-test.wav'}: {SECONDS:.0f} s, {BPM} BPM")


if __name__ == "__main__":
    main(Path(sys.argv[1]) if len(sys.argv) > 1 else Path("."))
