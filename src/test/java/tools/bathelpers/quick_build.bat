: Build quickly without cleaning or testing
if "%jarfile%" == "" (
  call setvars.bat
)

mvn install -o -f "%pomfile%" -Dmaven.test.skip
