@echo off
rem Builds the mod jar. Needs JDK 21 and internet access.
cd /d "%~dp0"
if not exist gradlew.bat (
  where gradle >nul 2>nul || (echo Install Gradle 9.5+ from https://gradle.org/install or open the project in IntelliJ IDEA. & exit /b 1)
  call gradle wrapper --gradle-version 9.8.0 || exit /b 1
)
call gradlew.bat build || exit /b 1
echo.
echo Done. Your jar: build\libs\my-client-mod-mc1.21.11-1.2.0.jar
