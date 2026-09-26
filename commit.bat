@echo off
rem Stages everything, commits it with a message you type, then pushes to origin.
rem
rem   commit.bat            asks for the message, commits and pushes
rem   commit.bat --no-push  commits only, for when you are offline
rem
rem The first push sets origin as the branch's upstream, so later ones are plain.

setlocal EnableExtensions
cd /d "%~dp0"

git rev-parse --is-inside-work-tree >nul 2>&1
if errorlevel 1 (
    echo.
    echo [ERROR] this is not a git repository.
    goto :fail
)

set MSG=
set /p MSG=Commit message:
rem set /p keeps the prompt's trailing space in the value, so test a stripped copy.
set "BLANK=%MSG: =%"
if not "%BLANK%"=="" goto :have_msg

echo.
echo [ERROR] no message given, nothing was committed.
goto :fail

:have_msg
set "DOPUSH=1"
if /i "%~1"=="--no-push" set "DOPUSH=0"

echo.
echo === committing ===
git add -A
if errorlevel 1 goto :fail

rem git diff --cached exits 0 when there is nothing staged, which means no commit to make.
git diff --cached --quiet
if not errorlevel 1 (
    echo.
    echo [ERROR] nothing to commit, the working tree is clean.
    goto :fail
)

git commit -m "%MSG%"
if errorlevel 1 goto :fail

if "%DOPUSH%"=="0" (
    echo.
    echo Committed. Not pushed, you asked for --no-push.
    goto :committed
)

echo.
echo === pushing ===
git remote get-url origin >nul 2>&1
if errorlevel 1 (
    echo.
    echo [ERROR] there is no remote called origin, so this was committed but not pushed.
    goto :fail
)

for /f "delims=" %%B in ('git rev-parse --abbrev-ref HEAD') do set "BRANCH=%%B"

rem No upstream yet means the branch has never been pushed, so set one.
git rev-parse --abbrev-ref --symbolic-full-name "@{upstream}" >nul 2>&1
if errorlevel 1 (
    git push -u origin "%BRANCH%"
) else (
    git push
)
if errorlevel 1 (
    echo.
    echo [ERROR] the commit is saved locally but the push failed. Scroll up for why,
    echo         usually no internet or no GitHub login on this machine.
    goto :fail
)

:committed
echo.
echo === done ===
git --no-pager log -1 --stat
echo.
goto :done

:fail
echo.
pause
exit /b 1

:done
echo.
pause
exit /b 0
