: Moves the AuthMe JAR file to the plugins folder of the test server
: You will have to hit 'Y' to really replace it if it already exists
if "%jarfile%" == "" (
  call setvars.bat
)

if exist %jarfile% (
  xcopy %jarfile% %plugins%
) else (
  echo Target file not found: '%jarfile%'
)
