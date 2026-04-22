#!/bin/sh
#
# Usage: ./move_plugin.sh
#

if [ -z $jarfile ];
   then
       ./setvars.sh
fi

if [ -f $jarfile ]
then
    cp $jarfile $plugins
else
    echo "Target file not found: $jarfile"
fi
