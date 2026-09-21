@echo off
setlocal
cd /d "%~dp0"

set "PORT=8080"
set "JAR=target\springbootProject-1.0-SNAPSHOT-exec.jar"
set "LOG=backend.log"

if not exist "%JAR%" (
    echo [ERROR] Backend jar not found: %JAR%
    echo Build it first with: mvn package
    pause
    exit /b 1
)

netstat -ano | findstr /R /C:":%PORT% .*LISTENING" >nul
if not errorlevel 1 (
    echo [INFO] Port %PORT% is already in use. Backend may already be running.
    echo Check %LOG% for the last startup log.
    pause
    exit /b 0
)

echo [INFO] Starting Spring Boot backend on port %PORT%...
echo [INFO] Logs: %CD%\%LOG%
start "Spring Boot Backend" /min cmd /c "java -jar %JAR% --server.port=%PORT% >> %LOG% 2>&1"
echo [INFO] Backend start command submitted.
echo [INFO] Verify with: http://127.0.0.1:%PORT%/api/auth/login
ping 127.0.0.1 -n 4 >nul
endlocal
