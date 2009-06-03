#!/usr/bin/python
#
# Creates an index from word -> sentence.
# Makes the assumption that sentences do not cross block boundaries
# (imprecise). Also assumes that no sentence is more than 30 lines
# (for scalability).


import re
import sys

MAX_PREV_LINES = 30

# The previous lines that make up this sentence.
prevlines = []

NONALPHA = re.compile("\W")

def tokenize(sentence):
  """ turn a sentence into word -> sentence mappings and emit them """
  for w in NONALPHA.split(sentence):
    if w:
      try:
        # Quick check -- is this purely a number? If so, skip it.
        int(w)
      except ValueError:
        # nope. It's a real word. Emit.
        print w + "\t" + sentence


def find_one_of(haystack, options):
  """ find the first member of the 'options' list in the haystack string,
      or return -1 if none could be found.
  """

  best = -1
  for needle in options:
    cur = haystack.find(needle)
    if best == -1:
      # no best match yet; this is either it, or still unfound (-1).
      best = cur
    elif cur > -1:
      # this is a real match; take the first of the ones we have.
      best = min(best, cur)

  return best


def next_punctuation(line):
  """ return the index of the next punctuation character in the line,
      or -1 if no punctuation exists.
  """
  return find_one_of(line, ['.','?','!',':'])


for line in sys.stdin.readlines():
  # remove extraneous whitespace and convert tabs to spaces
  line = line.strip().replace('\t',' ')

  dotpos = next_punctuation(line)
  while dotpos != -1:
    # We found up to the end of a sentence.
    # split the line up on the sentence boundary
    next_part = line[dotpos + 1:]
    this_part = line[0:dotpos]

    # grab any previous fragments of the sentence from
    # previous lines.
    prevlines.append(this_part)
    sentence = ' '.join(prevlines) 

    # now tokenize the words in the sentence and
    # emit them
    tokenize(sentence)

    # If there is more of this line remaining,
    # start processing the next sentence.
    prevlines = []
    line = next_part
    dotpos = next_punctuation(line)

  if len(line) > 0:
    # We've found a line (or end of a line) that does not end 
    # a sentence. Put it in the buffer for later.
    prevlines.append(line)

  # If we have too many lines building up in the buffer,
  # just process them like they're a complete sentence.
  if len(prevlines) > MAX_PREV_LINES:
    sentence = ' '.join(prevlines)
    tokenize(sentence)
    prevlines = []


if len(prevlines) > 0:
  # Empty the line buffer at the end of the task
  sentence = ' '.join(prevlines)
  tokenize(sentence)


