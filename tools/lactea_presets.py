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
