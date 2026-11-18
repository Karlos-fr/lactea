"""Flash analysis of the presets, from the frames recorded by tools/flashscan.

Usage:
  python -I tools/flash_report.py all <list.txt>                    every preset of the collection
  python -I tools/flash_report.py sample <n> <list.txt>            presets to analyse (stratified)
  python -I tools/flash_report.py analyse <flashscan out dir>       -> <dir>/report.json
  python -I tools/flash_report.py pick <report.json> <n> <list.txt> presets for calibration clips
  python -I tools/flash_report.py montage <clips dir> <list.txt> <out.mp4>
  python -I tools/flash_report.py previews <flashscan out dir> <previews dir>   2x2 mosaics, WebP
  python -I tools/flash_report.py export <out.json> <flashscan out dir>...      results for the packs

Measures per preset, after the first WARMUP_S seconds (the preset settling):
- general flashes (ITU-R BT.1702 / WCAG 2.x): a transition is a change of relative luminance of
  at least 10 % of white where the darker state is under 0.80; it counts for the screen when
  cells covering at least AREA of it make a transition in the same direction within two frames.
  A flash is a pair of opposing transitions. "flash" = more than 3 flashes (6 transitions) in a
  window of one second.
- red flashes: same on the "saturated red" value ((R - G - B) x 320, change >= 20) of cells
  where R / (R + G + B) >= 0.8.
- agitation: mean change of luminance per frame, over the cells (how much the image moves).
- luminance: mean relative luminance.
"chill" = no flash and agitation under CHILL_AGITATION (calibrated on two sets of clips judged by
eye: every "chill" preset under it, every "OK" one well above).
"""
import json
import random
import subprocess
import sys
from pathlib import Path

import numpy as np

ROOT = Path(__file__).resolve().parent.parent
CREAM = ROOT / "third_party" / "presets-cream-of-the-crop"
GRID_W, GRID_H = 16, 9
FPS = 30
WARMUP_S = 2.0
AREA = 0.25
LUM_STEP, LUM_DARK = 0.10, 0.80
RED_STEP, RED_RATIO = 20 / 320, 0.8
MAX_TRANSITIONS_PER_S = 6
CHILL_AGITATION = 0.005
THUMB_W, THUMB_H, THUMBS = 160, 90, 4


def all_presets() -> list[str]:
    return sorted(p.relative_to(CREAM).as_posix() for p in CREAM.rglob("*.milk")
                  if not p.relative_to(CREAM).parts[0].startswith("!"))


def write_all(out: Path) -> None:
    files = all_presets()
    out.write_text("\n".join(files) + "\n", encoding="utf-8")
    print(f"{out}: {len(files)} presets")


def sample(n: int, out: Path) -> None:
    """n presets of the collection, proportional to each category, always the same."""
    files = sorted(p.relative_to(CREAM).as_posix() for p in CREAM.rglob("*.milk") if not p.parts[-2].startswith("!"))
    files = [f for f in files if not f.startswith("! ")]
    by_cat: dict[str, list[str]] = {}
    for f in files:
        by_cat.setdefault(f.split("/")[0], []).append(f)
    rng = random.Random(42)
    picked = []
    for cat, items in sorted(by_cat.items()):
        k = max(1, round(n * len(items) / len(files)))
        picked += rng.sample(items, min(k, len(items)))
    out.write_text("\n".join(sorted(picked)) + "\n", encoding="utf-8")
    print(f"{out}: {len(picked)} presets from {len(by_cat)} categories")


def transitions(values: np.ndarray, step: float, ok) -> tuple[np.ndarray, np.ndarray]:
    """Per frame and cell: +1 rising / -1 falling transition, 0 none. values: frames x cells.
    ok(anchor, now): extra condition on the two states (e.g. the darker one under 0.8)."""
    frames, cells = values.shape
    out = np.zeros((frames, cells), np.int8)
    hi = values[0].copy()
    lo = values[0].copy()
    direction = np.zeros(cells, np.int8)
    for f in range(1, frames):
        x = values[f]
        hi = np.where(direction >= 0, np.maximum(hi, x), hi)
        lo = np.where(direction <= 0, np.minimum(lo, x), lo)
        down = (direction >= 0) & (hi - x >= step) & ok(hi, x)
        up = (direction <= 0) & ~down & (x - lo >= step) & ok(lo, x)
        out[f][up] = 1
        out[f][down] = -1
        direction = np.where(up, 1, np.where(down, -1, direction)).astype(np.int8)
        hi = np.where(up | down, x, hi)
        lo = np.where(up | down, x, lo)
    return out


def screen_transitions(cell_tr: np.ndarray) -> np.ndarray:
    """Frames where cells covering AREA of the screen change in the same direction (2 frames)."""
    frames, cells = cell_tr.shape
    events = np.zeros(frames, bool)
    last = -10
    for f in range(1, frames):
        window = cell_tr[f - 1:f + 1]
        for d in (1, -1):
            area = np.any(window == d, axis=0).mean()
            if area >= AREA and f - last >= 2:
                events[f] = True
                last = f
                break
    return events


def max_per_second(events: np.ndarray) -> tuple[int, int]:
    """Most events in one second, and how many one-second windows exceed the limit."""
    c = np.concatenate([[0], np.cumsum(events)])
    per_s = c[FPS:] - c[:-FPS] if len(events) >= FPS else np.array([events.sum()])
    return int(per_s.max()), int((per_s > MAX_TRANSITIONS_PER_S).sum())


def analyse_one(data: np.ndarray) -> dict:
    frames = data.shape[0]
    d = data[int(WARMUP_S * FPS):].reshape(-1, GRID_W * GRID_H, 3).astype(np.float32) / 255
    lum, ratio, red = d[..., 0], d[..., 1], d[..., 2]
    lum_tr = transitions(lum, LUM_STEP, lambda a, b: np.minimum(a, b) < LUM_DARK)
    red_tr = transitions(red * (ratio >= RED_RATIO), RED_STEP, lambda a, b: np.ones_like(a, bool))
    lum_ev = screen_transitions(lum_tr)
    red_ev = screen_transitions(red_tr)
    lum_max, lum_over = max_per_second(lum_ev)
    red_max, red_over = max_per_second(red_ev)
    agitation = float(np.abs(np.diff(lum, axis=0)).mean())
    return {
        "frames": frames,
        "transitions_max_1s": lum_max,
        "seconds_over": lum_over,
        "red_transitions_max_1s": red_max,
        "red_seconds_over": red_over,
        "agitation": round(agitation, 5),
        "luminance": round(float(lum.mean()), 4),
        "luminance_pulse": round(float(lum.mean(axis=1).std()), 4),
        "flash": (flash := lum_max > MAX_TRANSITIONS_PER_S or red_max > MAX_TRANSITIONS_PER_S),
        "chill": not flash and agitation < CHILL_AGITATION,
    }


def analyse(out_dir: Path) -> None:
    report = {}
    for line in (out_dir / "index.jsonl").read_text(encoding="utf-8").splitlines():
        e = json.loads(line)
        if e["status"] != "ok":
            report[e["key"]] = {"status": e["status"], "error": e.get("error", "")}
            continue
        raw = np.fromfile(out_dir / "frames" / e["file"], np.uint8)
        data = raw.reshape(e["frames"], GRID_H, GRID_W, 3)
        report[e["key"]] = {"status": "ok", "ms": e["ms"], **analyse_one(data)}
    (out_dir / "report.json").write_text(json.dumps(report, indent=1, ensure_ascii=False), encoding="utf-8")
    ok = [r for r in report.values() if r["status"] == "ok"]
    flash = sum(r["flash"] for r in ok)
    ag = np.array([r["agitation"] for r in ok])
    print(f"{len(report)} presets, {len(ok)} rendered, {flash} flash ({100 * flash / max(1, len(ok)):.0f} %)")
    print(f"agitation: median {np.median(ag):.4f}, p25 {np.percentile(ag, 25):.4f}, p75 {np.percentile(ag, 75):.4f}")
    print(f"render time: mean {np.mean([r['ms'] for r in ok]) / 1000:.2f} s per preset")


def previews(out_dir: Path, dest: Path) -> None:
    """One WebP per preset: its 4 images in a 2 x 2 mosaic, at <dest>/<key>.webp."""
    from PIL import Image
    n = 0
    for line in (out_dir / "index.jsonl").read_text(encoding="utf-8").splitlines():
        e = json.loads(line)
        src = out_dir / "thumbs" / e["file"].replace(".bin", ".rgb")
        if e["status"] != "ok" or not src.exists():
            continue
        raw = np.fromfile(src, np.uint8).reshape(THUMBS, THUMB_H, THUMB_W, 3)
        mosaic = np.concatenate([np.concatenate(raw[0:2], axis=1), np.concatenate(raw[2:4], axis=1)], axis=0)
        target = dest / f"{e['key']}.webp"
        target.parent.mkdir(parents=True, exist_ok=True)
        Image.fromarray(mosaic).save(target, "WEBP", quality=72, method=6)
        n += 1
    print(f"{dest}: {n} previews")


def export(out_json: Path, out_dirs: list[Path]) -> None:
    """Compact results for tools/make_packs.py: key -> flash, chill and the measures."""
    result = {}
    for d in out_dirs:
        for k, v in json.loads((d / "report.json").read_text(encoding="utf-8")).items():
            if v["status"] != "ok":
                continue
            result[k] = {"flash": v["flash"], "chill": v["chill"], "transitions": v["transitions_max_1s"],
                         "red": v["red_transitions_max_1s"], "agitation": v["agitation"], "luminance": v["luminance"]}
    out_json.parent.mkdir(parents=True, exist_ok=True)
    out_json.write_text(json.dumps({"method": "tools/flashscan (ITU-R BT.1702 flashes, 16x9 grid, 30 fps, "
                                              "synthetic 120 BPM music)",
                                    "chill_agitation": CHILL_AGITATION, "presets": dict(sorted(result.items()))},
                                   ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    flash = sum(v["flash"] for v in result.values())
    chill = sum(v["chill"] for v in result.values())
    print(f"{out_json}: {len(result)} presets, {flash} flash, {chill} chill")


def pick(report_path: Path, n: int, out: Path) -> None:
    """Clips to calibrate the thresholds: around the flash limit, clearly flashing, calmest,
    and the middle of the agitation range."""
    rep = {k: v for k, v in json.loads(report_path.read_text(encoding="utf-8")).items() if v["status"] == "ok"}
    keys = sorted(rep)
    rng = random.Random(7)
    by_flash = sorted(keys, key=lambda k: rep[k]["transitions_max_1s"])
    by_ag = sorted(keys, key=lambda k: rep[k]["agitation"])
    near = [k for k in keys if 4 <= rep[k]["transitions_max_1s"] <= 9]
    groups = [
        rng.sample(near, min(len(near), n * 3 // 10)),
        by_flash[-(n // 5):],
        by_ag[: n // 4],
        by_ag[len(by_ag) // 2 - n // 10: len(by_ag) // 2 + n // 10],
    ]
    chosen = []
    for g in groups:
        for k in g:
            if k not in chosen:
                chosen.append(k)
    rng.shuffle(chosen)  # no hint from the order
    chosen = chosen[:n]
    out.write_text("\n".join(f"{k}.milk" for k in chosen) + "\n", encoding="utf-8")
    print(f"{out}: {len(chosen)} presets")


def montage(clips: Path, list_path: Path, out: Path, seconds: float = 6.0) -> None:
    """One video with every calibration clip, numbered, from WARMUP_S on."""
    names = [l for l in list_path.read_text(encoding="utf-8").splitlines() if l.strip()]
    parts = []
    font = "C\\:/Windows/Fonts/arialbd.ttf"
    for i, _ in enumerate(names):
        src = clips / f"{i}.mp4"
        part = clips / f"part{i}.mp4"
        label = f"{i + 1} / {len(names)}"
        subprocess.run(["ffmpeg", "-v", "error", "-y", "-ss", str(WARMUP_S), "-t", str(seconds), "-i", str(src),
                        "-vf", f"scale=960:540,drawtext=fontfile='{font}':text='{label}':x=24:y=20:fontsize=44:"
                               "fontcolor=white:box=1:boxcolor=black@0.6:boxborderw=12,fps=30",
                        "-c:v", "libx264", "-preset", "veryfast", "-crf", "26", "-c:a", "aac", "-b:a", "128k",
                        "-ar", "44100", "-ac", "2", str(part)], check=True)
        parts.append(part)
    lst = clips / "montage.txt"
    lst.write_text("".join(f"file '{p.as_posix()}'\n" for p in parts), encoding="utf-8")
    subprocess.run(["ffmpeg", "-v", "error", "-y", "-f", "concat", "-safe", "0", "-i", str(lst), "-c", "copy",
                    "-movflags", "+faststart", str(out)], check=True)
    print(f"{out}: {len(parts)} clips of {seconds:.0f} s")


if __name__ == "__main__":
    cmd = sys.argv[1]
    if cmd == "all":
        write_all(Path(sys.argv[2]))
    elif cmd == "previews":
        previews(Path(sys.argv[2]), Path(sys.argv[3]))
    elif cmd == "export":
        export(Path(sys.argv[2]), [Path(a) for a in sys.argv[3:]])
    elif cmd == "sample":
        sample(int(sys.argv[2]), Path(sys.argv[3]))
    elif cmd == "analyse":
        analyse(Path(sys.argv[2]))
    elif cmd == "pick":
        pick(Path(sys.argv[2]), int(sys.argv[3]), Path(sys.argv[4]))
    elif cmd == "montage":
        montage(Path(sys.argv[2]), Path(sys.argv[3]), Path(sys.argv[4]))
    else:
        sys.exit(__doc__)
