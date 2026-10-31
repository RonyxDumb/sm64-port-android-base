#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SDL_DIR="$ROOT/app/jni/SDL"
VERSION="2.0.12"
ARCHIVE="$SDL_DIR/SDL2-$VERSION.zip"
EXTRACTED="$SDL_DIR/SDL-release-$VERSION"

mkdir -p "$SDL_DIR"
cd "$SDL_DIR"

# First try the original SDL distribution; GitHub is a fallback.
if [[ ! -f src/SDL.c || ! -f include/SDL.h ]]; then
    if [[ ! -d "$EXTRACTED" ]]; then
        if [[ ! -f "$ARCHIVE" ]]; then
            if ! curl -fL --connect-timeout 15 --max-time 120 --retry 2 -o "$ARCHIVE" "https://www.libsdl.org/release/SDL2-$VERSION.zip"; then
                rm -f "$ARCHIVE"
                curl -fL --connect-timeout 15 --max-time 180 --retry 3 -o "$ARCHIVE" "https://github.com/libsdl-org/SDL/archive/refs/tags/release-$VERSION.zip"
            fi
        fi
        unzip -q -o "$ARCHIVE"
    fi

    SOURCE="$EXTRACTED"
    if [[ ! -d "$SOURCE" ]]; then
        SOURCE="$SDL_DIR/SDL2-$VERSION"
    fi
    if [[ ! -f "$SOURCE/src/SDL.c" || ! -f "$SOURCE/include/SDL.h" ]]; then
        echo "SDL2 archive is incomplete or has an unexpected layout" >&2
        exit 1
    fi
    # Merge into possibly pre-existing directories; never move src into src/src.
    mkdir -p src include
    cp -a "$SOURCE/src/." src/
    cp -a "$SOURCE/include/." include/
fi

# NDK's Android.mk includes include/SDL2. Avoid privileged Windows symlinks:
# copy headers only (not recursively) to keep repeated runs safe.
mkdir -p include/SDL2
find include -maxdepth 1 -type f -exec cp -f {} include/SDL2/ \;

test -f src/SDL.c
test -f include/SDL.h
test -f include/SDL2/SDL_config.h
echo "SDL2 $VERSION prepared successfully."
