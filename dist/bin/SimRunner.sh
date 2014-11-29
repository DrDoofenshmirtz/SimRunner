#! /usr/bin/env sh

# Launcher script for the SimRunner application.
#
# This script expects to be located in the "bin" subdirectory of the
# application's distribution. Consequently, the application's working
# directory will point to the parent directory of "bin".

APP_TITLE="FEREM (c) 2014 DEINC"
SIMTASK_NAME="simtask"
WORKING_DIRECTORY=$0

if [ -L "${WORKING_DIRECTORY}" ]; 
then
  WORKING_DIRECTORY=`readlink $0`  
fi

WORKING_DIRECTORY=`dirname "${WORKING_DIRECTORY}"`
WORKING_DIRECTORY=`readlink -f "${WORKING_DIRECTORY}"/..`

echo "Starting ${APP_TITLE}..."
echo "(Working directory: ${WORKING_DIRECTORY})"

(cd "${WORKING_DIRECTORY}" && \
java -server -Xms64m -Xmx128m \
-cp "${WORKING_DIRECTORY}/lib/*" \
-splash:"${WORKING_DIRECTORY}/resources/splash.jpg" \
-Dfm.simrunner.app-title="${APP_TITLE}" \
-Dfm.simrunner.working-directory="${WORKING_DIRECTORY}" \
-Dfm.simrunner.simtask-name="${SIMTASK_NAME}" \
fm.simrunner.Main)

