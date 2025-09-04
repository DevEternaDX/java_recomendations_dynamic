## Frontend - Next.js (admin-ui)

### Estructura

- `app/layout.tsx` y `app/globals.css`: layout global y estilos.
- `app/page.tsx`: dashboard/simple landing.
- `app/rules/page.tsx`: listado y acciones (importar/exportar/eliminar todas).
- `app/rules/new/page.tsx`: creación de regla.
- `app/rules/[id]/page.tsx`: edición de regla (mensajes, condiciones, meta).
- `app/simulate/page.tsx`: simulación de reglas por usuario/fechas.
- `app/variables/page.tsx`: listado y upsert de variables.
- `app/logs/page.tsx`: consulta de logs (analytics).
- `lib/api.ts`: cliente Axios para la API del backend.

### Cliente API (`lib/api.ts`)

- Base URL configurable con `NEXT_PUBLIC_API_BASE` (defecto `http://localhost:8000`).
- Reglas: `listRules`, `getRule`, `createRule`, `updateRule`, `enableRule`, `cloneRule`, `deleteRule`, `deleteAllRules`.
- Importación:
  - `importRules(payload, format)` → `/rules/import` (JSON o YAML).
  - `importRulesCsv(csvText)` → `/rules/import_csv_reformed` (único CSV soportado).
- Exportación: `exportRules(format)`.
- Analítica: `getTriggersSeries`, `getLogs`.
- Variables: `listVariables`, `upsertVariable`.
- Simulación: `simulate`, `simulateRange`.

### Consideraciones de UI

- Los formularios usan camelCase (p. ej., `cooldownDays`, `maxPerDay`).
- Los mensajes se editan como `messages.candidates[]` en el editor, el backend los adapta.
- Importación autodetecta JSON/YAML/CSV al pegar contenido desde UI.


