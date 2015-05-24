#!/bin/bash

# Java VM memory settings. Minimum and maximum memory size.
JVM_MEMORY="-Xms1024m -Xmx2048m"

DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
JARFILE="$DIR/okse.jar"

if test -n "$JAVA_HOME"; then
    exec java -jar $JVM_MEMORY $JARFILE
    exit 1
else
    echo "JAVA_HOME is not set, exiting..."
    exit 7
fi

