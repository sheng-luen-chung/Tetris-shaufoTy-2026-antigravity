@echo off
echo ===================================================
echo [TEST] Compiling test and source files...
echo ===================================================
if not exist bin mkdir bin
javac -encoding UTF-8 -d bin src/com/tetris/main/Main.java src/com/tetris/controller/*.java src/com/tetris/model/*.java src/com/tetris/view/*.java src/com/tetris/util/*.java src/com/tetris/test/*.java

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Test Compilation failed!
    exit /b %ERRORLEVEL%
)

echo.
echo ===================================================
echo [TEST] Running Unit Tests via TestRunner...
echo ===================================================
java -cp bin com.tetris.test.TestRunner

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Unit tests failed! build aborted.
    exit /b %ERRORLEVEL%
)

echo.
echo [SUCCESS] All Unit Tests passed!
