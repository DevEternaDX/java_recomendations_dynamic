# Script para instalar Maven en Windows
param(
    [string]$InstallPath = "C:\Program Files\Apache\maven"
)

$ErrorActionPreference = "Stop"

Write-Host "=== Instalador de Apache Maven ===" -ForegroundColor Cyan
Write-Host ""

# Verificar si Maven ya está instalado
try {
    $existingMaven = & mvn -version 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "Maven ya está instalado:" -ForegroundColor Green
        Write-Host $existingMaven
        Write-Host ""
        $continue = Read-Host "¿Deseas reinstalar Maven? (y/N)"
        if ($continue -notmatch '^[Yy]') {
            Write-Host "Instalación cancelada." -ForegroundColor Yellow
            exit 0
        }
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
    Write-Host "ERROR: Java no está instalado o no está en el PATH" -ForegroundColor Red
    Write-Host "Por favor instala Java 17+ desde: https://adoptium.net/temurin/releases/" -ForegroundColor Yellow
    exit 1
}

Write-Host ""

# Descargar Maven
$mavenVersion = "3.9.6"
$downloadUrl = "https://archive.apache.org/dist/maven/maven-3/$mavenVersion/binaries/apache-maven-$mavenVersion-bin.zip"
$tempFile = "$env:TEMP\apache-maven-$mavenVersion-bin.zip"

Write-Host "Descargando Maven $mavenVersion..." -ForegroundColor Yellow
try {
    Invoke-WebRequest -Uri $downloadUrl -OutFile $tempFile -UseBasicParsing
    Write-Host "Descarga completada." -ForegroundColor Green
} catch {
    Write-Host "ERROR: No se pudo descargar Maven" -ForegroundColor Red
    Write-Host "URL: $downloadUrl" -ForegroundColor Yellow
    exit 1
}

# Crear directorio de instalación
Write-Host "Creando directorio de instalación: $InstallPath" -ForegroundColor Yellow
try {
    if (Test-Path $InstallPath) {
        Remove-Item $InstallPath -Recurse -Force
    }
    New-Item -ItemType Directory -Path $InstallPath -Force | Out-Null
} catch {
    Write-Host "ERROR: No se pudo crear el directorio $InstallPath" -ForegroundColor Red
    Write-Host "Ejecuta este script como Administrador" -ForegroundColor Yellow
    exit 1
}

# Extraer Maven
Write-Host "Extrayendo Maven..." -ForegroundColor Yellow
try {
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    [System.IO.Compression.ZipFile]::ExtractToDirectory($tempFile, $InstallPath)
    
    # Mover archivos de la subcarpeta al directorio principal
    $extractedFolder = Get-ChildItem $InstallPath | Where-Object { $_.PSIsContainer } | Select-Object -First 1
    if ($extractedFolder) {
        $sourcePath = $extractedFolder.FullName
        Get-ChildItem $sourcePath | Move-Item -Destination $InstallPath
        Remove-Item $sourcePath -Recurse -Force
    }
    
    Write-Host "Extracción completada." -ForegroundColor Green
} catch {
    Write-Host "ERROR: No se pudo extraer Maven" -ForegroundColor Red
    exit 1
}

# Limpiar archivo temporal
Remove-Item $tempFile -Force

# Configurar variables de entorno
Write-Host "Configurando variables de entorno..." -ForegroundColor Yellow
try {
    # MAVEN_HOME
    [Environment]::SetEnvironmentVariable("MAVEN_HOME", $InstallPath, "Machine")
    
    # PATH
    $currentPath = [Environment]::GetEnvironmentVariable("PATH", "Machine")
    $mavenBin = "$InstallPath\bin"
    
    if ($currentPath -notlike "*$mavenBin*") {
        $newPath = "$currentPath;$mavenBin"
        [Environment]::SetEnvironmentVariable("PATH", $newPath, "Machine")
    }
    
    Write-Host "Variables de entorno configuradas." -ForegroundColor Green
} catch {
    Write-Host "ERROR: No se pudieron configurar las variables de entorno" -ForegroundColor Red
    Write-Host "Configúralas manualmente:" -ForegroundColor Yellow
    Write-Host "  MAVEN_HOME = $InstallPath" -ForegroundColor White
    Write-Host "  PATH += $InstallPath\bin" -ForegroundColor White
}

Write-Host ""
Write-Host "=== Instalación Completada ===" -ForegroundColor Green
Write-Host ""
Write-Host "Para que los cambios surtan efecto:" -ForegroundColor Yellow
Write-Host "1. Cierra esta ventana de PowerShell" -ForegroundColor White
Write-Host "2. Abre una nueva ventana de PowerShell o Command Prompt" -ForegroundColor White
Write-Host "3. Ejecuta: mvn -version" -ForegroundColor White
Write-Host ""
Write-Host "Después puedes ejecutar tu aplicación Java con:" -ForegroundColor Yellow
Write-Host "  mvn clean compile" -ForegroundColor White
Write-Host "  mvn spring-boot:run" -ForegroundColor White
Write-Host ""

# Verificar instalación (puede no funcionar en la sesión actual)
Write-Host "Intentando verificar la instalación..." -ForegroundColor Yellow
try {
    # Actualizar PATH para la sesión actual
    $env:PATH = "$env:PATH;$InstallPath\bin"
    $env:MAVEN_HOME = $InstallPath
    
    $verification = & "$InstallPath\bin\mvn.cmd" -version 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "¡Verificación exitosa!" -ForegroundColor Green
        Write-Host $verification
    } else {
        Write-Host "La verificación falló, pero Maven debería funcionar en una nueva sesión." -ForegroundColor Yellow
    }
} catch {
    Write-Host "La verificación falló, pero Maven debería funcionar en una nueva sesión." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "¡Maven instalado exitosamente!" -ForegroundColor Green
