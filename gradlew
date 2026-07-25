#!/usr/bin/env sh

# Gradle wrapper script

PRG="$0"
DIR=`dirname "$PRG"`

WRAPPER_JAR="$DIR/gradle/wrapper/gradle-wrapper.jar"

if [ -f "$WRAPPER_JAR" ] && command -v java >/dev/null 2>&1; then
    exec java -jar "$WRAPPER_JAR" "$@"
elif command -v gradle >/dev/null 2>&1; then
    exec gradle "$@"
else
    echo "Error: Neither java with gradle-wrapper.jar nor system gradle executable was found." >&2
    exit 1
fi
