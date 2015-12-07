: Start the Minecraft server
if "%jarfile%" == "" (
  call setvars.bat
)

cd "%server%"
call java -Xmx1024M -Xms1024M -jar spigot_server.jar
cd "%batdir%"
dir /B *.bat