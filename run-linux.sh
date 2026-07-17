#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

echo "Building TradeCheck..."
mvn clean package

shopt -s nullglob
jar_files=(target/tradecheck-*.jar)
if (( ${#jar_files[@]} != 1 )); then
    echo "Unable to locate a single TradeCheck JAR in target/." >&2
    exit 1
fi

echo "Starting TradeCheck..."
exec java -jar "${jar_files[0]}"
