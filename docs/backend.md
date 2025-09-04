## Backend - Controladores, Servicios, Entidades

### Controladores

- `RuleController` (`/rules`)
  - `GET /rules` lista con filtros `tenantId`, `enabled`, `category`.
  - `GET /rules/{id}` devuelve DTO con `messages` formateados para UI.
  - `POST /rules` crea una regla desde `RuleRequest` con sus mensajes.
  - `PUT /rules/{id}` actualiza campos parciales (`RuleUpdateRequest`).
  - `POST /rules/{id}/enable` habilita/deshabilita.
  - `POST /rules/{id}/clone` clona incluyendo mensajes.
  - `DELETE /rules/{id}` elimina.
  - `DELETE /rules/all` borra todas (testing).
  - `GET /rules/export?format=json|yaml` exporta reglas del tenant.
  - `POST /rules/import?format=json|yaml` importa JSON/YAML (incluye fallback YAML raíz por id).
  - `POST /rules/import_csv_reformed` importa CSV horizontal (10 variantes -> mensajes).

- `EvaluationController`
  - `POST /simulate` evalúa usuario/fecha y devuelve `SimulationResult` con eventos.
  - `GET /features` retorna features calculadas (debug).

- `AnalyticsController` (`/analytics`)
  - `GET /triggers` series temporales de triggers por regla.
  - `GET /logs` consulta de changelog paginada; `GET /logs/download` descarga últimos N o rango.

- `VariableController` (`/variables`)
  - `GET /variables` lista por `tenantId`.
  - `POST /variables` upsert con `VariableRequest`.

- `HealthController` (`/health`)
  - `GET /health` estado simple.

### Servicios

- `RulesEngineService`
  - Orquesta evaluación: carga features, parsea lógica (DSL), selecciona mensaje, registra `Audit`, aplica cooldowns y límites.
  - `evaluateUser(userId, date, tenantId, debug)` → `SimulationResult`.

- `MessageService`
  - Anti-repetición (configurable `app.antiRepeatDays`).
  - Selección ponderada por `weight`.
  - Render de placeholders `{{ variable:agg:format }}` con valores de features.

- `FeatureService`
  - Carga CSV procesado si existe; si no, combina diarios y sueño.
  - Normaliza columnas (`COLUMN_MAPPING`), excluye campos no numéricos y calcula estadísticas (rolling mean/median/zscore, deltas) y derivadas (`max_hr_pct_user_max`).

- `InitializationService`
  - Seed de variables (`resources/seeds/variables_seed.json`) y reglas (`resources/seeds/rules_seed.json`) si DB vacía.
  - Genera variables automáticamente a partir de cabeceras de CSV en `data/`.

### Entidades clave

- `Rule`
  - Campos principales: `id`, `enabled`, `tenantId`, `category`, `priority`, `severity`, `cooldownDays`, `maxPerDay`, `locale`.
  - `tags` y `logic` almacenados como JSON (helpers `getTags()/setTags()`, `getLogic()/setLogic()`).
  - Relación `@OneToMany` con `RuleMessage`.
  - `getMessagesForFrontend()` devuelve `{ locale, candidates[] }` para UI.

- `RuleMessage`
  - `text`, `weight`, `active`, `locale` y referencia a `Rule`.

- `Audit`
  - Registra cada evaluación: `userId`, `date`, `tenantId`, `ruleId`, `fired`, `messageId`, `why`, `values`.

- `ChangeLog`
  - Auditoría de cambios (entidad/acción/before/after).

- `Variable`
  - Metadatos de variables (tipo/unidad/rangos/aggregators/ejemplos) por `tenantId`.

### Repositorios

- `RuleRepository`: consultas por tenant, enabled, categoría; `findRulesWithFilters` y ordenación.
- `RuleMessageRepository`, `AuditRepository`, `ChangeLogRepository`, `VariableRepository`: CRUD y consultas específicas.

### DSL de Reglas

- `dsl/DSLParser` transforma `logic` en árbol de nodos (`Node`):
  - Combinadores: `AllCondition` (AND), `AnyCondition` (OR), `NoneCondition` (NOT).
  - Condiciones: `NumericCondition` (>, >=, <, <=, ==), `RelativeCondition` (comparativas como deltas/zscore).


