"""Build Lactea's packs and manifest.json, into the packs/ folder the app reads from GitHub.

Usage:  python -I tools/make_packs.py [output_dir] [--previews DIR]

Packs:
- essentials: the presets listed in packs-src/essentials.txt + Lactea's own presets (packs-src/Lactea);
- one pack per category of third_party/presets-cream-of-the-crop;
- textures: the MilkDrop texture pack, needed by most presets (every preset pack requires it).

Each zip holds an index.json, so the app never has to scan or read the presets itself:
{ "format": 1, "id": "dancer", "version": "2",
  "presets": [ { "file": "Dancer/x.milk", "image": true, "fps": 50, "tag": "chill",
                  "preview": true } ],                                       (preset packs)
  "textures": { "dir": "textures", "names": [ "clouds", … ] },              (textures pack)
  "reference": { "device": "…", "gpu": "…", "height": 720, "cap": 60, "date": "…" } }
- "image": the preset samples a random texture, so Lactea can show the album cover in it
  (same test as CoverPresets.kt);
- "fps": frame rate measured on the reference TV (docs/bench/presets-<device>.json), used until
  the TV has measured the preset itself.
- "tag": "chill", "ok" or "flash", from the flash analysis on a PC (docs/flash/flash.json, made by
  tools/flashscan and tools/flash_report.py export).
- "preview": the zip holds "Dancer/x.webp", 4 images of the preset in a 2 x 2 mosaic. Previews
  come from --previews (tools/flash_report.py previews); without it, those already in the packs
  of the output folder are kept, so a rebuild never loses them.

Zips are reproducible: a pack whose content did not change keeps its SHA-256 and its version.
A pack whose content changed gets the next version number, so the app offers the update.
"""
import argparse
import hashlib
import io
import json
import re
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CREAM = ROOT / "third_party" / "presets-cream-of-the-crop"
TEXTURES = ROOT / "third_party" / "presets-milkdrop-texture-pack" / "textures"
SRC = ROOT / "packs-src"
BENCH = ROOT / "docs" / "bench" / "presets-BRAVIA_4K_AE2.json"
FLASH = ROOT / "docs" / "flash" / "flash.json"
REFERENCE_GPU = "Mali-G57"
REFERENCE_CAP = 60
SKIP = {"! Transition"}
TEXTURE_EXT = {".jpg", ".jpeg", ".png", ".bmp", ".tga", ".dds"}

# Same pattern as CoverPresets.RANDOM_TEXTURE in the app.
RANDOM_TEXTURE = re.compile(r"\b(sampler|texsize)_((?:[fp][wc]_)?)(rand\d\d(?:_[A-Za-z0-9]+)?)\b", re.I)
VALID_TEXTURE_NAME = re.compile(r"[A-Za-z0-9_]+")

DESCRIPTIONS = {
    "essentials": "The best of the collection and Lactea's own presets",
    "textures": "Images used by the presets",
    "dancer": "Shapes dancing to the beat",
    "drawing": "Animated lines and drawings",
    "fractal": "Fractals and endless zooms",
    "geometric": "Geometric shapes",
    "hypnotic": "Hypnotic patterns",
    "particles": "Particles and swarms",
    "reaction": "Chemical reactions and automata",
    "sparkle": "Sparks and explosions",
    "supernova": "Cosmic explosions",
    "waveform": "Waveforms and spectrums",
}


def pack_id(name: str) -> str:
    return re.sub(r"[^a-z0-9]+", "-", name.lower()).strip("-")


def zip_bytes(files: dict[str, bytes]) -> bytes:
    """Reproducible zip: sorted entries, fixed dates."""
    buf = io.BytesIO()
    with zipfile.ZipFile(buf, "w", zipfile.ZIP_DEFLATED, compresslevel=9) as z:
        for name in sorted(files):
            info = zipfile.ZipInfo(name, date_time=(1980, 1, 1, 0, 0, 0))
            info.compress_type = zipfile.ZIP_DEFLATED
            info.external_attr = 0o644 << 16
            z.writestr(info, files[name], compresslevel=9)
    return buf.getvalue()


def preset_entry(file: str, data: bytes, fps: dict, flash: dict, previews: dict) -> dict:
    e = {"file": file}
    if RANDOM_TEXTURE.search(data.decode("latin-1")):
        e["image"] = True
    key = file[:-len(".milk")]
    if key in fps:
        e["fps"] = round(fps[key])
    if key in flash:
        f = flash[key]
        e["tag"] = "flash" if f["flash"] else "chill" if f["chill"] else "ok"
    if key in previews:
        e["preview"] = True
    return e


def load_previews(out: Path, previews_dir: Path | None) -> dict[str, bytes]:
    """key -> WebP mosaic, from previews_dir, or else from the packs already in out."""
    found: dict[str, bytes] = {}
    if previews_dir is not None:
        for f in previews_dir.rglob("*.webp"):
            found[f.relative_to(previews_dir).as_posix()[:-len(".webp")]] = f.read_bytes()
        return found
    for z in out.glob("lactea-pack-*.zip"):
        with zipfile.ZipFile(z) as zf:
            for name in zf.namelist():
                if name.endswith(".webp") and not name.startswith("textures/"):
                    found[name[:-len(".webp")]] = zf.read(name)
    return found


def build(out: Path, previews_dir: Path | None = None) -> None:
    out.mkdir(parents=True, exist_ok=True)
    bench = json.loads(BENCH.read_text(encoding="utf-8"))
    fps = bench["fps"]
    flash = json.loads(FLASH.read_text(encoding="utf-8"))["presets"] if FLASH.exists() else {}
    previews = load_previews(out, previews_dir)
    print(f"flash analysis: {len(flash)} presets, previews: {len(previews)}")
    reference = {"device": bench["device"], "gpu": REFERENCE_GPU, "height": bench["height"],
                 "cap": REFERENCE_CAP, "date": bench["date"]}
    old = {}
    if (out / "manifest.json").exists():
        old = {p["id"]: p for p in json.loads((out / "manifest.json").read_text(encoding="utf-8"))["packs"]}

    # pack id -> (display name, {zip path: bytes})
    sources: dict[str, tuple[str, dict[str, bytes]]] = {}

    essentials = {}
    for line in (SRC / "essentials.txt").read_text(encoding="utf-8").splitlines():
        key = line.strip()
        if key and not key.startswith("#"):
            essentials[f"{key}.milk"] = (CREAM / f"{key}.milk").read_bytes()
    for f in sorted((SRC / "Lactea").glob("*.milk")):
        essentials[f"Lactea/{f.name}"] = f.read_bytes()
    sources["essentials"] = ("Essentials", essentials)

    for cat in sorted(p for p in CREAM.iterdir() if p.is_dir() and not p.name.startswith(".")):
        if cat.name in SKIP:
            continue
        files = {f.relative_to(CREAM).as_posix(): f.read_bytes() for f in sorted(cat.rglob("*.milk"))}
        if files:
            sources[pack_id(cat.name)] = (cat.name, files)

    textures = {f"textures/{f.name}": f.read_bytes()
                for f in sorted(TEXTURES.iterdir()) if f.suffix.lower() in TEXTURE_EXT}
    sources["textures"] = ("Textures", textures)

    packs = []
    for pid, (name, files) in sources.items():
        is_textures = pid == "textures"
        index: dict = {"format": 1, "id": pid}
        if is_textures:
            names = sorted({Path(n).stem for n in files if VALID_TEXTURE_NAME.fullmatch(Path(n).stem)}, key=str.lower)
            index["textures"] = {"dir": "textures", "names": names}
        else:
            index["presets"] = [preset_entry(n, d, fps, flash, previews) for n, d in sorted(files.items(), key=lambda x: x[0].lower())]
            for e in index["presets"]:
                if e.get("preview"):
                    files[e["file"][:-len(".milk")] + ".webp"] = previews[e["file"][:-len(".milk")]]
            index["reference"] = reference

        # Version: unchanged content keeps its version, changed content gets the next one.
        content = zip_bytes({**files, "index.json": json.dumps(index, ensure_ascii=False).encode()})
        sha = hashlib.sha256(content).hexdigest()
        prev = old.get(pid)
        if prev and prev.get("sha256_content") == sha:
            version = prev["version"]
        elif prev:
            version = str(int(prev["version"]) + 1)
        else:
            version = "1"
        index["version"] = version
        data = zip_bytes({**files, "index.json": json.dumps(index, ensure_ascii=False, separators=(",", ":")).encode()})
        (out / f"lactea-pack-{pid}.zip").write_bytes(data)

        entry = {
            "id": pid,
            "name": name,
            "kind": "textures" if is_textures else "presets",
            "version": version,
            "url": f"lactea-pack-{pid}.zip",
            "size": len(data),
            "presets": 0 if is_textures else sum(n.endswith(".milk") for n in files),
            "sha256": hashlib.sha256(data).hexdigest(),
            "sha256_content": sha,
            "description": DESCRIPTIONS.get(pid, ""),
        }
        if pid == "essentials":
            entry["recommended"] = True
        if not is_textures:
            entry["requires"] = ["textures"]
        packs.append(entry)
        n = len(files) if is_textures else sum(n.endswith(".milk") for n in files)
        print(f"{pid:11s} v{version:3s} {n:5d} {'files' if is_textures else 'presets'}  {len(data) / 1e6:5.1f} MB")

    manifest = {"version": 2, "source": "projectM-visualizer/presets-cream-of-the-crop", "packs": packs}
    (out / "manifest.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    for stale in out.glob("lactea-pack-*.zip"):
        if stale.name not in {p["url"] for p in packs}:
            stale.unlink()
    print(f"manifest.json: {len(packs)} packs, {sum(p['presets'] for p in packs)} presets")


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("output", nargs="?", default=str(ROOT / "packs"))
    ap.add_argument("--previews", help="folder of <key>.webp previews (tools/flash_report.py previews)")
    a = ap.parse_args()
    build(Path(a.output), Path(a.previews) if a.previews else None)
