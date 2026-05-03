@echo off
setlocal

set "ROOT=%~dp0"
cd /d "%ROOT%"

set "SRC=%ROOT%src"
set "INPUT=%~1"
set "CUP_JAR=%ROOT%tools\java-cup-11b.jar"

if "%INPUT%"=="" (
    echo Debe proporcionar un archivo de entrada.
    exit /b 1
)

if not exist "%CUP_JAR%" (
    echo No se encontro %CUP_JAR%
    exit /b 1
)

if not exist "%INPUT%" (
    echo %INPUT% ^(El sistema no puede encontrar el archivo especificado^)
    exit /b 1
)

java -cp ".;%SRC%;%CUP_JAR%" Main "%INPUT%"

endlocal