"""Generate Lactea's own presets, designed around the album cover.

They sample `sampler_rand00`: Lactea replaces it with the current album cover (see
CoverPresets.kt); without a cover, projectM picks a random image from the texture pack.

Usage:  python -I tools/lactea_presets.py   → packs-src/Lactea/*.milk (then tools/make_packs.py)
"""
from pathlib import Path

OUT = Path(__file__).resolve().parent.parent / "packs-src" / "Lactea"

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

    # Spectrum analyser identical to Winamp's (classic skin, default settings: "normal" colouring,
    # wide bands, moderate falloff, slow peak falloff), after webamp's VisPainter.ts, which
    # reproduces the Winamp 2.63 / 5.666 executables:
    # - FFT of the waveform, equalized by log10(1 + bias + (i + 1) (9 - bias) / 512) (bias 0.04,
    #   / 1.0025 per bin); the custom wave rebuilds it from projectM's spectrum (undoing
    #   projectM's own equalisation, 0.02 ln(512 / (512 - i)), and its different input scale and
    #   window: x 1024 / 24 / 480) and writes the 512 bins, in pixels / 16, as the colour of a line
    #   in a hidden row of pixels (row A; waves are aspect-corrected and upside down for the
    #   shaders: y = 0.23 is the row 0.5 + 0.27 x aspect.w of the textures);
    # - the warp shader computes, in row B, for each of the 19 bands: 4 of 75 columns (index
    #   91 % logarithmic, 9 % linear, interpolated between bins), averaged and truncated, at most
    #   15 px; the bar falls 12/16 px per frame (green); the peak, in 1/256 px (blue), falls by
    #   round(3 x 1.1^n) after n frames (red: n), restarting when the bar reaches it;
    #   steps are scaled to 60 frames per second from the real time between frames (q2), the TV
    #   may draw fewer;
    # - the comp shader draws Winamp's 76 x 16 pixel grid on the whole screen: black with grey
    #   dots, bars of 3 columns, round(bar) - 2 px high, one colour per row from the classic
    #   skin's VISCOLOR.TXT, grey peak at floor(peak) - 2.
    "Lactea - Spectre": dict(
        extra="""
per_frame_1=q2 = min(max((time - tprev) * 60, 0.25), 4);
per_frame_2=tprev = time;
wavecode_0_enabled=1
wavecode_0_samples=512
wavecode_0_sep=0
wavecode_0_bSpectrum=1
wavecode_0_bUseDots=0
wavecode_0_bDrawThick=1
wavecode_0_bAdditive=0
wavecode_0_scaling=1.000000
wavecode_0_smoothing=0.000000
wavecode_0_r=1.000000
wavecode_0_g=1.000000
wavecode_0_b=1.000000
wavecode_0_a=1.000000
wave_0_per_point1=bin = sample * 511;
wave_0_per_point2=eqpm = 0.02 * log(512 / (512 - min(bin, 511.5)));
wave_0_per_point3=raw = if(above(bin, 0.5), value1 / (0.15 * eqpm), 0);
wave_0_per_point4=bias = 0.04 / pow(1.0025, bin);
wave_0_per_point5=eqwa = log10(1 + bias + (bin + 1) * (9 - bias) / 512);
wave_0_per_point6=v = min(raw * 0.0888889 * eqwa / 16, 1);
wave_0_per_point7=x = (bin + 0.5) / 512;
wave_0_per_point8=y = 0.23;
wave_0_per_point9=r = v;
wave_0_per_point10=g = v;
wave_0_per_point11=b = v;
""",
        warp="""
shader_body
{
    float rowA = 0.5 + 0.27 * aspect.w;
    float rowB = rowA - 0.015;
    ret = float3(0.0, 0.0, 0.0);
    if (abs(uv.y - rowB) < 0.006)
    {
    float dy = texsize.w;
    float j = floor(uv.x * 19.0);
    float sum = 0.0;
    float col4;
    float si;
    float i1;
    float i2;
    float b1;
    float b2;
    float v1;
    float v2;
    col4 = j * 4.0 + 0.0;
    si = 0.09 * (col4 / 74.0 * 511.0) + 0.91 * pow(512.0, col4 / 74.0);
    i1 = min(floor(si), 511.0);
    i2 = min(ceil(si), 511.0);
    b1 = (i1 + 0.5) / 512.0;
    b2 = (i2 + 0.5) / 512.0;
    v1 = max(tex2D(sampler_main, float2(b1, rowA - dy)).x, max(tex2D(sampler_main, float2(b1, rowA)).x, tex2D(sampler_main, float2(b1, rowA + dy)).x));
    v2 = max(tex2D(sampler_main, float2(b2, rowA - dy)).x, max(tex2D(sampler_main, float2(b2, rowA)).x, tex2D(sampler_main, float2(b2, rowA + dy)).x));
    sum += lerp(v1, v2, si - floor(si)) * 16.0 * step(col4, 74.5);
    col4 = j * 4.0 + 1.0;
    si = 0.09 * (col4 / 74.0 * 511.0) + 0.91 * pow(512.0, col4 / 74.0);
    i1 = min(floor(si), 511.0);
    i2 = min(ceil(si), 511.0);
    b1 = (i1 + 0.5) / 512.0;
    b2 = (i2 + 0.5) / 512.0;
    v1 = max(tex2D(sampler_main, float2(b1, rowA - dy)).x, max(tex2D(sampler_main, float2(b1, rowA)).x, tex2D(sampler_main, float2(b1, rowA + dy)).x));
    v2 = max(tex2D(sampler_main, float2(b2, rowA - dy)).x, max(tex2D(sampler_main, float2(b2, rowA)).x, tex2D(sampler_main, float2(b2, rowA + dy)).x));
    sum += lerp(v1, v2, si - floor(si)) * 16.0 * step(col4, 74.5);
    col4 = j * 4.0 + 2.0;
    si = 0.09 * (col4 / 74.0 * 511.0) + 0.91 * pow(512.0, col4 / 74.0);
    i1 = min(floor(si), 511.0);
    i2 = min(ceil(si), 511.0);
    b1 = (i1 + 0.5) / 512.0;
    b2 = (i2 + 0.5) / 512.0;
    v1 = max(tex2D(sampler_main, float2(b1, rowA - dy)).x, max(tex2D(sampler_main, float2(b1, rowA)).x, tex2D(sampler_main, float2(b1, rowA + dy)).x));
    v2 = max(tex2D(sampler_main, float2(b2, rowA - dy)).x, max(tex2D(sampler_main, float2(b2, rowA)).x, tex2D(sampler_main, float2(b2, rowA + dy)).x));
    sum += lerp(v1, v2, si - floor(si)) * 16.0 * step(col4, 74.5);
    col4 = j * 4.0 + 3.0;
    si = 0.09 * (col4 / 74.0 * 511.0) + 0.91 * pow(512.0, col4 / 74.0);
    i1 = min(floor(si), 511.0);
    i2 = min(ceil(si), 511.0);
    b1 = (i1 + 0.5) / 512.0;
    b2 = (i2 + 0.5) / 512.0;
    v1 = max(tex2D(sampler_main, float2(b1, rowA - dy)).x, max(tex2D(sampler_main, float2(b1, rowA)).x, tex2D(sampler_main, float2(b1, rowA + dy)).x));
    v2 = max(tex2D(sampler_main, float2(b2, rowA - dy)).x, max(tex2D(sampler_main, float2(b2, rowA)).x, tex2D(sampler_main, float2(b2, rowA + dy)).x));
    sum += lerp(v1, v2, si - floor(si)) * 16.0 * step(col4, 74.5);
    float data = min(floor(sum / 4.0), 15.0);
    float k = q2;
    float3 st = tex2D(sampler_main, float2((j + 0.5) / 19.0, rowB)).xyz;
    float n = st.x * 255.0;
    float fall = st.y * 16.0 - 0.75 * k;
    fall = max(fall, data);
    float peak = st.z * 4096.0;
    float caught = step(peak, floor(fall * 256.0 + 0.5));
    peak = lerp(peak, fall * 256.0, caught);
    n = lerp(n, 0.0, caught);
    peak = max(peak - floor(3.0 * pow(1.1, n) + 0.5) * k, 0.0);
    n = min(n + k, 255.0);
    ret = float3(n / 255.0, fall / 16.0, peak / 4096.0);
    }
}""",
        comp="""
shader_body
{
    float rowB = 0.5 + 0.27 * aspect.w - 0.015;
    float c = floor(uv.x * 76.0);
    float top = floor(uv.y * 16.0);
    float r = 15.0 - top;
    float j = floor(c / 4.0);
    float3 st = tex2D(sampler_main, float2((j + 0.5) / 19.0, rowB)).xyz;
    float barH = floor(st.y * 16.0 + 0.5) - 2.0;
    float peakR = floor(st.z * 16.0) - 2.0;
    float inBar = step(fmod(c, 4.0), 2.5) * step(c, 74.5);
    float idx = 15.0 - r;
    float3 col = float3(0.9373, 0.1922, 0.0627);
    col = lerp(col, float3(0.8078, 0.1608, 0.0627), step(2.5, idx));
    col = lerp(col, float3(0.8392, 0.3529, 0.0000), step(3.5, idx));
    col = lerp(col, float3(0.8392, 0.4000, 0.0000), step(4.5, idx));
    col = lerp(col, float3(0.8392, 0.4510, 0.0000), step(5.5, idx));
    col = lerp(col, float3(0.7765, 0.4824, 0.0314), step(6.5, idx));
    col = lerp(col, float3(0.8706, 0.6471, 0.0941), step(7.5, idx));
    col = lerp(col, float3(0.8392, 0.7098, 0.1294), step(8.5, idx));
    col = lerp(col, float3(0.7412, 0.8706, 0.1608), step(9.5, idx));
    col = lerp(col, float3(0.5804, 0.8706, 0.1294), step(10.5, idx));
    col = lerp(col, float3(0.1608, 0.8078, 0.0627), step(11.5, idx));
    col = lerp(col, float3(0.1961, 0.7451, 0.0627), step(12.5, idx));
    col = lerp(col, float3(0.2235, 0.7098, 0.0627), step(13.5, idx));
    col = lerp(col, float3(0.1922, 0.6118, 0.0314), step(14.5, idx));
    col = lerp(col, float3(0.1608, 0.5804, 0.0000), step(15.5, idx));
    col = lerp(col, float3(0.0941, 0.5176, 0.0314), step(16.5, idx));
    float dots = step(fmod(c, 2.0), 0.5) * step(0.5, fmod(top, 2.0));
    float3 bg = lerp(float3(0.0000, 0.0000, 0.0000), float3(0.0941, 0.1294, 0.1608), dots);
    float bar = inBar * step(r, barH - 0.5);
    float pk = inBar * step(abs(r - peakR), 0.25) * step(0.0, peakR);
    ret = lerp(lerp(bg, col, bar), float3(0.5882, 0.5882, 0.5882), pk);
}"""),

    # Neon spectrum, Lactea's own: 48 mirrored bars (bass in the centre, treble towards the
    # edges) growing up and down from the middle line, with glowing trails (the image zooms and
    # turns slowly), bloom, chromatic aberration and a pulse on the bass.
    # Same data path as "Spectre": the custom wave writes the 512 bins, in decibels with a slope
    # towards the treble, in a hidden row of pixels (row A); the warp shader keeps the 24 band
    # heights in row B (fast rise, slow fall) and draws the bars over the zoomed previous image,
    # in the part of the screen above the hidden rows; the comp shader stretches that part to the
    # whole screen and adds the glow.
    "Lactea - Spectre neon": dict(
        extra="""
per_frame_1=q2 = min(max((time - tprev) * 60, 0.25), 4);
per_frame_2=tprev = time;
wavecode_0_enabled=1
wavecode_0_samples=512
wavecode_0_sep=0
wavecode_0_bSpectrum=1
wavecode_0_bUseDots=0
wavecode_0_bDrawThick=1
wavecode_0_bAdditive=0
wavecode_0_scaling=1.000000
wavecode_0_smoothing=0.000000
wavecode_0_r=1.000000
wavecode_0_g=1.000000
wavecode_0_b=1.000000
wavecode_0_a=1.000000
wave_0_per_point1=bin = sample * 511;
wave_0_per_point2=eqpm = 0.02 * log(512 / (512 - min(bin, 511.5)));
wave_0_per_point3=db = if(above(bin, 0.5), 20 * log10(max(value1 / eqpm, 0.000001)), -200);
wave_0_per_point4=k = 32 * log(1 + bin / 80) / log(6);
wave_0_per_point5=v = min(max((db + 1.8 * k - 16) / 67, 0), 1);
wave_0_per_point6=x = (bin + 0.5) / 512;
wave_0_per_point7=y = 0.23;
wave_0_per_point8=r = v;
wave_0_per_point9=g = v;
wave_0_per_point10=b = v;
""",
        warp="""
shader_body
{
    float rowA = 0.5 + 0.27 * aspect.w;
    float rowB = rowA - 0.015;
    float H = rowB - 0.012;
    float k = q2;
    ret = float3(0.0, 0.0, 0.0);
    if (uv.y > H)
    {
        if (abs(uv.y - rowB) < 0.006)
        {
            float i = floor(uv.x * 24.0);
            float lo = pow(400.0, i / 24.0);
            float hi = pow(400.0, (i + 1.0) / 24.0);
            float m = 0.0;
            m = max(m, max(tex2D(sampler_main, float2((lerp(lo, hi, 0.000) + 0.5) / 512.0, rowA)).x, tex2D(sampler_main, float2((lerp(lo, hi, 0.000) + 0.5) / 512.0, rowA - texsize.w)).x));
            m = max(m, max(tex2D(sampler_main, float2((lerp(lo, hi, 0.200) + 0.5) / 512.0, rowA)).x, tex2D(sampler_main, float2((lerp(lo, hi, 0.200) + 0.5) / 512.0, rowA - texsize.w)).x));
            m = max(m, max(tex2D(sampler_main, float2((lerp(lo, hi, 0.400) + 0.5) / 512.0, rowA)).x, tex2D(sampler_main, float2((lerp(lo, hi, 0.400) + 0.5) / 512.0, rowA - texsize.w)).x));
            m = max(m, max(tex2D(sampler_main, float2((lerp(lo, hi, 0.600) + 0.5) / 512.0, rowA)).x, tex2D(sampler_main, float2((lerp(lo, hi, 0.600) + 0.5) / 512.0, rowA - texsize.w)).x));
            m = max(m, max(tex2D(sampler_main, float2((lerp(lo, hi, 0.800) + 0.5) / 512.0, rowA)).x, tex2D(sampler_main, float2((lerp(lo, hi, 0.800) + 0.5) / 512.0, rowA - texsize.w)).x));
            m = max(m, max(tex2D(sampler_main, float2((lerp(lo, hi, 1.000) + 0.5) / 512.0, rowA)).x, tex2D(sampler_main, float2((lerp(lo, hi, 1.000) + 0.5) / 512.0, rowA - texsize.w)).x));
            float prev = tex2D(sampler_main, float2((i + 0.5) / 24.0, rowB)).y;
            float rise = lerp(prev, m, saturate(0.65 * k));
            float fall = prev - 0.022 * k;
            ret = float3(0.0, max(max(rise, fall), 0.0) * step(0.001, m + prev), 0.0);
        }
    }
    else
    {
        float2 p = float2(uv.x, uv.y / H);
        float2 d = p - 0.5;
        float a = 0.006 * sin(time * 0.37);
        float2 dr = float2(d.x * cos(a) - d.y * sin(a), d.x * sin(a) + d.y * cos(a));
        float2 src = 0.5 + dr * (0.975 - 0.015 * saturate(bass_att - 1.0));
        float3 fb = tex2D(sampler_main, float2(src.x, src.y * H)).xyz;
        fb = fb * 0.8 + (fb.yzx - fb) * 0.05;
        float ax = abs(p.x - 0.5) * 2.0;
        float bi = floor(ax * 24.0);
        float f = frac(ax * 24.0);
        float hv = tex2D(sampler_main, float2((bi + 0.5) / 24.0, rowB)).y;
        float hh = hv * 0.9;
        float ay = abs(p.y - 0.5) * 2.0;
        float wx = abs(f - 0.5) * 2.0;
        float body = (1.0 - smoothstep(0.5, 0.62, wx)) * (1.0 - smoothstep(hh - 0.01, hh + 0.01, ay)) * step(0.02, hv);
        float halo = exp(-max(ay - hh, 0.0) * 30.0) * exp(-max(wx - 0.55, 0.0) * 8.0) * step(0.02, hv);
        float t = bi / 23.0;
        float3 hue = 0.5 + 0.5 * cos(6.2831853 * (float3(0.0, 0.33, 0.67) + t * 0.75 + time * 0.035));
        float tip = saturate(ay / max(hh, 0.001));
        float3 neon = lerp(hue, float3(1.0, 1.0, 1.0), tip * tip * 0.55) * (0.45 + 0.55 * hv);
        float3 bars = neon * body + hue * halo * 0.15;
        float3 line = hue * exp(-ay * 80.0) * 0.2;
        ret = max(fb, bars + line);
    }
}""",
        comp="""
shader_body
{
    float rowB = 0.5 + 0.27 * aspect.w - 0.015;
    float H = rowB - 0.012;
    float2 q = float2(uv.x, uv.y * (H - 0.004));
    float2 d = uv - 0.5;
    float ca = 0.006 + 0.012 * saturate(bass_att - 1.0);
    float2 off = float2(d.x, d.y * H) * ca;
    float3 c = float3(tex2D(sampler_main, q + off).x, tex2D(sampler_main, q).y, tex2D(sampler_main, q - off).z);
    float3 glow = GetBlur1(q) * 0.55 + GetBlur2(q) * 0.45;
    float3 col = c + glow;
    col *= 1.0 + 0.15 * saturate(bass_att - 1.1);
    col *= 1.2 - dot(d, d) * 1.1;
    ret = col;
}"""),


    # Wormhole, film style: travel through a winding tunnel of turbulent plasma, bright blue,
    # cyan and white near the camera, dark navy and violet in the depth, with spiral rings round
    # the core and a white flare far away on the bass. The tunnel centre wanders widely (the
    # tunnel bends); the warp shader draws it over the previous image zoomed outwards (motion
    # blur), the comp shader adds bloom.
    "Lactea - Trou de ver film": dict(
        extra="""
per_frame_1=dt = min(max(time - tprev, 0), 0.1);
per_frame_2=tprev = time;
per_frame_3=pulse = pulse * 0.95 + 0.05 * min(max(bass_att - 1.0, 0), 1.5);
per_frame_4=q5 = pulse;
per_frame_5=travel = travel + dt * (0.4 + 0.5 * pulse);
per_frame_6=q1 = travel;
per_frame_7=q3 = 0.5 + 0.13 * sin(time * 0.41) + 0.05 * sin(time * 1.13);
per_frame_8=q4 = 0.5 + 0.10 * cos(time * 0.33) + 0.05 * sin(time * 0.97 + 1);
per_frame_9=mids = mids * 0.95 + 0.05 * min(max(mid_att - 0.8, 0), 1.5);
per_frame_10=q6 = mids;
""",
        warp="""
shader_body
{
    float2 c = float2(q3, q4);
    float2 p = (uv - c) * aspect.xy;
    float r = max(length(p), 0.001);
    float a = atan2(p.y, p.x) / 6.2831853;
    float z = 0.18 / r + q1;
    float sw = a + 0.02 / r + time * 0.015;
    float2 t1 = float2(sw, z * 0.22);
    float n = tex2D(sampler_noise_lq_lite, t1).x * 0.5 + tex2D(sampler_noise_lq_lite, t1 * 2.0 + 0.31).x * 0.3 + tex2D(sampler_noise_lq_lite, t1 * 4.0 + 0.67).x * 0.2;
    float cl = smoothstep(0.22, 0.88, n) * (0.85 + 0.3 * q6);
    float wall = saturate(r * 2.4);
    float core = smoothstep(0.02, 0.2, r);
    float3 navy = float3(0.1, 0.06, 0.28);
    float3 blue = float3(0.1, 0.32, 0.95);
    float3 cyan = float3(0.45, 0.85, 1.0);
    float3 white = float3(0.93, 0.97, 1.0);
    float3 col = lerp(navy, blue, cl * core);
    col = lerp(col, cyan, saturate(cl * cl * wall * 1.3));
    col = lerp(col, white, saturate(pow(cl, 4.0) * wall * 0.9));
    col *= 0.45 + 0.75 * core;
    float flare = q5 * (exp(-r * 45.0) + 0.25 * exp(-abs(p.y) * 220.0) * exp(-abs(p.x) * 5.0));
    col += white * flare;
    float2 src = c + (uv - c) * (0.975 - 0.01 * q5);
    float3 prev = tex2D(sampler_main, src).xyz;
    ret = lerp(col, max(col, prev * 0.9), 0.4);
}""",
        comp="""
shader_body
{
    float3 c = GetPixel(uv);
    float3 glow = GetBlur1(uv) * 0.35 + GetBlur2(uv) * 0.3;
    float2 d = uv - 0.5;
    ret = (c + glow) * (1.1 - dot(d, d) * 0.9);
}"""),

    # Wormhole, series style: star streaks rushing past a dark core, the round tunnel slowly
    # flattening into a corridor of light (a ceiling and a floor of blue and white light rushing
    # towards the camera, a dark slit in the middle, stars streaking sideways), and back.
    # q2 morphs between the round tunnel (1) and the flat corridor (0); the light walls are faint
    # in the round tunnel (mostly stars), bright in the corridor.
    "Lactea - Trou de ver serie": dict(
        extra="""
per_frame_1=dt = min(max(time - tprev, 0), 0.1);
per_frame_2=tprev = time;
per_frame_3=travel = travel + dt * (0.6 + 1.2 * max(bass_att - 0.9, 0));
per_frame_4=q1 = travel;
per_frame_5=q2 = min(max(0.5 + 0.8 * sin(time * 0.07), 0), 1);
per_frame_6=pulse = pulse * 0.85 + 0.15 * min(max(bass_att - 1.1, 0), 1.5);
per_frame_7=q5 = pulse;
per_frame_8=q7 = min(max(treb_att - 0.9, 0), 1.5);
""",
        warp="""
shader_body
{
    float2 p = (uv - 0.5) * aspect.xy;
    float m = q2;
    float d = max(length(float2(p.x * m, p.y)), 0.001);
    float z = 0.1 / d + q1 * 1.6;
    float an = atan2(p.y, p.x) / 6.2831853;
    float u = lerp(p.x * (0.1 / d) * 0.6, an * 2.0, m);
    float2 t1 = float2(u, z * 0.18);
    float n = tex2D(sampler_noise_lq_lite, t1).x * 0.6 + tex2D(sampler_noise_lq_lite, t1 * 2.0 + 0.41).x * 0.4;
    float stk = tex2D(sampler_noise_lq, float2(u * 3.0, z * 0.03)).x;
    float lgt = smoothstep(0.4, 0.85, n) * (0.4 + 0.9 * stk * stk);
    float gp = smoothstep(0.015, 0.11, d);
    float wl = lerp(1.0, 0.18, m) * (0.8 + 0.4 * q5);
    float3 blue = float3(0.12, 0.38, 1.0);
    float3 cyan = float3(0.5, 0.85, 1.0);
    float3 white = float3(0.95, 0.98, 1.0);
    float3 col = (blue * lgt + cyan * lgt * lgt + white * pow(lgt, 4.0)) * gp * wl;
    float r = max(length(p), 0.001);
    float a2 = atan2(p.y, p.x) / 6.2831853 + 0.5;
    float sid = floor(a2 * 600.0);
    float fa = abs(frac(a2 * 600.0) - 0.5);
    float2 cell = float2((fmod(sid, 256.0) + 0.5) / 256.0, (floor(sid / 256.0) + 0.5) / 256.0);
    float rnd = tex2D(sampler_noise_lq, cell).x;
    float rnd2 = tex2D(sampler_noise_lq, cell + float2(0.0, 0.1)).y;
    float zs = frac(rnd - q1 * 0.35);
    float rs = 0.03 / (zs + 0.03);
    float len = rs * (0.1 + 0.08 * q5);
    float st = step(rs - len, r) * step(r, rs) * step(fa, 0.12 + 0.2 * rs) * step(rnd2, 0.7 + 0.2 * q7);
    col += white * st * saturate(rs * 4.0) * (1.0 + 0.4 * q7);
    float sid2 = floor(a2 * 1500.0);
    float fa2 = abs(frac(a2 * 1500.0) - 0.5);
    float2 cell2 = float2((fmod(sid2, 256.0) + 0.5) / 256.0, (floor(sid2 / 256.0) + 20.5) / 256.0);
    float zs2 = frac(tex2D(sampler_noise_lq, cell2).z - q1 * 0.25);
    float rs2 = 0.02 / (zs2 + 0.02);
    float st2 = step(rs2 - rs2 * 0.08, r) * step(r, rs2) * step(fa2, 0.15) * step(tex2D(sampler_noise_lq, cell2 + float2(0.0, 0.2)).x, 0.5 + 0.4 * m);
    col += float3(0.75, 0.85, 1.0) * st2 * saturate(rs2 * 3.0) * (0.5 + 0.5 * m);
    float2 src = 0.5 + (uv - 0.5) * (0.93 - 0.02 * q5);
    float3 prev = tex2D(sampler_main, src).xyz;
    ret = max(col, prev * 0.72);
}""",
        comp="""
shader_body
{
    float3 c = GetPixel(uv);
    float3 glow = GetBlur1(uv) * 0.45 + GetBlur2(uv) * 0.35;
    float2 d = uv - 0.5;
    float flare = q5 * 0.35 * exp(-abs(d.y) * 60.0) * exp(-abs(d.x) * 3.0);
    float2 L = float2(0.5 + 0.38 * sin(time * 0.23), 0.5 + 0.24 * sin(time * 0.31 + 1.0));
    float fvis = saturate(0.55 + 0.6 * sin(time * 0.13)) * (0.7 + 0.5 * q5);
    float2 dl = (uv - L) * aspect.xy;
    float sun = exp(-length(dl) * 70.0) * 1.6 + 0.4 * exp(-abs(dl.y) * 260.0) * exp(-abs(dl.x) * 7.0) + 0.25 * exp(-abs(dl.x) * 260.0) * exp(-abs(dl.y) * 12.0);
    float3 lens = float3(0.85, 0.93, 1.0) * sun;
    float2 g1 = 0.5 + (L - 0.5) * -0.45;
    float d1 = length((uv - g1) * aspect.xy);
    float3 rainbow = 0.5 + 0.5 * cos(6.2831853 * (d1 / 0.09 * 0.8 + float3(0.0, 0.33, 0.67)));
    lens += rainbow * exp(-pow((d1 - 0.09) / 0.05, 2.0)) * 0.18;
    float2 g2 = 0.5 + (L - 0.5) * -0.9;
    float d2 = length((uv - g2) * aspect.xy);
    lens += float3(0.3, 0.7, 1.0) * smoothstep(0.045, 0.035, d2) * 0.18;
    float2 g3 = 0.5 + (L - 0.5) * -1.35;
    float d3 = length((uv - g3) * aspect.xy);
    lens += float3(1.0, 0.45, 0.6) * smoothstep(0.07, 0.05, d3) * 0.12;
    ret = (c + glow + float3(0.5, 0.75, 1.0) * flare + lens * fvis) * (1.1 - dot(d, d) * 0.9);
}"""),

    # Water: the bottom of a lagoon seen through clear water (computed by the warp shader, once
    # per pixel: rippled sand under turquoise water absorbing with depth, and moving caustics, the
    # classic iterated formula, 5 steps), under a pane of glass with the glass physics of
    # Proscenium's dock (after Codex Glass, GpuGlass.java): refractive
    # edges with a rippling surface, 7-point diffusion, chromatic aberration, light grain, and a
    # sky reflection on the slopes; and its three water animations with their formulas: calm water
    # (crossed sine swell), liquid (slower, larger) and rain (drops with expanding rings).
    # Lengths are in Proscenium's units (unit = screen density: 2 at 1080p).
    # The music acts like nature would, never shaking the whole surface:
    # - each beat of the bass drops a drop somewhere (8 at a time, in q9..q32: x, y + strength,
    #   birth time), bigger on a strong beat; a few light drops keep falling in quiet passages;
    # - the swell follows the sound level over several seconds, like a tide;
    # - the mids set the speed of the liquid's flow and of the caustics;
    # - the treble makes the wave crests glint.
    "Lactea - Eau": dict(
        extra="""
per_frame_1=dt = min(max(time - tprev, 0), 0.1);
per_frame_2=tprev = time;
per_frame_3=tide = tide * 0.992 + 0.008 * min(max(vol_att, 0.3), 1.6);
per_frame_4=mids = mids * 0.97 + 0.03 * min(max(mid_att - 0.7, 0), 1.5);
per_frame_5=treb = treb * 0.9 + 0.1 * min(max(treb_att - 0.8, 0), 1.5);
per_frame_6=flow = flow + dt * (0.6 + 0.9 * mids);
per_frame_7=rain = rain + dt * 0.6;
per_frame_8=rise = bass - bprev;
per_frame_9=bprev = bass;
per_frame_10=beat = above(rise, 0.1) * above(bass, bass_att) * above(time - lastbeat, 0.25);
per_frame_11=lastbeat = if(beat, time, lastbeat);
per_frame_12=force = min(max(floor(rise * 12), 0), 4);
per_frame_13=slot = if(beat, (slot + 1) % 8, slot);
per_frame_14=s0 = beat * equal(slot, 0);
per_frame_15=x0 = if(s0, 0.08 + rand(1000) * 0.00084, x0);
per_frame_16=y0 = if(s0, 0.1 + rand(1000) * 0.0008 + force, y0);
per_frame_17=t0 = if(s0, time, t0);
per_frame_18=s1 = beat * equal(slot, 1);
per_frame_19=x1 = if(s1, 0.08 + rand(1000) * 0.00084, x1);
per_frame_20=y1 = if(s1, 0.1 + rand(1000) * 0.0008 + force, y1);
per_frame_21=t1 = if(s1, time, t1);
per_frame_22=s2 = beat * equal(slot, 2);
per_frame_23=x2 = if(s2, 0.08 + rand(1000) * 0.00084, x2);
per_frame_24=y2 = if(s2, 0.1 + rand(1000) * 0.0008 + force, y2);
per_frame_25=t2 = if(s2, time, t2);
per_frame_26=s3 = beat * equal(slot, 3);
per_frame_27=x3 = if(s3, 0.08 + rand(1000) * 0.00084, x3);
per_frame_28=y3 = if(s3, 0.1 + rand(1000) * 0.0008 + force, y3);
per_frame_29=t3 = if(s3, time, t3);
per_frame_30=s4 = beat * equal(slot, 4);
per_frame_31=x4 = if(s4, 0.08 + rand(1000) * 0.00084, x4);
per_frame_32=y4 = if(s4, 0.1 + rand(1000) * 0.0008 + force, y4);
per_frame_33=t4 = if(s4, time, t4);
per_frame_34=s5 = beat * equal(slot, 5);
per_frame_35=x5 = if(s5, 0.08 + rand(1000) * 0.00084, x5);
per_frame_36=y5 = if(s5, 0.1 + rand(1000) * 0.0008 + force, y5);
per_frame_37=t5 = if(s5, time, t5);
per_frame_38=s6 = beat * equal(slot, 6);
per_frame_39=x6 = if(s6, 0.08 + rand(1000) * 0.00084, x6);
per_frame_40=y6 = if(s6, 0.1 + rand(1000) * 0.0008 + force, y6);
per_frame_41=t6 = if(s6, time, t6);
per_frame_42=s7 = beat * equal(slot, 7);
per_frame_43=x7 = if(s7, 0.08 + rand(1000) * 0.00084, x7);
per_frame_44=y7 = if(s7, 0.1 + rand(1000) * 0.0008 + force, y7);
per_frame_45=t7 = if(s7, time, t7);
per_frame_46=q1 = tide;
per_frame_47=q2 = treb;
per_frame_48=q3 = rain;
per_frame_49=q4 = flow;
per_frame_50=q9 = x0;
per_frame_51=q10 = y0;
per_frame_52=q11 = t0;
per_frame_53=q12 = x1;
per_frame_54=q13 = y1;
per_frame_55=q14 = t1;
per_frame_56=q15 = x2;
per_frame_57=q16 = y2;
per_frame_58=q17 = t2;
per_frame_59=q18 = x3;
per_frame_60=q19 = y3;
per_frame_61=q20 = t3;
per_frame_62=q21 = x4;
per_frame_63=q22 = y4;
per_frame_64=q23 = t4;
per_frame_65=q24 = x5;
per_frame_66=q25 = y5;
per_frame_67=q26 = t5;
per_frame_68=q27 = x6;
per_frame_69=q28 = y6;
per_frame_70=q29 = t6;
per_frame_71=q30 = x7;
per_frame_72=q31 = y7;
per_frame_73=q32 = t7;
""",
        warp="""
shader_body
{
    float2 bu = float2(uv.x * texsize.x / texsize.y, uv.y);
    float ct = q4 * 0.45 + 23.0;
    float2 cu = bu * 2.1 + float2(0.031, 0.017) * time;
    cu += (float2(tex2D(sampler_noise_hq, bu * 0.11 + 0.13).x, tex2D(sampler_noise_hq, bu * 0.11 + 0.61).x) - 0.5) * 0.45;
    float2 pc = fmod(cu * 6.2831853, 6.2831853) - 250.0;
    float2 ii = pc;
    float c = 1.0;
    float t;
    t = ct * (1.0 - 3.5 / 1.0);
    ii = pc + float2(cos(t - ii.x) + sin(t + ii.y), sin(t - ii.y) + cos(t + ii.x));
    c += 1.0 / length(float2(pc.x / (sin(ii.x + t) / 0.005), pc.y / (cos(ii.y + t) / 0.005)));
    t = ct * (1.0 - 3.5 / 2.0);
    ii = pc + float2(cos(t - ii.x) + sin(t + ii.y), sin(t - ii.y) + cos(t + ii.x));
    c += 1.0 / length(float2(pc.x / (sin(ii.x + t) / 0.005), pc.y / (cos(ii.y + t) / 0.005)));
    t = ct * (1.0 - 3.5 / 3.0);
    ii = pc + float2(cos(t - ii.x) + sin(t + ii.y), sin(t - ii.y) + cos(t + ii.x));
    c += 1.0 / length(float2(pc.x / (sin(ii.x + t) / 0.005), pc.y / (cos(ii.y + t) / 0.005)));
    t = ct * (1.0 - 3.5 / 4.0);
    ii = pc + float2(cos(t - ii.x) + sin(t + ii.y), sin(t - ii.y) + cos(t + ii.x));
    c += 1.0 / length(float2(pc.x / (sin(ii.x + t) / 0.005), pc.y / (cos(ii.y + t) / 0.005)));
    t = ct * (1.0 - 3.5 / 5.0);
    ii = pc + float2(cos(t - ii.x) + sin(t + ii.y), sin(t - ii.y) + cos(t + ii.x));
    c += 1.0 / length(float2(pc.x / (sin(ii.x + t) / 0.005), pc.y / (cos(ii.y + t) / 0.005)));
    c /= 5.0;
    c = 1.17 - pow(abs(c), 1.4);
    float caus = pow(abs(c), 8.0);
    float n1 = tex2D(sampler_noise_hq, bu * 0.35).x;
    float n2 = tex2D(sampler_noise_hq, bu * 1.7 + 0.31).x;
    float n3 = tex2D(sampler_noise_hq, bu * 7.0 + 0.77).x;
    float rip = 0.5 + 0.5 * sin(bu.y * 38.0 + bu.x * 9.0 + n1 * 9.0);
    float sand = 0.78 + 0.1 * (n1 - 0.5) + 0.08 * (n2 - 0.5) + 0.1 * (n3 - 0.5) + 0.08 * rip;
    float3 bottom = float3(0.93, 0.84, 0.66) * sand;
    float depth = saturate(0.1 + 0.3 * n1 + 0.25 * (1.0 - uv.y));
    float3 water = lerp(float3(0.62, 0.93, 0.9), float3(0.12, 0.5, 0.62), depth);
    float3 col = bottom * water + float3(0.0, 0.05, 0.08) * depth;
    col += float3(0.9, 1.0, 0.97) * caus * (0.9 - 0.4 * depth);
    float2 d = uv - 0.5;
    col *= 1.05 - dot(d, d) * 0.6;
    ret = col;
}""",
        comp="""
float hsh(float x)
{
    return frac(sin(x * 12.9898) * 43758.5453);
}
float sdbox(float2 q, float2 hs, float r)
{
    float2 e = abs(q) - hs + r;
    return length(max(e, 0.0)) + min(max(e.x, e.y), 0.0) - r;
}
float3 cover(float2 s)
{
    return tex2D(sampler_main, s / texsize.xy).xyz;
}
float2 ripple(float2 px, float2 dp, float age, float amp, float ringR, float unit)
{
    float2 dd = px - dp;
    float dist = max(length(dd), 0.001);
    float rr = 10.0 * unit + age * ringR;
    float band = 5.5 * unit + age * max(3.0 * unit, ringR * 0.086);
    float rd = abs(dist - rr);
    float amt = (1.0 - smoothstep(0.0, band, rd)) * (1.0 - age) * (1.0 - age) * sin((dist - rr) * 0.62 / unit) * amp * 2.1;
    float2 o = dd / dist * amt;
    o.y += (1.0 - smoothstep(0.0, 5.5 * unit, dist)) * amp * 1.25 * (1.0 - smoothstep(0.0, 0.16, age));
    return o;
}
shader_body
{
    float unit = texsize.y / 540.0;
    float2 px = uv * texsize.xy;
    float2 hs = texsize.xy * 0.5;
    float2 q = px - hs;
    float cr = 24.0 * unit;
    float band0 = 58.0 * unit;
    float strength = 18.0 * unit;
    float d = sdbox(q, hs, cr);
    float edge = max(0.0, -d);
    float amount = (1.0 - smoothstep(0.0, band0, edge)) * strength;
    amount += sin(px.x * 0.035 / unit + px.y * 0.022 / unit + time * 0.6) * 2.0 * unit * (amount / strength);
    float gx = sdbox(q + float2(1.0, 0.0), hs, cr) - sdbox(q - float2(1.0, 0.0), hs, cr);
    float gy = sdbox(q + float2(0.0, 1.0), hs, cr) - sdbox(q - float2(0.0, 1.0), hs, cr);
    float2 n = float2(gx, gy) / max(length(float2(gx, gy)), 0.0001);
    float2 off = -n * amount;
    float2 chroma = n * (amount / strength) * 1.35 * unit;
    float2 o = float2(0.0, 0.0);
    float tide = q1;
    float t = time * 0.72;
    float sc = 6.2831853 / (118.0 * unit);
    float ca = 1.8 * unit * (0.7 + 0.8 * tide);
    float pp = sin(px.x * sc + px.y * sc * 0.35 + t);
    float s2 = sin(px.y * sc * 1.2 - t * 0.72);
    float dg = sin((px.x + px.y) * sc * 0.62 + t * 0.48);
    o.x += (pp * 0.72 + dg * 0.28) * ca;
    o.y += (s2 * 0.62 - dg * 0.24) * ca;
    float sh = sin(px.x * 0.113 / unit + px.y * 0.071 / unit + t * 1.37);
    o += float2(sh, -sh * 0.55) * 0.18 * unit;
    t = q4 * 0.46;
    sc = 6.2831853 / (86.0 * unit);
    float la = 3.4 * unit * 1.35 * (0.6 + 0.8 * tide);
    pp = sin(px.x * sc + px.y * sc * 0.35 + t);
    s2 = sin(px.y * sc * 0.83 - t * 0.72);
    dg = sin((px.x + px.y) * sc * 0.62 + t * 0.31);
    o.x += (pp * 0.72 + dg * 0.28) * la;
    o.y += (s2 * 0.62 - dg * 0.24) * la;
    sh = sin(px.x * 0.113 / unit + px.y * 0.071 / unit + t * 1.37);
    o += float2(sh, -sh * 0.55) * 0.36 * unit;
    float rt = q3;
    float cyc;
    float idx;
    float2 dp;
    float age;
    cyc = rt * (0.28 + hsh(3.19) * 0.18) + hsh(2.31);
    idx = floor(cyc);
    dp = float2((0.08 + hsh(5.47 + idx * 11.17) * 0.84) * texsize.x, (0.10 + hsh(8.91 + idx * 13.31) * 0.80) * texsize.y);
    o += ripple(px, dp, cyc - idx, 1.4 * unit, 58.0 * unit, unit);
    cyc = rt * (0.28 + hsh(6.38) * 0.18) + hsh(4.62);
    idx = floor(cyc);
    dp = float2((0.08 + hsh(10.94 + idx * 11.17) * 0.84) * texsize.x, (0.10 + hsh(17.82 + idx * 13.31) * 0.80) * texsize.y);
    o += ripple(px, dp, cyc - idx, 1.4 * unit, 58.0 * unit, unit);
    cyc = rt * (0.28 + hsh(9.57) * 0.18) + hsh(6.93);
    idx = floor(cyc);
    dp = float2((0.08 + hsh(16.41 + idx * 11.17) * 0.84) * texsize.x, (0.10 + hsh(26.73 + idx * 13.31) * 0.80) * texsize.y);
    o += ripple(px, dp, cyc - idx, 1.4 * unit, 58.0 * unit, unit);
    cyc = rt * (0.28 + hsh(12.76) * 0.18) + hsh(9.24);
    idx = floor(cyc);
    dp = float2((0.08 + hsh(21.88 + idx * 11.17) * 0.84) * texsize.x, (0.10 + hsh(35.64 + idx * 13.31) * 0.80) * texsize.y);
    o += ripple(px, dp, cyc - idx, 1.4 * unit, 58.0 * unit, unit);
    age = (time - q11) * 0.4;
    dp = float2(q9, frac(q10)) * texsize.xy;
    o += ripple(px, dp, saturate(age), 5.5 * unit * (0.7 + floor(q10) * 0.3) * step(age, 1.0) * step(0.001, q11), 190.0 * unit, unit);
    age = (time - q14) * 0.4;
    dp = float2(q12, frac(q13)) * texsize.xy;
    o += ripple(px, dp, saturate(age), 5.5 * unit * (0.7 + floor(q13) * 0.3) * step(age, 1.0) * step(0.001, q14), 190.0 * unit, unit);
    age = (time - q17) * 0.4;
    dp = float2(q15, frac(q16)) * texsize.xy;
    o += ripple(px, dp, saturate(age), 5.5 * unit * (0.7 + floor(q16) * 0.3) * step(age, 1.0) * step(0.001, q17), 190.0 * unit, unit);
    age = (time - q20) * 0.4;
    dp = float2(q18, frac(q19)) * texsize.xy;
    o += ripple(px, dp, saturate(age), 5.5 * unit * (0.7 + floor(q19) * 0.3) * step(age, 1.0) * step(0.001, q20), 190.0 * unit, unit);
    age = (time - q23) * 0.4;
    dp = float2(q21, frac(q22)) * texsize.xy;
    o += ripple(px, dp, saturate(age), 5.5 * unit * (0.7 + floor(q22) * 0.3) * step(age, 1.0) * step(0.001, q23), 190.0 * unit, unit);
    age = (time - q26) * 0.4;
    dp = float2(q24, frac(q25)) * texsize.xy;
    o += ripple(px, dp, saturate(age), 5.5 * unit * (0.7 + floor(q25) * 0.3) * step(age, 1.0) * step(0.001, q26), 190.0 * unit, unit);
    age = (time - q29) * 0.4;
    dp = float2(q27, frac(q28)) * texsize.xy;
    o += ripple(px, dp, saturate(age), 5.5 * unit * (0.7 + floor(q28) * 0.3) * step(age, 1.0) * step(0.001, q29), 190.0 * unit, unit);
    age = (time - q32) * 0.4;
    dp = float2(q30, frac(q31)) * texsize.xy;
    o += ripple(px, dp, saturate(age), 5.5 * unit * (0.7 + floor(q31) * 0.3) * step(age, 1.0) * step(0.001, q32), 190.0 * unit, unit);
    sh = sin(px.x * 0.097 / unit + px.y * 0.083 / unit + rt * 3.0);
    o += float2(sh, -sh * 0.45) * 0.12 * unit;
    float ol = length(o);
    o *= min(1.0, 30.0 * unit / max(ol, 0.0001));
    float e = min(min(px.x, texsize.x - px.x), min(px.y, texsize.y - px.y));
    float2 ao = o * smoothstep(0.0, 18.0 * unit, e);
    off += ao;
    float2 sp = px + off;
    float r = 1.2 * unit;
    float2 h1 = float2(r, 0.0);
    float2 h2 = float2(0.5 * r, 0.866 * r);
    float2 h3 = float2(-0.5 * r, 0.866 * r);
    float3 cg = cover(sp) * 0.22 + (cover(sp + h1) + cover(sp - h1) + cover(sp + h2) + cover(sp - h2) + cover(sp + h3) + cover(sp - h3)) * 0.13;
    float3 col = cg;
    col.x = lerp(cg.x, cover(sp - chroma).x, saturate(length(chroma) * 4.0));
    col.z = lerp(cg.z, cover(sp + chroma).z, saturate(length(chroma) * 4.0));
    float luma = dot(col, float3(0.299, 0.587, 0.114));
    col = saturate(lerp(float3(luma, luma, luma), col, 1.1));
    col += (tex2D(sampler_noise_hq, uv * texsize.xy / 256.0).x - 0.5) * 0.02;
    float2 slope = ao / (30.0 * unit);
    float glint = pow(saturate(dot(slope, float2(0.6, -0.8)) * 1.6), 6.0);
    col += float3(0.9, 0.97, 1.0) * glint * (0.12 + 0.6 * q2);
    col += float3(0.55, 0.75, 0.9) * saturate(length(slope) * 1.2) * 0.12;
    float2 hl = uv - float2(0.3 + 0.4 * sin(time * 0.07), 0.15);
    col += float3(1.0, 1.0, 1.0) * 0.06 * exp(-dot(hl, hl) * 6.0);
    ret = col;
}"""),
}


def shader_lines(prefix: str, code: str) -> str:
    lines = code.strip("\n").split("\n")
    return "\n".join(f"{prefix}_{i}=`{line}" for i, line in enumerate(lines, 1))


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    for name, p in PRESETS.items():
        text = HEADER.format(zoom=p.get("zoom", "1.000"), rot=p.get("rot", "0.000"))
        text += p.get("extra", "").strip("\n") + "\n" if p.get("extra") else ""
        if "warp" in p:
            text += shader_lines("warp", p["warp"]) + "\n"
        text += shader_lines("comp", p["comp"]) + "\n"
        (OUT / f"{name}.milk").write_text(text, encoding="ascii", newline="\r\n")
        print("wrote", name)


if __name__ == "__main__":
    main()
