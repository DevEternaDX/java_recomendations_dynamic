@echo off
title ETERNA DX - Iniciando Servicios
echo Ejecutando script robusto de PowerShell...
echo.
powershell -ExecutionPolicy Bypass -File "%~dp0iniciar-robusto.ps1"
