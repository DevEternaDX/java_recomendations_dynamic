@echo off
echo ========================================
echo  ESTADO DE SERVICIOS ETERNA DX
echo ========================================
echo.

echo Verificando API Java (puerto 8000)...
netstat -ano | findstr :8000 >nul 2>&1
if %errorlevel% equ 0 (
    echo ✓ API Java corriendo en puerto 8000
    for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8000') do (
        echo   PID: %%a
    )
) else (
    echo ✗ API Java no esta corriendo
)

echo.
echo Verificando Panel Web (puerto 3000)...
netstat -ano | findstr :3000 >nul 2>&1
if %errorlevel% equ 0 (
    echo ✓ Panel Web corriendo en puerto 3000
    for /f "tokens=5" %%a in ('netstat -ano ^| findstr :3000') do (
        echo   PID: %%a
    )
) else (
    echo ✗ Panel Web no esta corriendo
)

echo.
echo Procesos Java activos:
tasklist /fi "imagename eq java.exe" 2>nul | findstr java.exe
if %errorlevel% neq 0 echo   Ninguno

echo.
echo Procesos Node.js activos:
tasklist /fi "imagename eq node.exe" 2>nul | findstr node.exe
if %errorlevel% neq 0 echo   Ninguno

echo.
echo ========================================
echo  ENLACES RÁPIDOS
echo ========================================
echo.
echo 🌐 Panel Web:     http://localhost:3000
echo 🔧 API Java:      http://localhost:8000
echo 🗃️  Base de datos: http://localhost:8000/h2-console
echo.
pause
