@echo off

if "%JAVA_HOME%"=="" call:FIND_JAVA_HOME
"%JAVA_HOME%\bin\java.exe" -jar DayZLogger.jar %*
pause
goto:END

:FIND_JAVA_HOME
FOR /F "skip=2 tokens=2*" %%A IN ('REG QUERY "HKLM\Software\JavaSoft\Java Runtime Environment" /v CurrentVersion') DO set CurVer=%%B
FOR /F "skip=2 tokens=2*" %%A IN ('REG QUERY "HKLM\Software\JavaSoft\Java Runtime Environment\%CurVer%" /v JavaHome') DO set JAVA_HOME=%%B
goto:EOF

:END
