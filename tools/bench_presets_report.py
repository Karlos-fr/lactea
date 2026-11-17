"""Report for Lactea's in-app preset benchmark (files/bench/bench.tsv on the TV).

Usage:
    python -I tools/bench_presets_report.py <bench.tsv> [device-name]

Writes, in docs/bench/:
    presets-<device>.md    readable report (French)
    presets-<device>.json  per-preset FPS + failures, reusable by the catalog (step 8)
"""
import json
import statistics
import sys
from collections import defaultdict
from datetime import date
from pathlib import Path

OUT = Path(__file__).resolve().parent.parent / "docs" / "bench"
BUCKETS = [
    ("Fluide (≥ 55)", 55, 999),
    ("Correct (45–55)", 45, 55),
    ("Lourd (30–45)", 30, 45),
    ("Très lourd (20–30)", 20, 30),
    ("Trop lourd (< 20)", 0, 20),
]


def bucket(fps: float) -> str:
    for name, lo, hi in BUCKETS:
        if lo <= fps < hi:
            return name
    return BUCKETS[-1][0]


def main() -> int:
    src = Path(sys.argv[1])
    device = sys.argv[2] if len(sys.argv) > 2 else "BRAVIA_4K_AE2"
    rows = []
    for line in src.read_text(encoding="utf-8", errors="replace").splitlines():
        parts = line.split("\t")
        if len(parts) < 5:
            continue
        key, status, fps, max_ms, height = parts[:5]
        msg = parts[5] if len(parts) > 5 else ""
        rows.append((key, status, float(fps), float(max_ms or 0), int(height or 0), msg))
    ok = [r for r in rows if r[1] == "OK"]
    failed = [r for r in rows if r[1] != "OK"]
    fps_all = [r[2] for r in ok]
    n = len(rows)

    by_bucket = defaultdict(int)
    for r in ok:
        by_bucket[bucket(r[2])] += 1

    by_cat = defaultdict(list)
    fail_cat = defaultdict(int)
    for r in ok:
        by_cat[r[0].split("/")[0]].append(r[2])
    for r in failed:
        fail_cat[r[0].split("/")[0]] += 1

    md = [f"# Banc d'essai des presets — {device}", "",
          f"Mesuré le {date.today():%d/%m/%Y} en 1280×720, plafond 60 images/s, 2 s de mesure par preset "
          f"après 1 s de stabilisation, avec de la musique (Spotify).", "",
          f"**{n} presets testés** — {len(ok)} chargés, **{len(failed)} échecs**"
          + (f" ({sum(r[5] == 'compat' for r in ok)} chargés grâce à la lecture « à la MilkDrop » des équations)"
             if any(r[5] == 'compat' for r in ok) else "")
          + (f", FPS médian **{statistics.median(fps_all):.0f}**" if fps_all else "") + ".", "",
          "## Répartition", "", "| Catégorie de FPS | Presets | Part |", "|---|---|---|"]
    for name, _, _ in BUCKETS:
        c = by_bucket[name]
        md.append(f"| {name} | {c} | {100 * c / max(n, 1):.1f} % |")
    md.append(f"| Échec de chargement | {len(failed)} | {100 * len(failed) / max(n, 1):.1f} % |")

    md += ["", "## Par catégorie de presets", "",
           "| Catégorie | Testés | FPS médian | Fluides (≥ 55) | < 30 FPS | Échecs |", "|---|---|---|---|---|---|"]
    for cat in sorted(set(by_cat) | set(fail_cat)):
        v = by_cat.get(cat, [])
        tot = len(v) + fail_cat[cat]
        med = f"{statistics.median(v):.0f}" if v else "—"
        md.append(f"| {cat} | {tot} | {med} | {sum(f >= 55 for f in v)} | {sum(f < 30 for f in v)} | {fail_cat[cat]} |")

    md += ["", "## Les 25 plus gourmands", ""]
    md += [f"- {r[2]:.0f} FPS — {r[0]}" for r in sorted(ok, key=lambda r: r[2])[:25]]
    if failed:
        md += ["", f"## Échecs ({len(failed)})", "",
               "Équations que même MilkDrop n'acceptait pas (fonction sans parenthèses, syntaxe invalide).", ""]
        md += [f"- {r[0]}" for r in failed[:100]]
        if len(failed) > 100:
            md.append(f"- … et {len(failed) - 100} autres (voir le JSON)")

    OUT.mkdir(parents=True, exist_ok=True)
    (OUT / f"presets-{device}.md").write_text("\n".join(md) + "\n", encoding="utf-8")
    (OUT / f"presets-{device}.json").write_text(json.dumps({
        "device": device, "height": 720, "date": f"{date.today():%Y-%m-%d}",
        "fps": {r[0]: round(r[2], 1) for r in ok},
        "failed": [r[0] for r in failed],
    }, ensure_ascii=False, indent=0), encoding="utf-8")
    sys.stdout.reconfigure(encoding="utf-8")
    print("\n".join(md[:30]))
    return 0


if __name__ == "__main__":
    sys.exit(main())
