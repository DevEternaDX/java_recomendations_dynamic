## Arquitectura General

El sistema se compone de:

- Backend Java (Spring Boot) bajo `src/main/java/com/eterna/dx/rulesengine`.
- Frontend Next.js bajo `admin-ui/`.
- Base de datos H2 embebida por defecto (en memoria o fichero).
- Scripts de automatización Windows para iniciar/detener y diagnosticar.

### Módulos Backend

- `controller/`: Endpoints REST (reglas, variables, analytics, simulación, health).
- `service/`: Lógica de negocio (motor de reglas, mensajes, inicialización, features).
- `entity/`: Entidades JPA (Rule, RuleMessage, Audit, ChangeLog, Variable).
- `repository/`: Repositorios Spring Data JPA.
- `dsl/`: Árbol sintáctico y evaluador de lógica de reglas (All/Any/None/Numeric/Relative).
- `config/`: Configuración de CORS, propiedades de aplicación y rutas de datos/seeds.

### Flujo de Evaluación

1. El frontend envía `POST /simulate` con `userId`, `date`, `tenantId`, `debug`.
2. `EvaluationController` delega en `RulesEngineService.evaluateUser`.
3. `FeatureService` carga y construye features desde CSVs (`data/`).
4. Se consultan reglas activas por tenant y se evalúan contra las features usando `DSLParser`.
5. Si una regla se dispara, `MessageService` selecciona y renderiza el mensaje.
6. Se registra auditoría en `Audit` (why/values) y se devuelven `RecommendationEvent`.
7. Se aplican cooldowns y límites por categoría/día.

### Persistencia

- `Rule` serializa `tags` y `logic` como JSON en columnas `tags` y `logic` (LOB).
- `RuleMessage` almacena variantes de mensajes con peso/activo/locale.
- `Audit` registra cada evaluación (user/date/rule/fired/messageId) con `why` y `values` en JSON.
- `ChangeLog` registra acciones de mantenimiento (extensible).
- `Variable` define metadatos de features (agregadores permitidos, rangos, etc.).

### Importación/Exportación

- JSON/YAML: `POST /rules/import?format=json|yaml` acepta `List<RuleRequest>` o YAML de raíz por id.
- CSV unificado: `POST /rules/import_csv_reformed` con columnas horizontales (10 variantes por regla).
- Exportación: `GET /rules/export?format=json|yaml`.

### Frontend

- App de Next.js con páginas en `admin-ui/app/*` y cliente en `admin-ui/lib/api.ts`.
- Operaciones de reglas: listar, crear, editar, clonar, habilitar, eliminar, importar/exportar.
- Simulación de reglas y vista de variables y logs.


