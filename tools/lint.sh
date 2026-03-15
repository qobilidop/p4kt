#!/bin/sh

set -e

cd "$(dirname "$0")/.."

FAILED=false

# Kotlin (detekt)
echo "Linting Kotlin..."
KT_FILES=$(find . -name '*.kt' -not -path '*/bazel-*/*' -not -path '*/examples/*')
if [ -n "$KT_FILES" ]; then
	# shellcheck disable=SC2086
	KT_INPUT=$(echo $KT_FILES | tr ' ' ',')
	java -jar /usr/local/lib/detekt.jar -i "$KT_INPUT" -c detekt.yml --build-upon-default-config || FAILED=true
fi

# Bazel (buildifier)
echo "Linting Bazel..."
BAZEL_FILES=$(find . -type f \( -name BUILD.bazel -o -name '*.bzl' -o -name MODULE.bazel -o -name WORKSPACE \) -not -path '*/bazel-*/*')
if [ -n "$BAZEL_FILES" ]; then
	# shellcheck disable=SC2086
	buildifier -lint=warn $BAZEL_FILES || FAILED=true
fi

# Shell (shellcheck)
echo "Linting Shell..."
SHELL_FILES="dev tools/format.sh tools/lint.sh tools/golden_test.sh tools/docs.sh tools/generate_examples_doc.sh"
# shellcheck disable=SC2086
shellcheck $SHELL_FILES || FAILED=true

# Dockerfile (hadolint)
echo "Linting Dockerfile..."
hadolint .devcontainer/Dockerfile || FAILED=true

if [ "$FAILED" = true ]; then
	echo "Linting failed."
	exit 1
fi

echo "Done."
