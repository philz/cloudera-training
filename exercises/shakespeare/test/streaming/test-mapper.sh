#!/bin/bash

set -o errexit

if [ $# != 1 ]; then
	echo "Usage: $0 mapper-script"
	exit 1
fi

MAPPER_OUTPUT=$(mktemp)

cat <<EOF | $1 > $MAPPER_OUTPUT
test@1	hello world
test@2	hello,	world!
test@3
EOF

cat <<'EOF' | diff -u $MAPPER_OUTPUT - && echo pass || echo fail
hello	test@1
world	test@1
hello	test@2
world	test@2
EOF
