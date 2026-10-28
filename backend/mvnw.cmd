@echo off
rem Minimal Maven bootstrap script (uses global mvn if available, otherwise downloads Maven 3.9.9 into %USERPROFILE%\.m2\wrapper).
setlocal
where mvn >nul 2>nul
if %ERRORLEVEL%==0 (
  mvn %*
  exit /b %ERRORLEVEL%
)

set "MVN_VERSION=3.9.9"
set "MVN_HOME=%USERPROFILE%\.m2\wrapper\apache-maven-%MVN_VERSION%"
if not exist "%MVN_HOME%\bin\mvn.cmd" (
  echo Downloading Maven %MVN_VERSION%...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$ProgressPreference='SilentlyContinue'; New-Item -ItemType Directory -Force '%USERPROFILE%\.m2\wrapper' | Out-Null; Invoke-WebRequest 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MVN_VERSION%/apache-maven-%MVN_VERSION%-bin.zip' -OutFile '%USERPROFILE%\.m2\wrapper\maven.zip'; Expand-Archive -Force '%USERPROFILE%\.m2\wrapper\maven.zip' '%USERPROFILE%\.m2\wrapper'; Remove-Item '%USERPROFILE%\.m2\wrapper\maven.zip'"
  if errorlevel 1 exit /b 1
)
call "%MVN_HOME%\bin\mvn.cmd" %*
exit /b %ERRORLEVEL%
