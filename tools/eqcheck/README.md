# eqcheck

Compiles every equation block of a preset collection with projectM's expression evaluator
(projectm-eval), on the TV, to find the blocks that do not compile and to check that a change in
how projectM reads presets causes no regression.

```bash
# 1. Build for the TV (Android NDK, armeabi-v7a)
cmake -S tools/eqcheck -B build/eqcheck -G Ninja \
  -DCMAKE_TOOLCHAIN_FILE=$ANDROID_SDK/ndk/<version>/build/cmake/android.toolchain.cmake \
  -DANDROID_ABI=armeabi-v7a -DANDROID_PLATFORM=26 -DCMAKE_BUILD_TYPE=Release
cmake --build build/eqcheck

# 2. Extract the equations (old projectM joining and MilkDrop joining)
python -I tools/eqcheck/make_input.py third_party/presets-cream-of-the-crop eq_in.txt

# 3. Run on the TV (about 45 s for 145,000 blocks)
adb push build/eqcheck/eqcheck /data/local/tmp/
adb push eq_in.txt /data/local/tmp/
adb shell "chmod 755 /data/local/tmp/eqcheck && /data/local/tmp/eqcheck < /data/local/tmp/eq_in.txt > /data/local/tmp/eq_out.txt"
adb pull /data/local/tmp/eq_out.txt
```

Each output line is `<preset>|<block>|<mode>` then `OK`, or `ERR`, the position and the message.
