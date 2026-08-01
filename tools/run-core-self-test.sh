#!/usr/bin/env sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
OUT=$(mktemp -d)
trap 'rm -rf "$OUT"' EXIT INT TERM
javac -Xlint:all -Werror -d "$OUT" \
  "$ROOT/app/src/main/java/com/hdclark/riskydice/rerolled/RiskSimulator.java" \
  "$ROOT/app/src/main/java/com/hdclark/riskydice/rerolled/RiskFormatter.java" \
  "$ROOT/tools/CoreSelfTest.java"
java -cp "$OUT" CoreSelfTest
