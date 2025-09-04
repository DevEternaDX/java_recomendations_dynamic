# Java Rules Engine con UI

Sistema de motor de reglas desarrollado en Java con Spring Boot y interfaz web en Next.js.

## Requisitos previos

- Java 17 o superior
- Maven 3.8+
- Node.js 18+ y npm

## Instalación rápida

### Windows (Automático)
```bash
# Clonar repositorio
git clone https://github.com/DevEternaDX/java_recomendations_dynamic.git
cd java_recomendations_dynamic

# Setup automático (verifica Java/Maven/Node, instala dependencias y compila)
./setup.bat

# Iniciar todo (API + UI)
.\iniciar-robusto.bat
```

### Manual
```bash
# 1. Iniciar API Java (puerto 8000)
mvn spring-boot:run

# 2. En otra terminal, iniciar UI (puerto 3000)
cd admin-ui
npm install
npm run dev
```

## URLs de acceso

- **Panel de administración**: http://localhost:3000
- **API REST**: http://localhost:8000
- **Base de datos H2**: http://localhost:8000/h2-console

## Funcionalidades

✅ **Gestión de reglas** - Crear, editar, eliminar reglas  
✅ **Importación masiva** - JSON, YAML, CSV  
✅ **Simulación** - Probar reglas con datos de prueba  
✅ **Variables dinámicas** - Sistema de variables configurables  
✅ **Logs y auditoría** - Seguimiento de cambios  

## Scripts disponibles

- `.\iniciar-robusto.bat` - Inicia API + UI de forma robusta
- `.\iniciar.bat` - Inicio simple
- `.\detener.bat` - Detiene todos los servicios
- `.\estado.bat` - Muestra estado de los servicios

## Importación de datos

El sistema soporta tres formatos:

### JSON
```json
[
  {
    "id": "REGLA-001",
    "tenantId": "default",
    "category": "actividad",
    "logic": {"all": [{"var": "steps", "op": ">", "value": 10000}]},
    "messages": [{"text": "¡Excelente actividad!", "weight": 1}]
  }
]
```

### YAML
```yaml
REGLA-001:
  when:
    steps: "> 10000"
  category: actividad
  candidates: ["MSG1", "MSG2"]
```

### CSV
Formato único soportado (rules_ui_format.csv):
- Cabeceras: `id,tenant_id,category,priority,severity,cooldown_days,max_per_day,enabled,tags,logic,locale,messages`
- `logic` y `messages` son strings JSON.
- Importación desde UI usa autodetección: si pegas YAML/JSON va a `/rules/import`, si subes `.csv` va a `/rules/import_csv_reformed`.

Ejemplo `logic` en CSV: `{"all":[{"var":"steps","agg":"current","op":"<","value":2500}]}`
Ejemplo `messages` en CSV: `[{"text":"Texto 1","weight":1,"active":true}]`

## Desarrollo

### Backend (Java)
```bash
# Compilar
mvn clean compile

# Ejecutar tests
mvn test

# Generar JAR
mvn package
```

### Frontend (Next.js)
```bash
cd admin-ui

# Desarrollo
npm run dev

# Build de producción
npm run build
npm start
```

## Estructura del proyecto

```
├── src/main/java/              # Código Java (Spring Boot)
├── admin-ui/                   # Frontend (Next.js)
├── importation_test/           # Archivos de prueba para importar
├── *.bat                       # Scripts de Windows
└── README.md                   # Esta documentación
```

## Documentación detallada

- Guía principal: `docs/README.md`
- Arquitectura: `docs/arquitectura.md`
- Backend (controladores/servicios/entidades): `docs/backend.md`
- Frontend (estructura y cliente API): `docs/frontend.md`
- Referencia de API: `docs/api.md`
- Formatos (JSON/YAML/CSV): `docs/formatos.md`
- Operación y troubleshooting: `docs/operacion.md`

## Troubleshooting

### Error: "npm install falla"
```bash
# Limpiar cache de npm
npm cache clean --force
cd admin-ui
rm -rf node_modules package-lock.json
npm install
```

### Error: "Java no encontrado"
- Instalar Java 17+
- Configurar JAVA_HOME
- Añadir Java al PATH

### Error: "Puerto ocupado"
```bash
# Verificar procesos
.\estado.bat

# Detener servicios
.\detener.bat
```