# (c) Copyright 2009 Cloudera, Inc.


usage_string = """
  Usage: update-exercises (args)

  Args may include:
    --workspace       Discard your current Eclipse workspace and rebuild
                      based on the exercises available in the repository.
    --reset           Resets the git repository to "factory defaults"
    -f                Force changes. Don't prompt for --reset. Don't warn
                      about a username mismatch.

  This script updates your git repository to the most recent
  version of the files. 

  This should be invoked from the base of the cloudera-training
  git repository. 
"""

import os
import shutil
import sys
import time
import ConfigParser

class RepoException(Exception):
  """ Exception about the repository """
  def __init__(self, cause):
    self.cause = cause

  def __str__(self):
    return str(self.cause)

  def __repr__(self):
    return repr(self.cause)

TRAINING_USER = "training"
    
# This is the URL we expect for the 'origin' ref in the git repo.
# We don't care what protocol (git://, http://, etc) they use.
CLOUDERA_ORIGIN_URL = "//github.com/cloudera/cloudera-training.git"

# Where do we install the Eclipse workspace?
WORKSPACE_DIR = os.path.expanduser("~/workspace")

# Where do we draw the original version of that workspace from?
WORKSPACE_SRC = "pristine-workspace"

# When determining whether or not to auto-update the workspace,
# consult this file in the src and target workspace directories.
WORKSPACE_VER_FILE = ".cloudera-workspace-version"

def check_for_user(force):
  """ To check that we're running in the VM, check that the username is
      'training' or 'root'. Prompt if we're not (and force=False).
  """
  global TRAINING_USER

  username = os.getlogin()
  if username == TRAINING_USER or username == "root":
    # All's well.
    return 
  else:
    print """WARNING: Your username of '%(username)s' isn't what we expected.
If you run this script outside of the Cloudera Training VM, it may install
files and databases in locations you don't expect. 
""" % { "username" : username }

    if force:
      print "Since -f was given, we're continuing anyway..."
    else:
      print "If you're sure you want to do this, type 'yes' to continue."
      response = raw_input("> ")
      if response.lower() != "yes":
        print "Action canceled by user request."
        sys.exit(1)

def check_for_repo():
  """ Ensure that we're at the root of the cloudera-training git repository.
      Panic if not and throw a RepoException. No return value on success.
  """
  global CLOUDERA_ORIGIN_URL

  print "Checking for repository..."

  if not os.path.exists(".git/config"):
    raise RepoException("""You don't seem to be running this from inside a git repository.
You should change to the root of the Cloudera training git repository and
re-execute this program. To do this in the Cloudera training VM, run:
  $ cd ~/git
  $ ./update-exercises
""")

  # strip leading tab chars from the config file entries for Python's parser.
  shutil.copy(".git/config", "/tmp/cloudera-git-config")
  os.system("sed -i -e 's/^\t//' /tmp/cloudera-git-config")

  # parse the config file and check that the origin remote is correct.
  parser = ConfigParser.SafeConfigParser()
  parser.read("/tmp/cloudera-git-config")
  try:
    seen_origin_url = parser.get('remote "origin"', "url")
    if not seen_origin_url.endswith(CLOUDERA_ORIGIN_URL):
      raise RepoException("""It seems like you're running this script in the wrong repository.
You should change to the root of the Cloudera training git repository and
re-execute this program. To do this, run:
  $ cd ~/git
  $ ./update-exercises
""")
  except ConfigParser.NoSectionError:
    raise RepoException("""Error: Your git repository seems corrupted (no 'origin' remote).
Please run the following command and try again:
  $ git remote add origin %(origin_url)s
""" % { origin_url : "http:" + CLOUDERA_ORIGIN_URL })
  except ConfigParser.NoOptionError:
    raise RepoException("""Error: Your git repository seems corrupted (no url for origin).
Please run the following commands and try again:
  $ git remote rm origin
  $ git remote add origin %(origin_url)s
""" % { origin_url : "http:" + CLOUDERA_ORIGIN_URL })

  print "Training exercise repository found."


def reset_git_repo():
  """ Reset the repository to 'factory defaults'. Blows away changes to the repository. """
  print "Resetting repository state..."
  ret = os.system("git reset --hard HEAD")
  if ret > 0:
    raise RepoException("Could not reset repository state")
  ret = os.system("git clean -d -f .")
  if ret > 0:
    raise RepoException("Could not clean repository")
  ret = os.system("git checkout master")
  if ret > 0:
    raise RepoException("Could not check out master branch")


def update_repo():
  """ Get the latest changes """
  print "Updating repository..."
  ret = os.system("git pull origin master:master")
  if ret > 0:
    raise RepoException("Could not download updates. Are you connected to the network?")


def backup_workspace():
  """ If a workspace already exists, move it somewhere else first """

  if not os.path.exists(WORKSPACE_DIR):
    return # Don't worry, there's nothing to lose.

  date_suffix = "." + time.strftime('%Y-%m-%d')
  int_suffix = ""
  intval = 0
  while os.path.exists(WORKSPACE_DIR + date_suffix + int_suffix):
    intval = intval + 1
    int_suffix = "." + str(intval)

  target = WORKSPACE_DIR + date_suffix + int_suffix
  shutil.move(WORKSPACE_DIR, target)
  
  print """*************************************************************************
A new version of the Eclipse workspace for the training projects has been
made available. Your existing workspace was backed up to:
  %(backup)s
""" % { "backup" : target }


def workspace_needs_update():
  """ Return true if the workspace version id has changed between the deployed
      and pristine workspaces, signifying that we should backup the deployed
      workspace and install the pristine one.
  """

  try:
    h = open(os.path.join(WORKSPACE_SRC, WORKSPACE_VER_FILE))
    pristine_ver = int(h.readline().strip())
  except:
    # Can't read the version number in the pristine workspace. Weird. Skip this
    # with a warning, and continue.
    print e
    print """
WARNING: The pristine workspace directory contains an invalid version id.
Have you modified any files in the pristine-workspace directory? If so,
consider resetting your git repository to "factory defaults" by running this
script again with the --reset flag.

Skipping workspace auto-deployment.
"""
    return False
  finally:
    h.close()

  deployed_ver_file = os.path.join(WORKSPACE_DIR, WORKSPACE_VER_FILE)
  try:
    h = open(deployed_ver_file)
    deployed_ver = int(h.readline().strip())
  except:
    print """
WARNING: Your Eclipse workspace contains an invalid version id.
Have you modified the %(versionfile)s file?
If so, this script cannot auto-update your workspace. To update your workspace,
run this script again with --workspace. Otherwise, your workspace may not
reflect the latest training exercises.

Skipping workspace auto-deployment.
""" % { "versionfile" : deployed_ver_file }
    return False
    
  finally:
    h.close()

  # Don't auto-update the workspace if they've somehow regressed a version.
  return deployed_ver < pristine_ver


def refresh_workspace():
  """ Copy the pristine workspace over top of the user's current workspace """
  backup_workspace()
  shutil.copytree(WORKSPACE_SRC, WORKSPACE_DIR)


def get_permission():
  """ Ask the user if he's really sure he wants to throw away changes """
  print """WARNING: The arguments you have selected will cause me to discard all changes
to files under the ~/git directory. Are you sure? Please type 'yes' to continue."""

  response = raw_input("> ")
  return response.lower() == "yes"


def main(argv):
  update_workspace = False # If True, rebuild the eclipse workspace.
  full_reset = False # If True, git reset --hard and git clean
  force = False

  if len(argv) > 1:
    for arg in argv[1:]:
      if arg == "--help":
        print usage_string
        return 1
      elif arg == "--workspace":
        update_workspace = True
      elif arg == "--reset":
        full_reset = True
      elif arg == "-f":
        force = True
      else:
        print "Unknown argument. Try --help"
        return 1

  # Check that we're the "training" user -- or root.
  check_for_user(force)

  # Check that we're in the proper cloudera-training git repo.
  check_for_repo()

  # if the user uses --reset, run a git-reset on it.
  if full_reset:
    reset_ok = force or get_permission()
    if reset_ok:
      reset_git_repo()
    else:
      print "Skipped repository reset."

  # The actual git-pull update
  update_repo()

  # If the version file in the workspace has been raised, or
  # the user gives us --workspace, we need to update the workspace too.
  if update_workspace or workspace_needs_update():
    refresh_workspace()

  # Now that we've finished the update itself, run the post-update hook
  # to set any additional state necessary. So long as they haven't
  # modified the training VM too hard, this will install any additional
  # data files, databases, etc.
  print "Running post-update hook..."
  ret = os.system("python util/post-update.py")
  if ret > 0:
    raise RepoException("An error occurred running the post-update hook.")

  print "Done!"
  return 0


if __name__ == "__main__":
  try:
    ret = main(sys.argv)
    sys.exit(ret)
  except RepoException, re:
    print str(re)
    sys.exit(1)

