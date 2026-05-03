@echo off
setlocal EnableExtensions

REM ====== Rutas ======
set "ROOT=%~dp0"
set "SRC=%ROOT%src"
set "TOOLS=%ROOT%tools"

REM ====== Encontrar JFlex jar ======
set "JFLEX_JAR="
for %%F in ("%TOOLS%\jflex*-full*.jar" "%TOOLS%\jflex*.jar") do (
  if exist "%%~fF" set "JFLEX_JAR=%%~fF"
)

REM ====== Encontrar CUP jar ======
set "CUP_JAR="
for %%F in ("%TOOLS%\java-cup*.jar" "%TOOLS%\cup*.jar") do (
  if exist "%%~fF" set "CUP_JAR=%%~fF"
)

if "%JFLEX_JAR%"=="" (
  echo [ERROR] No se encontro JFlex jar en %TOOLS%
  echo Asegurate de tener algo como: jflex-full-1.9.x.jar
  exit /b 1
)

if "%CUP_JAR%"=="" (
  echo [ERROR] No se encontro CUP jar en %TOOLS%
  echo Asegurate de tener algo como: java-cup-11b.jar
  exit /b 1
)

echo === JFlex: %JFLEX_JAR%
echo === CUP  : %CUP_JAR%
echo.

REM ====== Limpiar generados ======
del /q "%SRC%\Lexer.java" "%SRC%\parser.java" "%SRC%\sym.java" 2>nul
del /q "%SRC%\*.class" 2>nul

REM ====== Generar Lexer ======
echo === Generando Lexer.java (JFlex) ===
pushd "%SRC%"
java -jar "%JFLEX_JAR%" lexer.flex
if errorlevel 1 (
  echo [ERROR] Fallo JFlex.
  popd
  exit /b 1
)

REM ====== Generar Parser (CUP) ======
echo === Generando parser.java y sym.java (CUP) ===
java -jar "%CUP_JAR%" -parser parser -symbols sym parser.cup
if errorlevel 1 (
  echo [ERROR] Fallo CUP.
  popd
  exit /b 1
)

REM ====== Compilar ======
echo === Compilando (javac) ===
javac -cp ".;%CUP_JAR%" *.java
if errorlevel 1 (
  echo [ERROR] Fallo compilacion javac.
  popd
  exit /b 1
)

REM ====== Ejecutar ======
echo === Ejecutando ===
echo Compilacion completa. Para ejecutar use: run.bat Pruebas\entrada_prueba.sql

popd
echo.
echo === OK ===
endlocal