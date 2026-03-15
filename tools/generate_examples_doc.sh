#!/bin/sh

set -e

cd "$(dirname "$0")/.."

OUTPUT="docs/examples.md"

cat >"$OUTPUT" <<'HEADER'
# Examples

Each example is a standalone P4kt program alongside the P4 code it produces.
Source files are in the [`examples/`](https://github.com/qobilidop/p4kt/tree/main/examples) directory.

HEADER

for kt_file in examples/*.kt; do
	name=$(basename "$kt_file" .kt)
	p4_file="examples/${name}.p4"

	if [ ! -f "$p4_file" ]; then
		continue
	fi

	# Convert name to title case
	title=$(echo "$name" | sed 's/_/ /g' | awk '{for(i=1;i<=NF;i++) $i=toupper(substr($i,1,1)) substr($i,2)}1')

	{
		echo "## ${title}"
		echo ""
		echo '<div class="grid" markdown>'
		echo ""
		echo '```kotlin title="P4kt"'
		cat "$kt_file"
		echo '```'
		echo ""
		echo '```p4 title="P4 Output"'
		cat "$p4_file"
		echo '```'
		echo ""
		echo '</div>'
		echo ""
	} >>"$OUTPUT"
done
