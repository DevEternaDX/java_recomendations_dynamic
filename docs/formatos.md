## Formatos de Importación/Exportación

### JSON (UI → Backend)

Lista de `RuleRequest`:
```json
[
  {
    "id": "R-ACT-STEPS-LOWER-SEVERITY1",
    "tenantId": "default",
    "category": "actividad",
    "priority": 50,
    "severity": 1,
    "cooldownDays": 0,
    "maxPerDay": 0,
    "tags": [],
    "logic": {"all": [{"var":"steps","agg":"current","op":"<","value":2500}]},
    "locale": "es-ES",
    "messages": [{"text":"Texto 1","weight":1,"active":true}]
  }
]
```

### YAML (flexible)

1) Lista de reglas (equivalente a JSON):
```yaml
- id: R-EX
  tenant_id: default
  category: actividad
  logic: { all: [ { var: steps, agg: current, op: ">", value: 10000 } ] }
  messages:
    - text: "Excelente"
      weight: 1
      active: true
```

2) Raíz por ID (formato `rules_basic.yaml`):
```yaml
R-EX:
  when:
    steps: "> 10000"
  category: actividad
  candidates: ["Texto A", "Texto B"]
```
El backend convertirá `when` → `logic` y `candidates` → `messages`.

### CSV (único soportado)

`rules_ui_format.csv` con columnas:

- `id, tenant_id, category, priority, severity, cooldown_days, max_per_day, enabled, tags, logic, locale, messages`
- `logic` y `messages` como JSON strings, p. ej.:
  - logic: `{"all":[{"var":"steps","agg":"current","op":"<","value":2500}]}`
  - messages: `[{"text":"Texto 1","weight":1,"active":true}]`


