#!/usr/bin/python

import sys

locations = []
word = None
for input in sys.stdin.readlines():
  input = input.rstrip()
  parts = input.split("\t")

  if len(parts) < 2:
    continue
  newword = parts[0]
  newlocations = parts[1:]

  if not word:
    word = newword

  if word != newword:
    print word + "\t" + ",".join(locations)
    word = newword
    locations = []

  locations.extend(newlocations)

if word != None:
  print word + "\t" + ",".join(locations)

