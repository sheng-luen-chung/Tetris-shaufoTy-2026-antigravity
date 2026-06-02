@echo off
setlocal

set "PROJECT_ROOT=%~dp0"
cd /d "%PROJECT_ROOT%"

set "JDK25_HOME=C:\Program Files\Eclipse Adoptium\jdk-25.0.2.10-hotspot"
if exist "%JDK25_HOME%\bin\javac.exe" (
    set "JAVA_HOME=%JDK25_HOME%"
    set "PATH=%JAVA_HOME%\bin;%PATH%"
)

echo Using java from:
where java
echo Using javac from:
where javac
echo.
java -version
javac -version
echo.

if not exist "bin" mkdir "bin"

javac -d bin src\com\tetris\main\Main.java src\com\tetris\controller\*.java src\com\tetris\model\*.java src\com\tetris\view\*.java src\com\tetris\util\*.java src\com\tetris\test\*.java
if errorlevel 1 exit /b %errorlevel%

java -cp bin com.tetris.test.TestRunner
