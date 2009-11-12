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


def config_tracking_branch():
  """
    If the user has not selected a branch to track (or is running
    an older VM (0.3.2 or before), write out a tracking branch
    file, and switch to the 'legacy' branch.
  """
  if not os.path.exists(".update-branch"):
    print """
Could not find an update-branch file. Switching your VM to the 'legacy'
branch. If you are running a VM version 0.3.2 or older, you are
strongly encouraged to download a more recent version of the training
VM. If you are running a newer version, you should edit
~/git/.update-branch to contain a single line of the form:
  vm-<version>

for example:
  vm-0.3.3

"""
    # Write out the file that picks this branch.
    h = open(".update-branch", "w")
    h.write("legacy")
    h.close()

    print "You should run update-exercises again to get the most"
    print "recently-updated exercises."

def check_vm_version():
  """ The git repository knows the most recent version that exists.
      If we're on an older version, we should encourage users to
      upgrade.
  """

  def print_corrupt_update_branch():
    """ .update-branch is corrupt. Print an error. """
    print "Your file ~/git/.update-branch appears to be corrupt."
    print "You might want to remove this file and re-execute"
    print "the update-exercises script."

  def get_latest_version():
    """ Look up the latest version of the VM. """
    MISSING_VERSION = [ '0', '0', '0' ]

    if not os.path.exists("vm/latest-version"):
      return MISSING_VERSION

    h = open("vm/latest-version")
    line = h.readline()
    if line == None:
      return MISSING_VERSION
    h.close()

    verline = line.strip()
    if len(verline) == 0:
      return MISSING_VERSION

    # This line is of the form '0.x.y'. Get the components out.
    return verline.split(".")

  def print_upgrade_available():
    print "A new version of the Cloudera Training VM is available!"
    print "You should strongly considering downloading the newest"
    print "version of the training VM from www.cloudera.com to"
    print "stay current with the most recent edition of Hadoop and"
    print "the training exercises."
    print ""

  def compare_versions(current, latest):
    """ If the latest version is newer, print an upgrade msg.
        'current' and 'latest' are both arrays of integers
        representing the version.
        e.g., version 0.3.2 is represented as ['0', '3', '2'].
    """
    cnt = min(len(current), len(latest))
    i = 0
    while i < cnt:
      if int(current[i]) < int(latest[i]):
        print_upgrade_available()
        return
      i = i + 1

  if not os.path.exists(".update-branch"):
    return # couldn't find .update-branch? No version for us!

  h = open(".update-branch")
  lines = h.readlines()
  h.close();

  if len(lines) == 0:
    # Empty .update-branch? No version info to work with.
    print_corrupt_update_branch()
    return

  # Ignore blank lines; find the branch line.
  branchline = None
  for line in lines:
    line = line.strip()
    if len(line) > 0:
      branchline = line
      break

  if branchline is not None and len(branchline) > 0:
    # We have a current branch.
    if branchline == "legacy":
      print_upgrade_available()
    elif branchline.startswith("vm-"):
      # line is of the form 'vm-0.x.y'
      current_version_str = branchline[3:] # get 0.x.y
      current_version_parts = current_version_str.split(".") # get [0, x, y]
      new_version_parts = get_latest_version()
      compare_versions(current_version_parts, new_version_parts)
    else:
      print_corrupt_update_branch()


def setup_training_db():
  """
    The training SQL database should be flushed and reset with the
    shakespeare / bible data sets.
  """

  print "Resetting training database state..."
  os.system("mysql training < data/mysql-data")


def main(argv):
  config_tracking_branch()
  check_vm_version()
  setup_training_db()



if __name__ == "__main__":
  ret = main(sys.argv)
  sys.exit(ret)

