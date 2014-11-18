#! /usr/bin/env sh

if [ ! -d "$1" ]
then
  echo "Usage: SimRunner.sh working-directory"
  
  exit 85
fi

SCRIPT_HOME=$0

if [ -L $SCRIPT_HOME ]; 
then
  SCRIPT_HOME=`readlink $0`  
fi

SCRIPT_HOME=`dirname $SCRIPT_HOME`

java -server -Xms32m -Xmx48m -cp "${SCRIPT_HOME}/../lib/*" \
fm.simrunner.Main "$1"

