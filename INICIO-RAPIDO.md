# 🚀 INICIO RÁPIDO - Java Rules Engine

## ⚡ Instalación Automática (Recomendado)

### Opción 1: Script Automático con Chocolatey
```powershell
# Ejecutar como Administrador
.\install-chocolatey-maven.ps1
```

### Opción 2: Instalación Manual Rápida

#### Windows con Chocolatey
```powershell
# Instalar Chocolatey (si no lo tienes)
Set-ExecutionPolicy Bypass -Scope Process -Force; [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072; iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))

# Instalar Java y Maven
choco install temurin17 maven -y
```

#### Windows con Winget
```cmd
winget install EclipseAdoptium.Temurin.17.JDK
winget install Apache.Maven
```

## 🏃‍♂️ Ejecución

Una vez instalado Maven:

```bash
# Compilar
mvn clean compile

# Ejecutar
mvn spring-boot:run
```

O usar el script:
```cmd
run.bat
```

## ✅ Verificación

La aplicación estará disponible en: **http://localhost:8000**

Probar endpoints:
```bash
curl http://localhost:8000/health
curl http://localhost:8000/variables
curl http://localhost:8000/rules
```

## 🔧 Solución de Problemas

### "mvn no se reconoce como comando"
- Ejecutar: `.\install-chocolatey-maven.ps1`
- O instalar manualmente desde: https://maven.apache.org/download.cgi

### "java no se reconoce como comando"  
- Instalar Java 17: https://adoptium.net/temurin/releases/
- O usar: `choco install temurin17`

### Puerto 8000 ocupado
Editar `src/main/resources/application.properties`:
```properties
server.port=8080
```

## 📁 Estructura del Proyecto

```
java_def_recomendations/
├── src/main/java/           # Código Java
├── src/main/resources/      # Configuración y seeds
├── data/                    # Archivos CSV (opcional)
├── pom.xml                  # Configuración Maven
├── run.bat                  # Script de arranque
└── README.md               # Documentación completa
```

## 🎯 Siguiente Paso

Una vez que la aplicación esté ejecutándose:

1. **Frontend**: Cambiar la URL de la API en tu frontend Next.js a `http://localhost:8000`
2. **Datos**: Colocar archivos CSV en la carpeta `data/`
3. **Testing**: Usar los endpoints para probar funcionalidad

## 📖 Documentación Completa

Ver `README.md` para documentación detallada de la migración y todas las funcionalidades.

---

**¡La migración a Java está COMPLETA! 🎉**

Solo necesitas instalar Maven y ejecutar la aplicación.
