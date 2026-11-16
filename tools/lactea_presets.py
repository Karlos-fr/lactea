"""Generate Lactea's own presets, designed around the album cover.

They sample `sampler_rand00`: Lactea replaces it with the current album cover (see
CoverPresets.kt); without a cover, projectM picks a random image from the texture pack.

Usage:  python -I tools/lactea_presets.py   → app/src/main/assets/presets/Lactea/*.milk
"""
from pathlib import Path

OUT = Path(__file__).resolve().parent.parent / "app" / "src" / "main" / "assets" / "presets" / "Lactea"

HEADER = """MILKDROP_PRESET_VERSION=201
PSVERSION=2
PSVERSION_WARP=2
PSVERSION_COMP=2
[preset00]
fRating=5.000000
fGammaAdj=1.000
fDecay=1.000
fVideoEchoZoom=1.000
fVideoEchoAlpha=0.000
nVideoEchoOrientation=0
nWaveMode=0
bAdditiveWaves=0
bWaveDots=0
bWaveThick=0
bModWaveAlphaByVolume=0
bMaximizeWaveColor=0
bTexWrap=1
bDarkenCenter=0
bRedBlueStereo=0
bBrighten=0
bDarken=0
bSolarize=0
bInvert=0
fWaveAlpha=0.000
fWaveScale=1.000
fWaveSmoothing=0.500
fWaveParam=0.000
fModWaveAlphaStart=0.750
fModWaveAlphaEnd=0.950
fWarpAnimSpeed=1.000
fWarpScale=1.000
fZoomExponent=1.000
fShader=0.000
zoom={zoom}
rot={rot}
cx=0.500
cy=0.500
dx=0.000
dy=0.000
warp=0.000
sx=1.000
sy=1.000
wave_r=1.000
wave_g=1.000
wave_b=1.000
wave_x=0.500
wave_y=0.500
ob_size=0.000
ob_r=0.000
ob_g=0.000
ob_b=0.000
ob_a=0.000
ib_size=0.000
ib_r=0.000
ib_g=0.000
ib_b=0.000
ib_a=0.000
nMotionVectorsX=0.000
nMotionVectorsY=0.000
mv_dx=0.000
mv_dy=0.000
mv_l=0.000
mv_r=0.000
mv_g=0.000
mv_b=0.000
mv_a=0.000
"""

PRESETS = {
    "Lactea - Kaleidoscope de pochette": dict(
        warp="""
sampler sampler_rand00;
shader_body
{
    float2 p = (uv - 0.5) * aspect.xy;
    float r = length(p);
    float a = atan2(p.y, p.x) + time * 0.12;
    float seg = 0.7853982;
    a = abs(fmod(a + 64.0 * seg, seg) - seg * 0.5);
    float2 k = float2(cos(a), sin(a)) * r * (1.25 - 0.3 * saturate(bass_att - 0.8));
    float3 cov = tex2D(sampler_rand00, frac(k + 0.5 + float2(time * 0.015, time * 0.01))).xyz;
    float3 prev = tex2D(sampler_main, 0.5 + (uv - 0.5) * 0.97).xyz;
    ret = lerp(prev * 0.94, cov, 0.12 + 0.25 * saturate(bass_att - 1.0));
}""",
        comp="""
shader_body
{
    float2 d = uv - 0.5;
    ret = GetPixel(uv) * (1.12 - dot(d, d) * 0.6);
}"""),

    "Lactea - Tunnel de pochette": dict(
        warp="""
sampler sampler_rand00;
shader_body
{
    float2 p = (uv - 0.5) * aspect.xy;
    float r = max(length(p), 0.001);
    float a = atan2(p.y, p.x);
    float2 t = float2(a * 0.3183099 + time * 0.03, 0.25 / r + time * (0.25 + 0.2 * bass_att));
    float3 cov = tex2D(sampler_rand00, frac(t)).xyz;
    float shade = saturate(r * 2.2);
    float3 prev = tex2D(sampler_main, uv).xyz;
    ret = lerp(prev * 0.9, cov * shade, 0.5);
}""",
        comp="""
shader_body
{
    float2 d = uv - 0.5;
    ret = GetPixel(uv) * (1.12 - dot(d, d) * 0.6);
}"""),

    "Lactea - Miroirs": dict(
        warp="""
sampler sampler_rand00;
shader_body
{
    float2 p = (uv - 0.5) * aspect.xy;
    float z = 1.6 + 0.4 * sin(time * 0.25) - 0.3 * saturate(bass_att - 1.0);
    float an = time * 0.05;
    float2 q = float2(p.x * cos(an) - p.y * sin(an), p.x * sin(an) + p.y * cos(an)) * z;
    q = abs(frac(q * 0.5 + 0.5) * 2.0 - 1.0);
    float3 cov = tex2D(sampler_rand00, q).xyz;
    float3 prev = tex2D(sampler_main, uv).xyz;
    ret = lerp(prev * 0.92, cov, 0.35);
}""",
        comp="""
shader_body
{
    float2 d = uv - 0.5;
    ret = GetPixel(uv) * (1.12 - dot(d, d) * 0.6);
}"""),

    "Lactea - Dissolution": dict(
        zoom="1.012", rot="0.002",
        warp="""
sampler sampler_rand00;
shader_body
{
    float2 n = (tex2D(sampler_noise_lq, uv * 0.6 + time * 0.02).xy - 0.5) * 0.01;
    float3 prev = tex2D(sampler_main, uv + n).xyz * 0.975;
    float2 p = (uv - 0.5) * aspect.xy;
    float inside = 1.0 - smoothstep(0.18, 0.22, max(abs(p.x), abs(p.y)));
    float3 cov = tex2D(sampler_rand00, saturate(p / 0.4 + 0.5)).xyz;
    ret = lerp(prev, cov, inside * (0.06 + 0.3 * saturate(bass_att - 1.1)));
}""",
        comp="""
shader_body
{
    float2 d = uv - 0.5;
    ret = GetPixel(uv) * (1.12 - dot(d, d) * 0.6);
}"""),

    "Lactea - Vagues": dict(
        warp="""
sampler sampler_rand00;
shader_body
{
    float2 p = uv;
    float amp = 0.01 + 0.03 * saturate(bass_att - 0.9);
    p.x += sin(uv.y * 18.0 + time * 2.0) * amp;
    p.y += sin(uv.x * 14.0 + time * 1.6) * amp;
    float2 c = (p - 0.5) * aspect.xy * 1.15 + 0.5;
    float split = 0.004 + 0.012 * saturate(treb_att - 0.9);
    float3 cov;
    cov.x = tex2D(sampler_rand00, frac(c + float2(split, 0.0))).x;
    cov.y = tex2D(sampler_rand00, frac(c)).y;
    cov.z = tex2D(sampler_rand00, frac(c - float2(split, 0.0))).z;
    float3 prev = tex2D(sampler_main, uv).xyz;
    ret = lerp(prev * 0.9, cov, 0.4);
}""",
        comp="""
shader_body
{
    float2 d = uv - 0.5;
    ret = GetPixel(uv) * (1.12 - dot(d, d) * 0.6);
}"""),

    "Lactea - Mosaique pulsee": dict(
        warp="""
sampler sampler_rand00;
shader_body
{
    float n = 3.0 + floor(fmod(time * 0.25, 4.0));
    float2 p = (uv - 0.5) * aspect.xy + 0.5;
    float2 cell = floor(p * n);
    float2 f = frac(p * n);
    float ph = frac(sin(dot(cell, float2(12.9898, 78.233))) * 43758.5453);
    float pulse = 0.85 + 0.3 * saturate(bass_att - 0.8) * step(0.5, frac(ph + time * 0.5));
    float2 g = (f - 0.5) / pulse + 0.5;
    float inside = step(0.0, g.x) * step(g.x, 1.0) * step(0.0, g.y) * step(g.y, 1.0);
    float3 cov = tex2D(sampler_rand00, saturate(g)).xyz * inside;
    float3 prev = tex2D(sampler_main, uv).xyz;
    ret = lerp(prev * 0.85, cov * (0.75 + 0.5 * ph), 0.45);
}""",
        comp="""
shader_body
{
    float2 d = uv - 0.5;
    ret = GetPixel(uv) * (1.12 - dot(d, d) * 0.6);
}"""),
}


def shader_lines(prefix: str, code: str) -> str:
    lines = code.strip("\n").split("\n")
    return "\n".join(f"{prefix}_{i}=`{line}" for i, line in enumerate(lines, 1))


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    for name, p in PRESETS.items():
        text = HEADER.format(zoom=p.get("zoom", "1.000"), rot=p.get("rot", "0.000"))
        text += shader_lines("warp", p["warp"]) + "\n" + shader_lines("comp", p["comp"]) + "\n"
        (OUT / f"{name}.milk").write_text(text, encoding="ascii", newline="\r\n")
        print("wrote", name)


if __name__ == "__main__":
    main()
