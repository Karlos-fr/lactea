cmake_minimum_required(VERSION 3.22)
project(lactea_native LANGUAGES C CXX)

# --- libprojectM (unmodified, LGPL, built as a shared library) ---
set(ENABLE_PLAYLIST OFF CACHE BOOL "" FORCE)      # playlist handled in Kotlin
set(BUILD_SHARED_LIBS ON CACHE BOOL "" FORCE)     # dynamic linking (LGPL)
set(BUILD_TESTING OFF CACHE BOOL "" FORCE)
set(BUILD_DOCS OFF CACHE BOOL "" FORCE)
set(ENABLE_SDL_UI OFF CACHE BOOL "" FORCE)
set(ENABLE_DEBUG_POSTFIX OFF CACHE BOOL "" FORCE)

# projectM's version.h reads the *top-level* project version: give it projectM's own.
set(CMAKE_PROJECT_VERSION_MAJOR 4)
set(CMAKE_PROJECT_VERSION_MINOR 1)
set(CMAKE_PROJECT_VERSION_PATCH 8)

add_subdirectory(
        ${CMAKE_CURRENT_SOURCE_DIR}/../../../../third_party/projectm
        ${CMAKE_CURRENT_BINARY_DIR}/projectm
)

# Android logging available to projectM (diagnostics).
target_link_libraries(projectM PRIVATE log)

# --- Lactea JNI bridge ---
add_library(lactea SHARED lactea_jni.cpp)
target_link_libraries(lactea PRIVATE libprojectM::projectM GLESv3 log android)
