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
SHELL_FILES="dev tools/format.sh"
if [ "$CHECK" = true ]; then
	# shellcheck disable=SC2086
	shfmt -d $SHELL_FILES || FAILED=true
else
	# shellcheck disable=SC2086
	shfmt -w $SHELL_FILES
fi

# Markdown (mdformat)
echo "Formatting Markdown..."
MD_FILES=$(find . -name '*.md' -not -path '*/bazel-*/*')
if [ -n "$MD_FILES" ]; then
	if [ "$CHECK" = true ]; then
		# shellcheck disable=SC2086
		mdformat --wrap no --check $MD_FILES || FAILED=true
	else
		# shellcheck disable=SC2086
		mdformat --wrap no $MD_FILES
	fi
fi

# YAML (yamlfmt)
echo "Formatting YAML..."
YAML_FILES=$(find . \( -name '*.yml' -o -name '*.yaml' \) -not -path '*/bazel-*/*')
if [ -n "$YAML_FILES" ]; then
	if [ "$CHECK" = true ]; then
		# shellcheck disable=SC2086
		yamlfmt -dry -quiet $YAML_FILES || FAILED=true
	else
		# shellcheck disable=SC2086
		yamlfmt $YAML_FILES
	fi
fi

if [ "$FAILED" = true ]; then
	echo "Formatting check failed."
	exit 1
fi

echo "Done."
