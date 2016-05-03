#!/bin/sh
#
# Usage: ./build_project.sh
#

if [ -z $jarfile ];
   then
       ./setvars.sh
fi

mvn clean install -f $pomfile -B
