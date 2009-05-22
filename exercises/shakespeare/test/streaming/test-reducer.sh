#!/bin/bash

set -o errexit

if [ $# != 1 ]; then
	echo "Usage: $0 reducer-script"
	exit 1
fi

REDUCER_OUTPUT=$(mktemp)

cat <<EOF | $1 > $REDUCER_OUTPUT
hello	test@1
hello	test@2
world	test@1
world	test@2
EOF

cat <<'EOF' | diff -u $REDUCER_OUTPUT - && echo pass || echo fail
hello	test@1,test@2
world	test@1,test@2
EOF
