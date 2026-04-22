#!/bin/sh

if [ -z $jarfile ]
then
    ./setvars.sh
fi

cd $server
java -Xmx1024M -Xms1024M -jar spigot_server.jar
cd $batdir
./list_files.sh
