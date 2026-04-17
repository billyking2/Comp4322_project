@echo off

cd "%~dp0"
if not exist "bin" mkdir "bin"
javac -d "bin" -cp "lib/*" src/app/*.java src/ui/*.java src/Main.java