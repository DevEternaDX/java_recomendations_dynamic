Write-Host "========================================" -ForegroundColor Cyan
Write-Host " ETERNA DX - Iniciando Servicios" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Configurar variables de entorno
$env:MAVEN_HOME = "$env:USERPROFILE\tools\apache-maven-3.9.9"
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.8.9-hotspot"
$env:PATH = "$env:MAVEN_HOME\bin;$env:JAVA_HOME\bin;$env:PATH"

Write-Host "[1/2] Iniciando API Java..." -ForegroundColor Green

# Crear script temporal para API Java
$apiScript = @"
cd '$PWD'
`$env:MAVEN_HOME = '$env:MAVEN_HOME'
`$env:JAVA_HOME = '$env:JAVA_HOME'
`$env:PATH = '$env:MAVEN_HOME\bin;$env:JAVA_HOME\bin;$env:PATH'
Write-Host 'Iniciando API Java en puerto 8000...' -ForegroundColor Yellow
mvn spring-boot:run
Read-Host 'API detenida. Presiona Enter para cerrar'
"@

$apiScriptPath = "$env:TEMP\eterna-api.ps1"
$apiScript | Out-File -FilePath $apiScriptPath -Encoding UTF8

# Iniciar API Java
$apiProcess = Start-Process powershell -ArgumentList "-ExecutionPolicy Bypass", "-File", $apiScriptPath -WindowStyle Minimized -PassThru

Write-Host "API Java iniciándose... (PID: $($apiProcess.Id))" -ForegroundColor Yellow
Write-Host "Esperando 25 segundos para que la API inicie completamente..." -ForegroundColor Yellow
Start-Sleep -Seconds 25

Write-Host "[2/2] Iniciando Panel Web..." -ForegroundColor Green

# Crear script temporal para Panel Web
$panelScript = @"
cd '$PWD\admin-ui'
Write-Host 'Iniciando Panel Web en puerto 3000...' -ForegroundColor Yellow
npm run dev
Read-Host 'Panel detenido. Presiona Enter para cerrar'
"@

$panelScriptPath = "$env:TEMP\eterna-panel.ps1"
$panelScript | Out-File -FilePath $panelScriptPath -Encoding UTF8

# Iniciar Panel Web
$panelProcess = Start-Process powershell -ArgumentList "-ExecutionPolicy Bypass", "-File", $panelScriptPath -WindowStyle Minimized -PassThru

Write-Host "Panel Web iniciándose... (PID: $($panelProcess.Id))" -ForegroundColor Yellow
Start-Sleep -Seconds 5

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host " 🎉 SERVICIOS INICIADOS" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "🌐 Panel Web:     http://localhost:3000" -ForegroundColor White
Write-Host "🔧 API Java:      http://localhost:8000" -ForegroundColor White
Write-Host "🗃️  Base de datos: http://localhost:8000/h2-console" -ForegroundColor White
Write-Host ""
Write-Host "📋 Procesos iniciados:" -ForegroundColor Yellow
Write-Host "   - API Java (PID: $($apiProcess.Id))" -ForegroundColor White
Write-Host "   - Panel Web (PID: $($panelProcess.Id))" -ForegroundColor White
Write-Host ""
Write-Host "⚠️  Los servicios están corriendo en ventanas minimizadas" -ForegroundColor Yellow
Write-Host "   Para detenerlos, usa 'detener.bat' o cierra las ventanas" -ForegroundColor Yellow
Write-Host ""

# Abrir el panel web
$response = Read-Host "¿Deseas abrir el panel web ahora? (S/N)"
if ($response -eq "S" -or $response -eq "s") {
    Start-Process "http://localhost:3000"
}

Write-Host ""
Write-Host "Los servicios siguen corriendo en segundo plano." -ForegroundColor Green
Write-Host "Puedes cerrar esta ventana con seguridad." -ForegroundColor Green
Read-Host "Presiona Enter para continuar"
