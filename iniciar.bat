@echo off
title ETERNA DX - Iniciando...
echo Iniciando ETERNA DX Rules Engine...
echo.

REM Configurar Maven y Java
set MAVEN_HOME=%USERPROFILE%\tools\apache-maven-3.9.9
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.8.9-hotspot
set PATH=%MAVEN_HOME%\bin;%JAVA_HOME%\bin;%PATH%

REM Iniciar API Java en segundo plano (con keep-alive)
echo [1/2] Iniciando API Java...
start /min "ETERNA API" cmd /k "set MAVEN_HOME=%MAVEN_HOME%&& set JAVA_HOME=%JAVA_HOME%&& set PATH=%MAVEN_HOME%\bin;%JAVA_HOME%\bin;%PATH%&& mvn spring-boot:run"

REM Esperar más tiempo para que la API inicie completamente
echo Esperando que la API inicie completamente...
timeout /t 20 /nobreak >nul

REM Iniciar Panel Web en segundo plano (con keep-alive)
echo [2/2] Iniciando Panel Web...
cd admin-ui
start /min "ETERNA Panel" cmd /k "npm run dev"
cd ..

echo.
echo ✓ Servicios iniciados en ventanas minimizadas
echo.
echo 🌐 Panel Web: http://localhost:3000
echo 🔧 API Java:  http://localhost:8000
echo.
timeout /t 3 /nobreak >nul
start http://localhost:3000
echo Panel web abierto en el navegador.
echo.
echo Presiona cualquier tecla para cerrar esta ventana...
pause >nul
