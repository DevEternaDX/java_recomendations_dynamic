@echo off
echo Iniciando aplicacion Java Rules Engine...
echo.

REM Verificar si Maven esta instalado
mvn -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Maven no esta instalado o no esta en el PATH
    echo Por favor instala Maven desde: https://maven.apache.org/download.cgi
    echo Y agrega el directorio bin de Maven al PATH del sistema
    pause
    exit /b 1
)

REM Compilar y ejecutar
echo Compilando proyecto...
mvn clean compile
if %errorlevel% neq 0 (
    echo ERROR: Fallo la compilacion
    pause
    exit /b 1
)

echo.
echo Ejecutando aplicacion...
mvn spring-boot:run

pause
