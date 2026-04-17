@echo off
cd "%~dp0"
if not exist "bin" mkdir "bin"
echo Compiling...
javac -d "bin" -cp "lib/*" src/app/*.java src/ui/*.java src/Main.java
if %errorlevel% neq 0 (
    echo Compilation failed!
    pause
    exit /b
)
echo Running...
java -cp "bin;lib/*" Main
pause