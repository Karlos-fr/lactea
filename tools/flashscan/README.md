# flashscan — finding the presets that flash

Renders presets on a PC, faster than real time, and measures their flashes, so Lactea can tell
calm presets from those that strobe. The analysis follows the broadcast rule (ITU-R BT.1702, the
basis of the WCAG 2.x "three flashes" criterion and of the Harding test): more than 3 flashes in
one second, over at least a quarter of the screen, is a flashing preset.

1. `flashscan` (C++) plays each preset for 14 s on the same test music, at 30 frames per
   *virtual* second (projectM's clock is driven by `projectm_set_frame_time`, added to the
   Lactea fork of projectM), and records each frame as a 16 × 9 grid: relative luminance, red
   ratio and saturated-red value (WCAG definitions).
2. `tools/flash_report.py analyse` counts the flashes (general and red), the agitation (how much
   the image changes) and the mean luminance of each preset.

The test music is synthetic (`tools/flash_audio.py`): 120 BPM, punchy on purpose, since presets
flash on the beat; free of rights and identical for every preset.

## Build (Windows, Visual Studio 2022)

GLEW 2.2.0 and GLFW 3.4, from their official source releases, outside the repository:

```bash
B=D:/VibeCoding/lactea-build
# GLEW -> $B/deps
cmake -S $B/dl/glew/glew-2.2.0/build/cmake -B $B/build-glew -G "Visual Studio 17 2022" -A x64 \
      -DCMAKE_POLICY_VERSION_MINIMUM=3.5 -DBUILD_UTILS=OFF -DCMAKE_INSTALL_PREFIX=$B/deps
cmake --build $B/build-glew --config Release --target install
# flashscan (with projectM from third_party/projectm)
cmake -S tools/flashscan -B $B/build-flashscan -G "Visual Studio 17 2022" -A x64 \
      -DCMAKE_PREFIX_PATH=$B/deps -DGLFW_SOURCE_DIR=$B/dl/glfw/glfw-3.4 -DCMAKE_POLICY_VERSION_MINIMUM=3.5
cmake --build $B/build-flashscan --config Release --target flashscan
```

## Run

```bash
F=D:/VibeCoding/lactea-build/flash
python -I tools/flash_audio.py $F                                   # test music
python -I tools/flash_report.py sample 200 $F/pilot/list.txt         # or a list of every preset
$B/build-flashscan/Release/flashscan.exe --root third_party/presets-cream-of-the-crop \
    --list $F/pilot/list.txt --audio $F/flash-test.f32 \
    --textures third_party/presets-milkdrop-texture-pack/textures --out $F/pilot/out
python -I tools/flash_report.py analyse $F/pilot/out                 # -> report.json
```

An interrupted run resumes where it stopped. To calibrate the thresholds against human
judgement, `pick` chooses presets around the limits, `flashscan --clips <dir> --wav <music>`
films them and `montage` puts the clips in one numbered video.

## Limits

- A preset calm on the test music can flash on another song: this measures one punchy case.
- The PC renders like the TV up to small GPU differences; luminance is not affected much.
- It helps choose soft presets; it is no medical guarantee for photosensitive people.
