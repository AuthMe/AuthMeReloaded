#!/bin/sh
#
# Usage: ./analyze_project.sh
#

if [ -z $jarfile ];
   then
       ./setvars.sh
fi

mvn clean verify sonar:sonar -f $pomfile
