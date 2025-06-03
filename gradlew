#!/bin/bash
##############################################################################
##
##  Gradle start up script for UN*X
##
##############################################################################
GRADLE_HOME=$(dirname "$0")
exec "$GRADLE_HOME"/gradle/wrapper/gradle-wrapper.jar "$@"
