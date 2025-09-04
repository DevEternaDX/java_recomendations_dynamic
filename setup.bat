@echo off
echo ========================================
echo  SETUP AUTOMATICO - ETERNA DX
echo ========================================

echo [1/3] Verificando dependencias...

:: Verificar Java
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Java no encontrado. Instala Java 17+ y configurar JAVA_HOME
    pause
    exit /b 1
)
echo ✅ Java encontrado

:: Verificar Maven
call mvn -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Maven no encontrado. Ejecuta install-maven.ps1 primero
    pause
    exit /b 1
)
echo ✅ Maven encontrado

:: Verificar Node.js
node --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Node.js no encontrado. Instala Node.js 18+ desde nodejs.org
    pause
    exit /b 1
)
echo ✅ Node.js encontrado

echo.
echo [2/3] Instalando dependencias del frontend...
cd admin-ui
call npm install
if %errorlevel% neq 0 (
    echo ❌ Error instalando dependencias de Node.js
    pause
    exit /b 1
)
echo ✅ Dependencias instaladas
cd ..

echo.
echo [3/3] Compilando backend Java...
call mvn clean compile -q
if %errorlevel% neq 0 (
    echo ❌ Error compilando Java
    pause
    exit /b 1
)
echo ✅ Backend compilado

echo.
echo ========================================
echo  ✅ SETUP COMPLETADO
echo ========================================
echo.
echo Para iniciar el sistema:
echo   .\iniciar-robusto.bat
echo.
echo URLs:
echo   Panel: http://localhost:3000
echo   API:   http://localhost:8000
echo.
pause
