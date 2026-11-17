"""Turn a preset benchmark into the reference FPS table shipped inside the app.

Usage:  python -I tools/make_reference.py [docs/bench/presets-<device>.json]

Writes app/src/main/assets/bench/reference.json. The app uses these values for presets it has
not measured itself yet (new installs), so "exclude slow presets", Auto resolution and smart
transitions work from the first launch. Its own measurements replace them as presets play.
"""
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
src = Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT / "docs" / "bench" / "presets-BRAVIA_4K_AE2.json"
data = json.loads(src.read_text(encoding="utf-8"))
out = ROOT / "app" / "src" / "main" / "assets" / "bench" / "reference.json"
out.parent.mkdir(parents=True, exist_ok=True)
ref = {
    "device": data["device"],
    "gpu": "Mali-G57",
    "height": data["height"],
    "cap": 60,
    "date": data["date"],
    "fps": {k: round(v) for k, v in sorted(data["fps"].items())},
}
out.write_text(json.dumps(ref, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
print(f"{out.relative_to(ROOT)}: {len(ref['fps'])} presets, {out.stat().st_size / 1e3:.0f} kB")
