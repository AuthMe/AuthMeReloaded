: Build quickly without cleaning or testing
if "%jarfile%" == "" (
  call setvars.bat
)

mvn install -f "%pomfile%" -Dmaven.test.skip