#!/usr/bin/python

import re
import sys

NONALPHA = re.compile("\W")

for input in sys.stdin.readlines():
  keyline = input.split("\t", 1)
  if (len(keyline) == 2):
    (key, line) = keyline
    for w in NONALPHA.split(line):
      if w:
        print w + "\t" + key
