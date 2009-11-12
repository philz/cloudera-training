# (c) Copyright 2009 Cloudera, Inc.


usage_string = """
  Usage: update-exercises (args)

  Args may include:
    --workspace          Discard your current Eclipse workspace and rebuild
                         based on the exercises available in the repository.
    --reset              Resets the git repository to "factory defaults"
    -f                   Force changes. Don't prompt for --reset. Don't warn
                         about a username mismatch.
    --ignore-repo-check  Continue even if we're not using the official
                         Cloudera training repository.

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

# When determining what branch of the repository we should
# be on, consult this file in the root of the VM.
BRANCH_FILE = ".update-branch"


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

def switch_branch():
  """ Load the .update-branch file and switch to the branch
      specified by the user. Return the branch named in this
      file.
  """

  branch = None
  try:
    h = open(BRANCH_FILE)
    lines = h.readlines()
    h.close()
    for line in lines:
      line = line.strip()
      if len(line) > 0:
        branch = line
        break
  except IOError:
    # couldn't open the branch file.
    # We will just use the legacy branch.
    print "Warning, could not find " + BRANCH_FILE + "."

  if branch == None:
    print "Could not get correct branch information; using legacy branch."
    branch = "legacy"

  # Try to switch to this branch, if it already exists locally.
  ret = os.system("git checkout " + branch + " > /dev/null")
  if ret > 0:
    # It's not. Create a new branch based on the tracking source.
    ret = os.system("git checkout -b " + branch + " origin/" + branch)
    if ret > 0:
      print "Could not move to working branch: " + branch
      print "I might not be able to grab the most recent copy of the exercises"
      print "Please check that your ~/git/.update-branch file specifies the"
      print "correct virtual machine version."
    else:
      print "Now going to update to exercises intended for VM version: " \
          + branch
  else:
    print "Using virtual machine exercise version: " + branch

  return branch


def update_repo(branch):
  """ Get the latest changes """
  print "Updating repository..."
  ret = os.system("git pull origin " + branch + ":" + branch)
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


def get_cdh_version(representative_path, prefix, suffix):
  """ Find the cdh version component of a filename and return it or None.
      representative_path is a wildcard path to match
        (e.g., /path/to/hadoop-*-core.jar)
      prefix is all the characters in the path before the version string
      suffix is the unique substring that occurs immediately after the version
  """

  lines = os.popen("ls -1 " + representative_path).readlines()
  if len(lines) == 0:
    return None
  line = lines[0].strip()

  if len(line) > 0:
    start_offset = len(prefix)
    end_offset = line.index(suffix)
    return line[start_offset:end_offset]
  else:
    return None


def get_installed_hadoop_version():
  """ Return the version string (e.g., '0.20.1+152') of the installed CDH release """
  return get_cdh_version("/usr/lib/hadoop/hadoop-*-core.jar", \
      "/usr/lib/hadoop/hadoop-", "-core.jar")


def get_installed_pig_version():
  """ Return the version string of the installed CDH pig release """
  return get_cdh_version("/usr/lib/pig/pig-*-core.jar", \
      "/usr/lib/pig/pig-", "-core.jar")


def get_classpath_libs(libdir, maxdepth):
  """ Return the libraries that hadoop or another project depends on,
      formatted as classpath entries for an eclipse workspace.
  """

  lines = os.popen("find " + libdir + " -maxdepth " + str(maxdepth) \
      + " -name \"*.jar\"").readlines()
  libs = []
  for line in lines:
    libs.append("<classpathentry kind=\"lib\" path=\"" + line.strip() \
        + "\"/>\\")

  return "\n".join(libs) + "\n"


def refresh_workspace():
  """ Copy the pristine workspace over top of the user's current workspace """
  backup_workspace()
  shutil.copytree(WORKSPACE_SRC, WORKSPACE_DIR)

  # Since the workspace dependencies may change based on the installed
  # Hadoop version, grab all of these dynamically.
  hadoop_ver = get_installed_hadoop_version()
  if hadoop_ver != None:
    # Replace all the version tokens in the workspace files
    # with this version id.
    os.system("find " + WORKSPACE_DIR + " -name .classpath -exec " \
        + "sed -i -e 's/CLOUDERA_HADOOP_VERSION/" + hadoop_ver + "/g' {} \;")

  pig_ver = get_installed_pig_version()
  if pig_ver != None:
    # Replace all the version tokens in the workspace files
    # with this version id.
    os.system("find " + WORKSPACE_DIR + " -name .classpath -exec " \
        + "sed -i -e 's/CLOUDERA_PIG_VERSION/" + pig_ver + "/g' {} \;")

  hadoop_libs = get_classpath_libs("/usr/lib/hadoop/lib", 2)
  if hadoop_libs != None:
    # Replace a token in the .classpath files with all the lib entries
    # for the Hadoop dependency jars.
    os.system("find " + WORKSPACE_DIR + " -name .classpath -exec " \
        + "sed -i -e 's|CLOUDERA_HADOOP_DEPS|" + hadoop_libs + "|' {} \;")

  # Repeat this for Pig.
  pig_libs = get_classpath_libs("/usr/lib/pig/lib", 1)
  if pig_libs != None:
    os.system("find " + WORKSPACE_DIR + " -name .classpath -exec " \
        + "sed -i -e 's|CLOUDERA_PIG_DEPS|" + pig_libs + "|' {} \;")


def get_permission():
  """ Ask the user if he's really sure he wants to throw away changes """
  print """WARNING: The arguments you have selected will cause me to discard all changes
to files under the ~/git directory. Are you sure? Please type 'yes' to continue."""

  response = raw_input("> ")
  return response.lower() == "yes"


def main(argv):
  update_workspace = False # If True, rebuild the eclipse workspace.
  full_reset = False # If True, git reset --hard and git clean
  force = False # If True, don't check our username, etc.
  force_repo = False # If True, don't check our origin repo

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
      elif arg == "--ignore-repo-check":
        force_repo = True
      else:
        print "Unknown argument. Try --help"
        return 1

  # Check that we're the "training" user -- or root.
  check_for_user(force)

  # Check that we're in the proper cloudera-training git repo.
  try:
    check_for_repo()
  except RepoException, re:
    if force_repo:
      print "Got repo-location error, but continuing with --ignore-repo-check"
    else:
      raise re

  # if the user uses --reset, run a git-reset on it.
  if full_reset:
    reset_ok = force or get_permission()
    if reset_ok:
      reset_git_repo()
    else:
      print "Skipped repository reset."

  # Ensure that we are on the correct branch
  # based on the branch configuration file.
  curbranch = switch_branch()

  # The actual git-pull update
  update_repo(curbranch)

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

