# Build for the TV with the Android NDK, see tools/eqcheck/README.md.
cmake_minimum_required(VERSION 3.21)
project(eqcheck C)

add_subdirectory(../../third_party/projectm/vendor/projectm-eval projectm-eval EXCLUDE_FROM_ALL)

add_executable(eqcheck eqcheck.c)
target_link_libraries(eqcheck PRIVATE projectM::Eval m)
