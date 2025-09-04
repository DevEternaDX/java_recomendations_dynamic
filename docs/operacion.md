## Operación, Configuración y Troubleshooting

### Requisitos

- Java 17+, Maven 3.8+, Node.js 18+.

### Inicio rápido

```powershell
./setup.bat
./iniciar-robusto.bat
```

UI: http://localhost:3000  
API: http://localhost:8000

### Configuración

- `src/main/resources/application.properties`:
  - `spring.servlet.multipart.max-file-size=10MB`
  - `spring.servlet.multipart.max-request-size=10MB`
- `AppProperties` (prefijo `app.`): `env`, `defaultLocale`, `maxRecsPerDay`, `maxRecsPerCategoryPerDay`, `antiRepeatDays`, `authEnabled`.
- `DataProperties` (prefijo `data.`): rutas de CSVs (`dailyCsvPath`, `sleepCsvPath`, `processedCsvPath`).

### Importación masiva

- JSON/YAML: pegar en UI o `POST /rules/import?format=json|yaml`.
- CSV: `rules_ui_format.csv` a `/rules/import_csv_reformed`.

### Despliegue (Docker frontend)

`admin-ui/Dockerfile` incluido para servir Next.js en producción.

### Troubleshooting

- Error 500 al importar YAML:
  - Ver consola de API. El backend admite lista o raíz por ID; validar formato.
- Unsupported Media Type en CSV:
  - Enviar `Content-Type: text/csv`.
- No aparecen reglas tras importar:
  - Revisar `tenantId` (por defecto `default`) y filtros en UI.
- UI llama endpoints inexistentes (/rules/{id}/stats):
  - Ya desactivados en UI; verificar que se refrescó el panel.
- Panel no arranca / CORS:
  - `app.env != production` habilita CORS abierto en `WebConfig`.


