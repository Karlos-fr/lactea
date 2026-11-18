# flashscan: offline analysis of the presets' flashes on a PC (Windows, OpenGL 3.3).
# Needs GLEW installed (CMAKE_PREFIX_PATH); GLFW is taken from GLFW_SOURCE_DIR (official zip).
# See tools/flashscan/README.md.
cmake_minimum_required(VERSION 3.22)
project(flashscan LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# GLFW, built from its source release.
set(GLFW_BUILD_DOCS OFF CACHE BOOL "" FORCE)
set(GLFW_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(GLFW_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(GLFW_INSTALL OFF CACHE BOOL "" FORCE)
add_subdirectory(${GLFW_SOURCE_DIR} glfw)

# projectM: the same fork as the app, desktop OpenGL, static.
set(ENABLE_PLAYLIST OFF CACHE BOOL "" FORCE)
set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(BUILD_TESTING OFF CACHE BOOL "" FORCE)
set(BUILD_DOCS OFF CACHE BOOL "" FORCE)
set(ENABLE_SDL_UI OFF CACHE BOOL "" FORCE)
set(ENABLE_DEBUG_POSTFIX OFF CACHE BOOL "" FORCE)
set(ENABLE_SYSTEM_PROJECTM_EVAL OFF CACHE BOOL "" FORCE)
set(CMAKE_PROJECT_VERSION_MAJOR 4)
set(CMAKE_PROJECT_VERSION_MINOR 1)
set(CMAKE_PROJECT_VERSION_PATCH 8)
add_subdirectory(${CMAKE_CURRENT_SOURCE_DIR}/../../third_party/projectm projectm)

find_package(GLEW REQUIRED)
find_package(OpenGL REQUIRED)

add_executable(flashscan main.cpp)
target_link_libraries(flashscan PRIVATE libprojectM::projectM glfw GLEW::glew OpenGL::GL winmm)
if(MSVC)
    target_compile_options(flashscan PRIVATE /utf-8)
endif()

# The shared GLEW (the one projectM uses, initialised by flashscan): its DLL next to the program.
add_custom_command(TARGET flashscan POST_BUILD
    COMMAND ${CMAKE_COMMAND} -E copy_if_different $<TARGET_RUNTIME_DLLS:flashscan> $<TARGET_FILE_DIR:flashscan>
    COMMAND_EXPAND_LISTS)
