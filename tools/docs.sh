#!/bin/sh

set -e

cd "$(dirname "$0")/.."

# Generate examples page from examples/ directory
tools/generate_examples_doc.sh

case "${1:-serve}" in
serve)
	exec mkdocs serve -a 0.0.0.0:8000
	;;
build)
	exec mkdocs build
	;;
*)
	echo "Usage: ./dev docs [serve|build]"
	exit 1
	;;
esac
