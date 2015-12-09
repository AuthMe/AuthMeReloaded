#!/bin/sh
#
# Usage: ./analyze_project.sh
#

if [ -z $jarfile ];
   then
       ./setvars.sh
fi

mvn install -f $pomfile -Dmaven.test.skip
