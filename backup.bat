@echo off
rem Zips the project into backups\ so a change can be thrown away later.
rem
rem   backup.bat            asks for a label
rem   backup.bat <label>    uses the label from the command line
rem
rem Put one back with restore-backup.bat.

setlocal EnableExtensions
cd /d "%~dp0"

if not exist "gradle.properties" (
    echo [ERROR] gradle.properties not found, is this the project root?
    goto :fail
)

if not exist "backups\" mkdir "backups"

set LABEL=%~1
if not "%LABEL%"=="" goto :have_label

set LABEL=
set /p LABEL=Label for this backup:
rem set /p keeps the prompt's trailing space in the value, so test a stripped copy.
set "BLANK=%LABEL: =%"
if "%BLANK%"=="" set LABEL=backup

:have_label
rem Trim the label down to characters that are safe in a filename.
set SAFE=%LABEL: =-%
set SAFE=%SAFE:"=%
set SAFE=%SAFE:/=-%
set SAFE=%SAFE:\=-%
set SAFE=%SAFE::=-%
if "%SAFE%"=="" set SAFE=backup

rem Drop trailing dashes, the stamp adds one of its own.
:trim
if "%SAFE:~-1%"=="-" (
    set "SAFE=%SAFE:~0,-1%"
    goto :trim
)

rem A sortable stamp, so restore-backup.bat can offer the newest one first.
powershell -NoProfile -Command "Get-Date -Format yyyyMMdd-HHmmss | Set-Content -Path .backup-stamp.txt"
if errorlevel 1 goto :fail
set /p STAMP=<.backup-stamp.txt

set VSH_LABEL=%SAFE%
set VSH_STAMP=%STAMP%

echo.
echo === backing up ===
powershell -NoProfile -Command ^
    "$ErrorActionPreference = 'Stop';" ^
    "$keep = @('src', 'gradle', 'build.gradle', 'settings.gradle', 'gradle.properties', 'gradlew', 'gradlew.bat', 'README.md', 'LICENSE', '.gitignore');" ^
    "$keep += @(Get-ChildItem -Path . -Filter *.bat -File | ForEach-Object { $_.Name });" ^
    "$keep = @($keep | Where-Object { Test-Path $_ } | Select-Object -Unique);" ^
    "if ($keep.Count -eq 0) { throw 'nothing to back up' };" ^
    "$zip = Join-Path 'backups' ($env:VSH_LABEL + '-' + $env:VSH_STAMP + '.zip');" ^
    "Compress-Archive -Path $keep -DestinationPath $zip -Force;" ^
    "Write-Host ('    ' + $keep.Count + ' items -> ' + $zip)"
if errorlevel 1 goto :fail

del .backup-stamp.txt
echo.
goto :done

:fail
if exist ".backup-stamp.txt" del .backup-stamp.txt
echo.
echo [FAILED] could not write the backup, see the error above.
goto :done

:done
echo.
pause
exit /b 0
