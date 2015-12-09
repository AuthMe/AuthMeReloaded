: Analyze the project with Sonar (requires you install SonarQube)
if "%jarfile%" == "" (
  call setvars.bat
)

mvn clean verify sonar:sonar -f "%pomfile%"