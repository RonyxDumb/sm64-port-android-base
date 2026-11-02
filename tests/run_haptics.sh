#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
build=$(mktemp -d)
trap 'rm -rf "$build"' EXIT
javac -source 8 -target 8 -d "$build" \
    app/src/main/java/com/pipino/sm64port/RumbleEnvelope.java tests/RumbleEnvelopeTest.java
java -cp "$build" com.pipino.sm64port.RumbleEnvelopeTest
