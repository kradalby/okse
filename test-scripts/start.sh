#!/bin/bash

# Java VM memory settings. Minimum and maximum memory size.
JVM_MEMORY="-Xms256m -Xmx512m"

DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
JARFILE="okse.jar"

if which java >/dev/null; then
    cd $DIR
    exec java -jar $JVM_MEMORY $JARFILE
    exit 0
else
    echo "Java is not found in your path, is it installed?"
    exit 1
fi

