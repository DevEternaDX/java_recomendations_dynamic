## Referencia de API

Base: `http://localhost:8000`

### Reglas (`/rules`)

- `GET /rules?tenantId=default&enabled=true&category=actividad`
  - Respuesta: `Rule[]`

- `GET /rules/{id}`
  - Respuesta: DTO con `messages: { locale, candidates: [{ id, text, weight, active }] }`.

- `POST /rules`
  - Body: `RuleRequest`
  - Respuesta: `{ id }`

- `PUT /rules/{id}`
  - Body: `RuleUpdateRequest`
  - Respuesta: `{ id }`

- `POST /rules/{id}/enable` → `{ enabled: boolean }`
- `POST /rules/{id}/clone` → `{ id: string }`
- `DELETE /rules/{id}` → `{ id, deleted: true }`
- `DELETE /rules/all` → `{ deleted, deleted_messages, message }`

- `GET /rules/export?format=json|yaml` → `Rule[]`
- `POST /rules/import?format=json|yaml`
  - Acepta:
    - `List<RuleRequest>` (JSON o YAML)
    - YAML raíz por ID (formato `rules_basic.yaml`):
      ```yaml
      R-EXAMPLE:
        when: { steps: "> 10000" }
        category: actividad
        candidates: ["Texto 1", "Texto 2"]
      ```

- `POST /rules/import_csv_reformed` (CSV único soportado)
  - Cabeceras: `id,tenant_id,category,priority,severity,cooldown_days,max_per_day,enabled,tags,logic,locale,messages`
  - `logic` y `messages` son JSON strings.

### Simulación

- `POST /simulate`
  - Body:
    ```json
    { "userId":"u1", "date":"2024-05-10", "tenantId":"default", "debug":true }
    ```
  - Respuesta: `SimulationResult { events: RecommendationEvent[], debug?: any }`

### Variables (`/variables`)

- `GET /variables?tenantId=default` → `Variable[]`
- `POST /variables` (upsert) → `{ key }`

### Analytics (`/analytics`)

- `GET /analytics/triggers?start=YYYY-MM-DD&end=YYYY-MM-DD&tenant_id=default&rule_ids=R1,R2`
  - Respuesta: `TriggerAnalytics { series: [{ ruleId, points: [{ date, count }] }] }`

- `GET /analytics/logs?...` → `Page<ChangeLog>`
- `GET /analytics/logs/download?start=...&end=...` → `ChangeLog[]`

## Contratos de Datos (DTOs)

- `RuleRequest`
  - `id, version, enabled, tenantId, category, priority, severity, cooldownDays, maxPerDay, tags[], logic{}, locale, messages[]`
- `RuleUpdateRequest`
  - Parcial de los campos anteriores; `messages[]` reemplaza todas las variantes.
- `MessageRequest`
  - `text, weight, active, locale`
- `RecommendationEvent`
  - `date, tenantId, userId, ruleId, ruleName, category, severity, priority, messageId, messageText, locale, why[]`
- `VariableRequest`
  - `key, label, description, unit, type, allowedAggregators[], validMin, validMax, missingPolicy, decimals, category, tenantId, examples{}`


