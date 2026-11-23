#!/usr/bin/env bash
# Build every Voidrun target sequentially (low-RAM safe: never parallel).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
JDK21="$ROOT/.jdk/jdk-21.0.13.1+2"
JDK25="$ROOT/.jdk/jdk-25.0.5.1+2"
LOG_DIR="$ROOT/spec"
mkdir -p "$LOG_DIR"

TARGETS=(
  "voidrun-forge-1.8.9:$JDK21"
  "voidrun-fabric-1.20.1:$JDK21"
  "voidrun-fabric-1.21.1:$JDK21"
  "voidrun-fabric-1.21.11:$JDK21"
  "voidrun-fabric-26.1.2:$JDK25"
  "voidrun-fabric-26.2:$JDK25"
  "voidrun-fabric-26.3:$JDK25"
)

for ENTRY in "${TARGETS[@]}"; do
  TARGET="${ENTRY%%:*}"
  JAVA="${ENTRY##*:}"
  echo "[$TARGET]"
  (
    cd "$ROOT/$TARGET"
    flock /tmp/voidrun-gradle.lock env JAVA_HOME="$JAVA" \
      ./gradlew build --no-daemon --console=plain 2>&1 | tee "$LOG_DIR/build-$TARGET.log"
  )
done
echo "OK: all targets built"
