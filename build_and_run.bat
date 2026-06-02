@echo off
echo ===================================================
echo [0/3] Running Unit Tests (CI Mode)...
echo ===================================================
call run_tests.bat
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Unit Tests failed! Aborting build process.
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo ===================================================
echo [1/3] Compiling Java source files...
echo ===================================================
if not exist bin mkdir bin
javac -encoding UTF-8 -d bin src/com/tetris/main/Main.java src/com/tetris/controller/*.java src/com/tetris/model/*.java src/com/tetris/view/*.java src/com/tetris/util/*.java src/com/tetris/test/*.java

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Compilation failed! Please check your source code.
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo ===================================================
echo [1.5/3] Copying resources to bin...
echo ===================================================
if not exist bin\resources mkdir bin\resources
xcopy /E /Y /I src\resources bin\resources >nul

echo.
echo ===================================================
echo [2/3] Packaging classes into Tetris.jar...
echo ===================================================
jar cvfm Tetris.jar manifest.txt -C bin .

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Packaging failed!
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo ===================================================
echo [3/3] Running Tetris.jar...
echo ===================================================
java -jar Tetris.jar
