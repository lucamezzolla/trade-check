@echo off
setlocal
cd /d "%~dp0"

echo Building TradeCheck...
call mvn clean package
if errorlevel 1 goto :error

set "JAR_FILE="
for %%F in (target\tradecheck-*.jar) do set "JAR_FILE=%%F"
if not defined JAR_FILE (
    echo Unable to locate the TradeCheck JAR in target\.
    goto :error
)

echo Starting TradeCheck...
java -jar "%JAR_FILE%"
if errorlevel 1 goto :error

endlocal
exit /b 0

:error
echo.
echo TradeCheck could not be started. Make sure Java 21 and Maven are installed and available in PATH.
pause
endlocal
exit /b 1
