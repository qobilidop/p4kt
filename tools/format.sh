#!/bin/sh

set -e

cd "$(dirname "$0")/.."

CHECK=false
if [ "$1" = "--check" ]; then
	CHECK=true
fi

FAILED=false

# Kotlin (ktfmt)
echo "Formatting Kotlin..."
KT_FILES=$(find . -name '*.kt' -not -path '*/bazel-*/*')
if [ -n "$KT_FILES" ]; then
	if [ "$CHECK" = true ]; then
		# shellcheck disable=SC2086
		java -jar /usr/local/lib/ktfmt.jar --google-style --set-exit-if-changed --dry-run $KT_FILES || FAILED=true
	else
		# shellcheck disable=SC2086
		java -jar /usr/local/lib/ktfmt.jar --google-style $KT_FILES
	fi
fi

# Bazel (buildifier)
echo "Formatting Bazel..."
BAZEL_FILES=$(find . -type f \( -name BUILD.bazel -o -name '*.bzl' -o -name MODULE.bazel -o -name WORKSPACE \) -not -path '*/bazel-*/*')
if [ -n "$BAZEL_FILES" ]; then
	if [ "$CHECK" = true ]; then
		# shellcheck disable=SC2086
		buildifier -mode=check $BAZEL_FILES || FAILED=true
	else
		# shellcheck disable=SC2086
		buildifier $BAZEL_FILES
	fi
fi

# Shell (shfmt)
echo "Formatting Shell..."
SHELL_FILES="dev tools/format.sh tools/lint.sh tools/docs.sh tools/generate_examples_doc.sh"
if [ "$CHECK" = true ]; then
	# shellcheck disable=SC2086
	shfmt -d $SHELL_FILES || FAILED=true
else
	# shellcheck disable=SC2086
	shfmt -w $SHELL_FILES
fi

# Markdown, JSON, YAML (dprint)
echo "Formatting Markdown, JSON, YAML..."
if [ "$CHECK" = true ]; then
	dprint check || FAILED=true
else
	dprint fmt
fi

if [ "$FAILED" = true ]; then
	echo "Formatting check failed."
	exit 1
fi

echo "Done."
