#! /usr/bin/env sh

# Launcher script for the SimRunner application.
#
# This script expects to be located in the "bin" subdirectory of the
# application's distribution. Consequently, the application's working
# directory will point to this script's parent directory.

WORKING_DIRECTORY=$0

if [ -L $WORKING_DIRECTORY ]; 
then
  WORKING_DIRECTORY=`readlink $0`  
fi

WORKING_DIRECTORY=`dirname $WORKING_DIRECTORY`
WORKING_DIRECTORY=`readlink -f $WORKING_DIRECTORY/..`

echo "Starting SimRunner (c) 2014 DEINC..."
echo "(Working directory: $WORKING_DIRECTORY)"

java -server -Xms64m -Xmx128m -cp "${WORKING_DIRECTORY}/lib/*" \
fm.simrunner.Main "$WORKING_DIRECTORY"

