@echo off
rem Builds the mod jar. Needs JDK 21 and Gradle 8.10+ on PATH (internet required on first run).
cd /d "%~dp0"
where gradle >nul 2>nul || (echo Gradle not found. Install Gradle 8.10+ and JDK 21. & exit /b 1)
if not exist gradlew.bat call gradle wrapper --gradle-version 8.10.2
call gradlew.bat build || exit /b 1
echo.
echo Done: see build\libs\hexpowers-1.0.0.jar
pause
