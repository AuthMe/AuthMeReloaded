: Moves the AuthMe JAR file to the plugins folder of the test server
if "%jarfile%" == "" (
  call setvars.bat
)

if exist %jarfile% (
  xcopy %jarfile% %plugins% /y
) else (
  echo Target file not found: '%jarfile%'
)
