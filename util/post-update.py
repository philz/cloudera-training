# (c) Copyright 2009 Cloudera, Inc.

"""
  Performs any 'install' actions associated with this repository.
  Execution of this script is delayed until after the git-pull
  issued by run-update.py, so that new data files, etc. can have
  new associated install actions.

  Users should not run this script manually.

  Multiple executions of this script should be idempotent. Care
  should be taken that even if it is interrupted part-way through
  a re-execution can recover the correct final state.
"""

import os
import shutil 
import sys


def setup_training_db():
  """
    The training SQL database should be flushed and reset with the
    shakespeare / bible data sets.
  """

  print "Resetting training database state..."
  os.system("mysql training < data/mysql-data")


def main(argv):
  setup_training_db()



if __name__ == "__main__":
  ret = main(sys.argv)
  sys.exit(ret)

