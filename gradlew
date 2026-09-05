#!/usr/bin/env sh

##############################################################################
# Gradle start up script for POSIX. If missing, Android Studio will offer to
# regenerate this file automatically the first time you open the project
# (File > Sync Project with Gradle Files), or run: gradle wrapper
##############################################################################

DEFAULT_JVM_OPTS="-Xmx64m -Xms64m"
APP_HOME=$(cd "$(dirname "$0")" && pwd)
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

exec java $DEFAULT_JVM_OPTS -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
