#!/bin/sh

set -e

BINARY="$1"
EXPECTED="$2"

ACTUAL=$("$BINARY")

if [ "$ACTUAL" != "$(cat "$EXPECTED")" ]; then
	echo "Golden test failed."
	echo ""
	echo "=== Expected ==="
	cat "$EXPECTED"
	echo ""
	echo "=== Actual ==="
	echo "$ACTUAL"
	exit 1
fi
