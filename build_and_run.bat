@echo off
setlocal

set "PROJECT_ROOT=%~dp0"
cd /d "%PROJECT_ROOT%"

call :choose_jdk
call :configure_java
if errorlevel 1 exit /b %errorlevel%

echo ===================================================
echo [0/3] Running Unit Tests (CI Mode)...
echo ===================================================
call run_tests.bat
if errorlevel 1 (
    echo [ERROR] Unit Tests failed! Aborting build process.
    pause
    exit /b %errorlevel%
)

echo.
echo ===================================================
echo [1/3] Compiling Java source files...
echo ===================================================
if not exist "bin" mkdir "bin"
"%JAVAC_CMD%" -encoding UTF-8 -d bin src\com\tetris\main\Main.java src\com\tetris\controller\*.java src\com\tetris\model\*.java src\com\tetris\view\*.java src\com\tetris\util\*.java src\com\tetris\test\*.java

if errorlevel 1 (
    echo.
    echo [ERROR] Compilation failed! Please check your source code.
    pause
    exit /b %errorlevel%
)

echo.
echo ===================================================
echo [1.5/3] Copying resources to bin...
echo ===================================================
if not exist "bin\resources" mkdir "bin\resources"
xcopy /E /Y /I "src\resources" "bin\resources" >nul

echo.
echo ===================================================
echo [2/3] Packaging classes into Tetris.jar...
echo ===================================================
jar cvfm Tetris.jar manifest.txt -C bin .

if errorlevel 1 (
    echo.
    echo [ERROR] Packaging failed!
    pause
    exit /b %errorlevel%
)

echo.
echo ===================================================
echo [3/3] Running Tetris.jar...
echo ===================================================
"%JAVA_CMD%" -jar Tetris.jar
exit /b %errorlevel%

:configure_java
if defined SELECTED_JDK (
    set "JAVA_HOME=%SELECTED_JDK%"
    set "JAVA_CMD=%SELECTED_JDK%\bin\java.exe"
    set "JAVAC_CMD=%SELECTED_JDK%\bin\javac.exe"
    set "PATH=%SELECTED_JDK%\bin;%PATH%"
) else (
    where javac >nul 2>nul
    if errorlevel 1 (
        echo No JDK found. Please install a JDK, or set JAVA_HOME to a JDK folder.
        exit /b 1
    )
    set "JAVA_CMD=java"
    set "JAVAC_CMD=javac"
)

if defined SELECTED_JDK echo Selected JDK: %SELECTED_JDK%
echo Using java from:
where java
echo Using javac from:
where javac
echo.
"%JAVA_CMD%" -version
"%JAVAC_CMD%" -version
echo.
exit /b 0

:choose_jdk
set "SELECTED_JDK="
call :use_first_existing_jdk ".jdk"
call :use_first_existing_jdk ".jdk\jdk-*"
call :use_first_existing_jdk "C:\Program Files\Eclipse Adoptium\jdk-25*"
call :use_first_existing_jdk "C:\Program Files\Microsoft\jdk-25*"
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\javac.exe" (
    if not defined SELECTED_JDK set "SELECTED_JDK=%JAVA_HOME%"
)
call :use_first_existing_jdk "C:\Program Files\Eclipse Adoptium\jdk-21*"
call :use_first_existing_jdk "C:\Program Files\Eclipse Adoptium\jdk-17*"
call :use_first_existing_jdk "C:\Program Files\Microsoft\jdk-21*"
call :use_first_existing_jdk "C:\Program Files\Microsoft\jdk-17*"
call :use_first_existing_jdk "C:\Program Files\Java\jdk-*"
exit /b 0

:use_first_existing_jdk
if defined SELECTED_JDK exit /b 0
for /d %%D in ("%~1") do (
    if exist "%%~fD\bin\javac.exe" (
        set "SELECTED_JDK=%%~fD"
        exit /b 0
    )
)
exit /b 0
