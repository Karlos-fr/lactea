"""Build Lactea preset packs (one zip per category) and manifest.json.

Usage:  python -I tools/make_packs.py [output_dir] [--version N]

Source: third_party/presets-cream-of-the-crop (one top-level folder per category).
Output: <output_dir>/lactea-pack-<id>.zip + manifest.json, ready to attach to a GitHub
release (the app reads releases/latest/download/manifest.json) or to serve over HTTP.
"""
import argparse
import hashlib
import json
import re
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SOURCE = ROOT / "third_party" / "presets-cream-of-the-crop"
SKIP = {"! Transition"}

DESCRIPTIONS = {
    "Dancer": "Formes qui dansent sur le rythme",
    "Drawing": "Tracés et dessins animés",
    "Fractal": "Fractales et zooms infinis",
    "Geometric": "Formes géométriques",
    "Hypnotic": "Motifs hypnotiques",
    "Particles": "Particules et nuées",
    "Reaction": "Réactions chimiques et automates",
    "Sparkle": "Étincelles et explosions",
    "Supernova": "Explosions cosmiques",
    "Waveform": "Formes d'onde et spectres",
}


def pack_id(name: str) -> str:
    return re.sub(r"[^a-z0-9]+", "-", name.lower()).strip("-")


def build(out: Path, version: str) -> None:
    out.mkdir(parents=True, exist_ok=True)
    packs = []
    for cat in sorted(p for p in SOURCE.iterdir() if p.is_dir() and not p.name.startswith(".")):
        if cat.name in SKIP:
            continue
        files = sorted(cat.rglob("*.milk"))
        if not files:
            continue
        pid = pack_id(cat.name)
        zpath = out / f"lactea-pack-{pid}.zip"
        with zipfile.ZipFile(zpath, "w", zipfile.ZIP_DEFLATED, compresslevel=9) as z:
            for f in files:
                z.write(f, f.relative_to(SOURCE).as_posix())
        data = zpath.read_bytes()
        packs.append({
            "id": pid,
            "name": cat.name,
            "version": version,
            "url": zpath.name,
            "size": len(data),
            "presets": len(files),
            "sha256": hashlib.sha256(data).hexdigest(),
            "description": DESCRIPTIONS.get(cat.name, ""),
        })
        print(f"{pid:10s} {len(files):5d} presets  {len(data) / 1e6:6.1f} MB")
    manifest = {"version": 1, "source": "projectM-visualizer/presets-cream-of-the-crop", "packs": packs}
    (out / "manifest.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"manifest.json: {len(packs)} packs, {sum(p['presets'] for p in packs)} presets")


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("output", nargs="?", default=str(ROOT / "dist" / "packs"))
    ap.add_argument("--version", default="1")
    a = ap.parse_args()
    build(Path(a.output), a.version)
