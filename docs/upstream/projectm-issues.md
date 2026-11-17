# Messages à envoyer aux auteurs de projectM

Publiés le 17/11/2026 : [issue #1083](https://github.com/projectM-visualizer/projectm/issues/1083), [pull request #1084](https://github.com/projectM-visualizer/projectm/pull/1084), [projectm-eval#23](https://github.com/projectM-visualizer/projectm-eval/issues/23). Trois messages :
1. une **issue** sur projectM (le bug) ;
2. une **pull request** sur projectM (la correction, branche `fix/milkdrop-equation-compat` du fork) ;
3. une **issue** sur projectm-eval (le « . » seul).

---

## 1. Issue — projectM-visualizer/projectm

**Title:** 67 Cream of the Crop presets fail with "Could not compile ..." because equation lines are not joined like MilkDrop

**Body:**

While testing the whole [presets-cream-of-the-crop](https://github.com/projectM-visualizer/presets-cream-of-the-crop) collection (9,857 presets) with libprojectM 4.1.8 on Android (OpenGL ES 3.2), 67 presets fail to load, all with an equation compile error ("Could not compile per-frame code", "... per-pixel code", "... custom shape 0 per-frame code", ...). The same code paths are unchanged on `master`.

There are two causes, both differences with MilkDrop 2:

**1. Equation lines are joined with a newline instead of nothing.**
MilkDrop reads `per_frame_1`, `per_frame_2`, ... and concatenates them **without any separator**, after stripping `//` and `\\` comments (`ReadCode()` + `CState::StripLinefeedCharsAndComments()` in `vis_milk2/state.cpp`). Many presets rely on it, with a name or a number split across two lines, for example in *Supernova/Radiate/martin - sparky caleidoscope*:

```
per_frame_21=k1 =  is_
per_frame_22=beat*equal(index%2,0);
```

MilkDrop compiles `k1 = is_beat*equal(index%2,0);`. `PresetFileParser::GetCode()` adds `std::endl` after each line, so projectm-eval sees `is_ beat` and the preset fails. Other examples: `ob_size=.01+bass*mid/11` + `99;;` (*Fractal/Lattice/va ultramix2 - 534*), `gamma=1 + bass*bass_att` + `chng=...`. Interestingly, the documentation of `GetCode()` already says *"Equations are returned as a single, long line"* and that comments are stripped, but the implementation does neither.

**2. One block that does not compile rejects the whole preset.**
In MilkDrop, when a block fails to compile (`NSEEL_code_compile()` returns NULL), it only shows a warning (`IDS_WARNING_PRESET_X_ERROR_IN_PER_FRAME_CODE`, ...) and runs the preset without that block. projectM throws `MilkdropCompileException` and the preset is not loaded. This affects presets with typos MilkDrop tolerated, e.g. `additive=above(sin(time*200),0.99l);` or `y=if(c,.-.4,y);`.

**Measurements** (all 144,906 equation blocks of the collection compiled with projectm-eval):

| | Presets failing |
|---|---|
| Current behaviour | 67 |
| Lines joined like MilkDrop | 14 |
| + uncompilable blocks skipped like MilkDrop | 0 |

No block that compiles today fails with the MilkDrop joining. I have a fix and will open a PR.

---

## 2. Pull request — projectM-visualizer/projectm

**From:** `Karlos-fr/projectm:fix/milkdrop-equation-compat` → `projectM-visualizer/projectm:master`

**Title:** Read preset equations like MilkDrop: join lines without separator, skip blocks that do not compile

**Body:**

Fixes #<issue number>.

- `PresetFileParser::GetCode()`: equation lines are concatenated without a separator after stripping `//` and `\\` comments, as MilkDrop does in `ReadCode()` + `StripLinefeedCharsAndComments()`, and as the function's documentation already states. Shader lines (starting with a backtick) still get one line per line.
- `PerFrameContext`, `PerPixelContext`, `ShapePerFrameContext`, `WaveformPerFrameContext`, `WaveformPerPointContext`: a block that does not compile is logged with `LOG_WARN` and skipped instead of throwing `MilkdropCompileException`, so the preset runs without it, as in MilkDrop. Code handles were already checked for `nullptr` before execution.

**Testing**
- All 144,906 equation blocks of presets-cream-of-the-crop compiled with projectm-eval, before and after: no regression, failing presets go from 67 to 0.
- The 67 presets loaded and rendered on a Sony BRAVIA (Mali-G57, GLES 3.2) with the same change applied to 4.1.8.
- `master` with this change builds for Android (NDK, armeabi-v7a, GLES).

---

## 3. Issue — projectM-visualizer/projectm-eval

**Title:** A lone "." should be parsed as the number 0, like ns-eel2

**Body:**

In MilkDrop 2's ns-eel2 lexer (`nseel-lextab.c`), a single `.` is a valid floating point constant, converted with `atof(".")`, which is 0. So `y=.-.4;` is `y = 0 - 0.4`, and `x=.;` is `x = 0`.

projectm-eval's scanner requires at least one digit (`FLT_BASE {DIGIT}+\.{DIGIT}*|{DIGIT}*\.{DIGIT}+`), so these expressions fail with "syntax error, unexpected invalid token". A few presets of presets-cream-of-the-crop contain it, e.g. *Geometric/Symbols/amandio c - codex 4 - plot wag nz+ necrophors gadget lard* (`y=if(c,.-.4,y);`) and *Reaction/Liquid Blobby/pheremones is spelled pheromones asshole* (`q2=below(rad,.-53);`).

I checked ns-eel2's behaviour by running its lexer tables on these inputs: `.` alone gives a DBLCONST token. Accepting `\.` alone as 0 in `Scanner.l` would match MilkDrop.
