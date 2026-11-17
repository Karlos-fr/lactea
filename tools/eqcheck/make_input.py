"""Extract every equation block of every preset, the way projectM's PresetFileParser reads it,
for tools/eqcheck (see README.md).

Usage:  python -I tools/eqcheck/make_input.py <presets-dir> <output-file>

Each block is written twice: "old" = lines joined with a newline (projectM <= 4.1.x),
"milkdrop" = comments stripped and lines concatenated without separator (Milkdrop's
ReadCode() + StripLinefeedCharsAndComments()).
"""
import sys
from pathlib import Path

PREFIXES = ["per_frame_init_", "per_frame_", "per_pixel_"]
PREFIXES += [f"wave_{i}_{p}" for i in range(4) for p in ("init", "per_frame", "per_point")]
PREFIXES += [f"shape_{i}_{p}" for i in range(4) for p in ("init", "per_frame")]


def parse(data: bytes) -> dict:
    """PresetFileParser::Read + ParseLine: split on CR/LF, key up to the first ' ' or '='."""
    values = {}
    for raw in data.replace(b"\r", b"\n").split(b"\n"):
        if not raw:
            continue
        line = raw.decode("latin-1")
        pos = min((p for p in (line.find(" "), line.find("=")) if p >= 0), default=-1)
        if pos <= 0:
            continue
        values.setdefault(line[:pos], line[pos + 1:])
    return values


def lines(values: dict, prefix: str) -> list:
    out = []
    i = 1
    while f"{prefix}{i}" in values:
        out.append(values[f"{prefix}{i}"])
        i += 1
    return out


def strip_comment(line: str) -> str:
    cut = [p for p in (line.find("//"), line.find("\\\\")) if p >= 0]
    return line[:min(cut)] if cut else line


def old_mode(ls):
    return "".join((l[1:] if l.startswith("`") else l) + "\n" for l in ls)


def milkdrop_mode(ls):
    out = ""
    for l in ls:
        shader = l.startswith("`")
        l = strip_comment(l[1:] if shader else l)
        out += l + ("\n" if shader else "")
    return out


def main() -> int:
    root = Path(sys.argv[1])
    n = 0
    with open(sys.argv[2], "wb") as out:
        for f in sorted(root.rglob("*.milk")):
            key = f.relative_to(root).with_suffix("").as_posix()
            values = parse(f.read_bytes())
            for prefix in PREFIXES:
                ls = lines(values, prefix)
                if not ls:
                    continue
                for mode, code in (("old", old_mode(ls)), ("milkdrop", milkdrop_mode(ls))):
                    data = code.encode("latin-1")
                    out.write(f"{key}|{prefix}|{mode}\t{len(data)}\n".encode("utf-8") + data + b"\n")
                    n += 1
    print(f"{n} blocks")
    return 0


if __name__ == "__main__":
    sys.exit(main())
