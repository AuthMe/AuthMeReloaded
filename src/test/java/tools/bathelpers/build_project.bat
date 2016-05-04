: Build the project normally
if "%jarfile%" == "" (
  call setvars.bat
)

mvn clean install -f "%pomfile%" -B
