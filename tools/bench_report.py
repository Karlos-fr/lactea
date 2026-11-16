"""Summarise docs/bench/bench-<res>p.log files produced by tools/bench.sh.

For each resolution: per-preset median FPS (ignoring the first 3 seconds after a
preset change, which include the blend), overall stats, and the slowest presets.
Writes docs/bench/REPORT.md.
"""
import re
import statistics
import sys
from pathlib import Path

BENCH = Path(__file__).resolve().parent.parent / "docs" / "bench"
PRESET_RE = re.compile(r"PRESET \d+/\d+ (.+)$")
FPS_RE = re.compile(r"FPS fps=(\d+) .*res=(\d+)x(\d+)")
FAIL_RE = re.compile(r"Preset failed: (.+)$")
SKIP_SAMPLES = 3


def parse(path: Path):
    per_preset: dict[str, list[int]] = {}
    failures: list[str] = []
    surface = None
    current, seen = None, 0
    for line in path.read_text(encoding="utf-8", errors="replace").splitlines():
        if m := PRESET_RE.search(line):
            current, seen = m.group(1).strip(), 0
            per_preset.setdefault(current, [])
        elif (m := FPS_RE.search(line)) and current:
            surface = f"{m.group(2)}x{m.group(3)}"
            seen += 1
            if seen > SKIP_SAMPLES:
                per_preset[current].append(int(m.group(1)))
        elif m := FAIL_RE.search(line):
            failures.append(m.group(1).strip())
    medians = {p: statistics.median(v) for p, v in per_preset.items() if v}
    return surface, medians, failures


def main() -> int:
    logs = sorted(BENCH.glob("bench-*p.log"), key=lambda p: int(re.search(r"(\d+)p", p.name).group(1)))
    if not logs:
        print("no logs", file=sys.stderr)
        return 1
    out = ["# Banc d'essai Lactea — résultats", ""]
    out.append("| Résolution | Surface réelle | Presets mesurés | FPS médian | ≥ 55 FPS | 30–54 FPS | < 30 FPS | Échecs |")
    out.append("|---|---|---|---|---|---|---|---|")
    details = []
    for log in logs:
        res = log.stem.replace("bench-", "")
        surface, med, fails = parse(log)
        vals = list(med.values())
        if not vals:
            continue
        good = sum(v >= 55 for v in vals)
        mid = sum(30 <= v < 55 for v in vals)
        bad = sum(v < 30 for v in vals)
        out.append(f"| {res} | {surface} | {len(vals)} | {statistics.median(vals):.0f} | {good} | {mid} | {bad} | {len(set(fails))} |")
        slow = sorted(med.items(), key=lambda kv: kv[1])[:8]
        details.append(f"\n## {res} — presets les plus lents\n")
        details += [f"- {fps:.0f} FPS — {name}" for name, fps in slow]
        if fails:
            details.append(f"\n**Échecs de chargement ({len(set(fails))})** :")
            details += [f"- {f[:160]}" for f in sorted(set(fails))]
    report = "\n".join(out + details) + "\n"
    (BENCH / "REPORT.md").write_text(report, encoding="utf-8")
    print(report)
    return 0


if __name__ == "__main__":
    sys.exit(main())
