# Script simplificado para instalar Maven usando Chocolatey
$ErrorActionPreference = "Stop"

Write-Host "=== Instalador Rápido de Maven (Chocolatey) ===" -ForegroundColor Cyan
Write-Host ""

# Verificar si Maven ya está instalado
try {
    $existingMaven = & mvn -version 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "Maven ya está instalado:" -ForegroundColor Green
        Write-Host $existingMaven
        Write-Host ""
        Write-Host "Puedes ejecutar directamente:" -ForegroundColor Yellow
        Write-Host "  mvn clean compile" -ForegroundColor White
        Write-Host "  mvn spring-boot:run" -ForegroundColor White
        exit 0
    }
} catch {
    # Maven no está instalado, continuar
}

# Verificar Java
Write-Host "Verificando Java..." -ForegroundColor Yellow
try {
    $javaVersion = & java -version 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "Java no encontrado"
    }
    Write-Host "Java encontrado:" -ForegroundColor Green
    Write-Host ($javaVersion | Select-Object -First 1)
} catch {
    Write-Host "ERROR: Java no está instalado" -ForegroundColor Red
    Write-Host ""
    Write-Host "Instalando Java con Chocolatey..." -ForegroundColor Yellow
    
    # Instalar Java también
    try {
        & choco install temurin17 -y
        Write-Host "Java 17 instalado." -ForegroundColor Green
    } catch {
        Write-Host "ERROR: No se pudo instalar Java automáticamente" -ForegroundColor Red
        Write-Host "Por favor instala Java 17+ manualmente desde: https://adoptium.net/temurin/releases/" -ForegroundColor Yellow
        exit 1
    }
}

Write-Host ""

# Verificar si Chocolatey está instalado
Write-Host "Verificando Chocolatey..." -ForegroundColor Yellow
try {
    & choco --version | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "Chocolatey no encontrado"
    }
    Write-Host "Chocolatey encontrado." -ForegroundColor Green
} catch {
    Write-Host "Chocolatey no está instalado. Instalándolo..." -ForegroundColor Yellow
    
    # Instalar Chocolatey
    try {
        Set-ExecutionPolicy Bypass -Scope Process -Force
        [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072
        Invoke-Expression ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))
        
        Write-Host "Chocolatey instalado." -ForegroundColor Green
        
        # Refrescar PATH para la sesión actual
        $env:PATH = [System.Environment]::GetEnvironmentVariable("PATH","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("PATH","User")
        
    } catch {
        Write-Host "ERROR: No se pudo instalar Chocolatey" -ForegroundColor Red
        Write-Host "Por favor instala Maven manualmente desde: https://maven.apache.org/download.cgi" -ForegroundColor Yellow
        exit 1
    }
}

# Instalar Maven
Write-Host ""
Write-Host "Instalando Maven..." -ForegroundColor Yellow
try {
    & choco install maven -y
    Write-Host "Maven instalado exitosamente." -ForegroundColor Green
} catch {
    Write-Host "ERROR: No se pudo instalar Maven" -ForegroundColor Red
    exit 1
}

# Refrescar PATH
Write-Host "Actualizando PATH..." -ForegroundColor Yellow
$env:PATH = [System.Environment]::GetEnvironmentVariable("PATH","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("PATH","User")

# Verificar instalación
Write-Host ""
Write-Host "Verificando instalación..." -ForegroundColor Yellow
try {
    Start-Sleep -Seconds 2  # Dar tiempo para que se actualice el PATH
    $verification = & mvn -version 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "¡Verificación exitosa!" -ForegroundColor Green
        Write-Host $verification
    } else {
        throw "Verificación falló"
    }
} catch {
    Write-Host "La verificación falló en esta sesión." -ForegroundColor Yellow
    Write-Host "Por favor abre una nueva ventana de PowerShell y ejecuta: mvn -version" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "=== Instalación Completada ===" -ForegroundColor Green
Write-Host ""
Write-Host "Ahora puedes ejecutar tu aplicación Java:" -ForegroundColor Yellow
Write-Host "  mvn clean compile" -ForegroundColor White
Write-Host "  mvn spring-boot:run" -ForegroundColor White
Write-Host ""
Write-Host "O usar el script de arranque:" -ForegroundColor Yellow
Write-Host "  .\run.bat" -ForegroundColor White
Write-Host ""

Write-Host "¡Maven instalado exitosamente con Chocolatey!" -ForegroundColor Green

