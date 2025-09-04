@echo off
title ETERNA DX - Estado
echo ========================================
echo  ESTADO DE SERVICIOS
echo ========================================
echo.

echo Verificando puertos...
netstat -ano | findstr :8000 >nul 2>&1
if %errorlevel% equ 0 (
    echo ✓ API Java corriendo (puerto 8000)
) else (
    echo ✗ API Java no corriendo
)

netstat -ano | findstr :3000 >nul 2>&1
if %errorlevel% equ 0 (
    echo ✓ Panel Web corriendo (puerto 3000)
) else (
    echo ✗ Panel Web no corriendo
)

echo.
echo Enlaces:
echo 🌐 Panel Web: http://localhost:3000
echo 🔧 API Java:  http://localhost:8000
echo.
pause
