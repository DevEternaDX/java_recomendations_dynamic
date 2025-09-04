@echo off
title ETERNA DX - Deteniendo...
echo Deteniendo ETERNA DX...
echo.

echo Cerrando ventanas específicas de ETERNA...
taskkill /f /fi "WINDOWTITLE eq ETERNA API*" >nul 2>&1
taskkill /f /fi "WINDOWTITLE eq ETERNA Panel*" >nul 2>&1

echo Cerrando procesos Java...
taskkill /f /im java.exe >nul 2>&1
if %errorlevel% equ 0 (
    echo ✓ Procesos Java detenidos
) else (
    echo ⚠️  No se encontraron procesos Java
)

echo Cerrando procesos Node.js...
taskkill /f /im node.exe >nul 2>&1
if %errorlevel% equ 0 (
    echo ✓ Procesos Node.js detenidos
) else (
    echo ⚠️  No se encontraron procesos Node.js
)

echo.
echo ✓ Todos los servicios han sido detenidos
echo.
timeout /t 3 /nobreak >nul
