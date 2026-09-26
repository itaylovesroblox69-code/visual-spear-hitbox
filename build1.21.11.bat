@echo off
rem ---------------------------------------------------------------------------
rem Builds Visual Spear Hitbox for Minecraft 1.21.11 and lists the resulting jar.
rem
rem   build1.21.11.bat          build the jar
rem   build1.21.11.bat clean    wipe build outputs first
rem   build1.21.11.bat run      launch the dev client instead of building
rem ---------------------------------------------------------------------------

setlocal EnableExtensions
cd /d "%~dp0"

set MOD_VERSION=1.21.11
set TASK=build

if /i "%~1"=="clean" set TASK=clean build
if /i "%~1"=="run" set TASK=runClient
if /i "%~1"=="runclient" set TASK=runClient

if not exist "gradlew.bat" (
    echo [ERROR] gradlew.bat was not found next to this script.
    echo         Put this file in the project root and run it again.
    goto :fail
)

echo.
echo === Visual Spear Hitbox -^> Minecraft %MOD_VERSION% : gradlew.bat %TASK% ===
echo.

call "%~dp0gradlew.bat" %TASK% --console=plain
if errorlevel 1 goto :fail

if /i "%~1"=="run" goto :ranClient
if /i "%~1"=="runclient" goto :ranClient

echo.
echo === Build finished. Mod jars in build\libs: ===
if exist "build\libs\*.jar" (
    for %%F in ("build\libs\*.jar") do echo     %%~nxF
) else (
    echo     no jars found - did the build really succeed?
)
echo.
echo Drop the jar without "-sources" in it into .minecraft\mods.
echo Java 21 is required - this project targets Minecraft %MOD_VERSION%.
goto :done

:ranClient
echo.
echo Dev client closed.
goto :done

:done
echo.
pause
exit /b 0

:fail
echo.
echo [FAILED] Gradle did not finish successfully. Scroll up for the error.
pause
exit /b 1
