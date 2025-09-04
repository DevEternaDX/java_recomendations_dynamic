package com.eterna.dx.rulesengine.controller;

import com.eterna.dx.rulesengine.dto.request.MessageRequest;
import com.eterna.dx.rulesengine.dto.request.RuleRequest;
import com.eterna.dx.rulesengine.dto.request.RuleUpdateRequest;
import com.eterna.dx.rulesengine.entity.Rule;
import com.eterna.dx.rulesengine.entity.RuleMessage;
import com.eterna.dx.rulesengine.repository.RuleMessageRepository;
import com.eterna.dx.rulesengine.repository.RuleRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.StringReader;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Controlador para gestión de reglas.
 * Equivalente a rules.py en FastAPI.
 */
@RestController
@RequestMapping("/rules")
@RequiredArgsConstructor
@Slf4j
public class RuleController {

    private final RuleRepository ruleRepository;
    private final RuleMessageRepository ruleMessageRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory())
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    /**
     * Lista todas las reglas con filtros opcionales.
     * GET /rules
     */
    @GetMapping
    public List<Rule> listRules(
            @RequestParam(defaultValue = "default") String tenantId,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) String category) {
        
        return ruleRepository.findRulesWithFilters(tenantId, enabled, category);
    }

    /**
     * Obtiene una regla específica por ID.
     * GET /rules/{rule_id}
     */
    @GetMapping("/{ruleId}")
    public Map<String, Object> getRule(@PathVariable String ruleId) {
        Rule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                        "Regla no encontrada: " + ruleId));
        
        // Convertir a formato esperado por el frontend
        Map<String, Object> ruleDto = new HashMap<>();
        ruleDto.put("id", rule.getId());
        ruleDto.put("version", rule.getVersion());
        ruleDto.put("enabled", rule.getEnabled());
        ruleDto.put("tenantId", rule.getTenantId());
        ruleDto.put("category", rule.getCategory());
        ruleDto.put("priority", rule.getPriority());
        ruleDto.put("severity", rule.getSeverity());
        ruleDto.put("cooldownDays", rule.getCooldownDays());
        ruleDto.put("maxPerDay", rule.getMaxPerDay());
        ruleDto.put("tags", rule.getTags());
        ruleDto.put("logic", rule.getLogic());
        ruleDto.put("locale", rule.getLocale());
        ruleDto.put("createdAt", rule.getCreatedAt());
        ruleDto.put("updatedAt", rule.getUpdatedAt());
        
        // Formatear mensajes para el frontend
        ruleDto.put("messages", rule.getMessagesForFrontend());
        
        return ruleDto;
    }

    /**
     * Crea una nueva regla.
     * POST /rules
     */
    @PostMapping
    @Transactional
    public ResponseEntity<Map<String, String>> createRule(@Valid @RequestBody RuleRequest request) {
        try {
            // Verificar que el ID no exista
            if (ruleRepository.existsById(request.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, 
                        "Ya existe una regla con ID: " + request.getId());
            }

            // Crear entidad Rule
            Rule rule = Rule.builder()
                    .id(request.getId())
                    .version(request.getVersion())
                    .enabled(request.getEnabled())
                    .tenantId(request.getTenantId())
                    .category(request.getCategory())
                    .priority(request.getPriority())
                    .severity(request.getSeverity())
                    .cooldownDays(request.getCooldownDays())
                    .maxPerDay(request.getMaxPerDay())
                    .locale(request.getLocale())
                    .createdBy(request.getCreatedBy())
                    .updatedBy(request.getUpdatedBy())
                    .build();

            rule.setTags(request.getTags());
            rule.setLogic(request.getLogic());

            // Guardar regla
            rule = ruleRepository.save(rule);

            // Crear mensajes
            for (MessageRequest msgReq : request.getMessages()) {
                RuleMessage message = RuleMessage.builder()
                        .rule(rule)
                        .text(msgReq.getText())
                        .weight(msgReq.getWeight())
                        .active(msgReq.getActive())
                        .locale(msgReq.getLocale() != null ? msgReq.getLocale() : rule.getLocale())
                        .build();
                
                rule.addMessage(message);
            }

            ruleRepository.save(rule);

            log.info("Regla {} creada exitosamente con {} mensajes", rule.getId(), request.getMessages().size());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("id", rule.getId()));

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error creando regla {}: {}", request.getId(), e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                    "Error interno: " + e.getMessage());
        }
    }

    /**
     * Actualiza una regla existente.
     * PUT /rules/{rule_id}
     */
    @PutMapping("/{ruleId}")
    @Transactional
    public Map<String, String> updateRule(@PathVariable String ruleId, 
                                         @Valid @RequestBody RuleUpdateRequest request) {
        try {
            Rule rule = ruleRepository.findById(ruleId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                            "Regla no encontrada: " + ruleId));

            // Actualizar campos no nulos
            if (request.getEnabled() != null) {
                rule.setEnabled(request.getEnabled());
            }
            if (request.getCategory() != null) {
                rule.setCategory(request.getCategory());
            }
            if (request.getPriority() != null) {
                rule.setPriority(request.getPriority());
            }
            if (request.getSeverity() != null) {
                rule.setSeverity(request.getSeverity());
            }
            if (request.getCooldownDays() != null) {
                rule.setCooldownDays(request.getCooldownDays());
            }
            if (request.getMaxPerDay() != null) {
                rule.setMaxPerDay(request.getMaxPerDay());
            }
            if (request.getTags() != null) {
                rule.setTags(request.getTags());
            }
            if (request.getLogic() != null) {
                rule.setLogic(request.getLogic());
            }
            if (request.getLocale() != null) {
                rule.setLocale(request.getLocale());
            }
            if (request.getUpdatedBy() != null) {
                rule.setUpdatedBy(request.getUpdatedBy());
            }

            // Si se proporcionan nuevos mensajes, reemplazar todos
            if (request.getMessages() != null) {
                // Eliminar mensajes existentes
                rule.getMessages().clear();
                
                // Agregar nuevos mensajes
                for (MessageRequest msgReq : request.getMessages()) {
                    RuleMessage message = RuleMessage.builder()
                            .rule(rule)
                            .text(msgReq.getText())
                            .weight(msgReq.getWeight())
                            .active(msgReq.getActive())
                            .locale(msgReq.getLocale() != null ? msgReq.getLocale() : rule.getLocale())
                            .build();
                    
                    rule.addMessage(message);
                }
            }

            ruleRepository.save(rule);

            log.info("Regla {} actualizada exitosamente", ruleId);
            return Map.of("id", ruleId);

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error actualizando regla {}: {}", ruleId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                    "Error interno: " + e.getMessage());
        }
    }

    /**
     * Elimina una regla.
     * DELETE /rules/{rule_id}
     */
    @DeleteMapping("/{ruleId}")
    @Transactional
    public Map<String, Object> deleteRule(@PathVariable String ruleId) {
        try {
            Rule rule = ruleRepository.findById(ruleId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                            "Regla no encontrada: " + ruleId));

            ruleRepository.delete(rule);

            log.info("Regla {} eliminada exitosamente", ruleId);
            return Map.of("id", ruleId, "deleted", true);

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error eliminando regla {}: {}", ruleId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                    "Error interno: " + e.getMessage());
        }
    }

    /**
     * Elimina todas las reglas (solo para testing).
     * DELETE /rules/all
     */
    @DeleteMapping("/all")
    @Transactional
    public Map<String, Object> deleteAllRules() {
        try {
            long totalRules = ruleRepository.count();
            if (totalRules == 0) {
                return Map.of("deleted", 0, "message", "No hay reglas para eliminar");
            }

            long deletedMessages = ruleMessageRepository.count();
            ruleRepository.deleteAll();

            log.warn("¡TODAS las reglas han sido eliminadas! Total: {}", totalRules);
            return Map.of(
                    "deleted", totalRules,
                    "deleted_messages", deletedMessages,
                    "message", String.format("Se eliminaron %d reglas y %d mensajes", totalRules, deletedMessages)
            );

        } catch (Exception e) {
            log.error("Error eliminando todas las reglas: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                    "Error interno: " + e.getMessage());
        }
    }

    /**
     * Cambia el estado de activación de una regla.
     * POST /rules/{rule_id}/enable
     */
    @PostMapping("/{ruleId}/enable")
    @Transactional
    public Map<String, Object> enableRule(@PathVariable String ruleId, 
                                         @RequestBody Map<String, Boolean> request) {
        try {
            Rule rule = ruleRepository.findById(ruleId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                            "Regla no encontrada: " + ruleId));

            boolean enabled = request.getOrDefault("enabled", true);
            rule.setEnabled(enabled);
            ruleRepository.save(rule);

            log.info("Regla {} {}", ruleId, enabled ? "habilitada" : "deshabilitada");
            return Map.of("id", ruleId, "enabled", enabled);

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error cambiando estado de regla {}: {}", ruleId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                    "Error interno: " + e.getMessage());
        }
    }

    /**
     * Clona una regla existente.
     * POST /rules/{rule_id}/clone
     */
    @PostMapping("/{ruleId}/clone")
    @Transactional
    public Map<String, String> cloneRule(@PathVariable String ruleId, 
                                        @RequestBody Map<String, String> request) {
        try {
            Rule originalRule = ruleRepository.findById(ruleId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                            "Regla no encontrada: " + ruleId));

            String newId = request.get("new_id");
            if (newId == null || newId.trim().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                        "new_id es requerido");
            }

            if (ruleRepository.existsById(newId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, 
                        "Ya existe una regla con ID: " + newId);
            }

            // Clonar regla
            Rule clonedRule = Rule.builder()
                    .id(newId)
                    .version(originalRule.getVersion())
                    .enabled(false) // Inicialmente deshabilitada
                    .tenantId(originalRule.getTenantId())
                    .category(originalRule.getCategory())
                    .priority(originalRule.getPriority())
                    .severity(originalRule.getSeverity())
                    .cooldownDays(originalRule.getCooldownDays())
                    .maxPerDay(originalRule.getMaxPerDay())
                    .locale(originalRule.getLocale())
                    .build();

            clonedRule.setTags(originalRule.getTags());
            clonedRule.setLogic(originalRule.getLogic());

            clonedRule = ruleRepository.save(clonedRule);

            // Clonar mensajes
            for (RuleMessage originalMessage : originalRule.getMessages()) {
                RuleMessage clonedMessage = RuleMessage.builder()
                        .rule(clonedRule)
                        .text(originalMessage.getText())
                        .weight(originalMessage.getWeight())
                        .active(originalMessage.getActive())
                        .locale(originalMessage.getLocale())
                        .build();
                
                clonedRule.addMessage(clonedMessage);
            }

            ruleRepository.save(clonedRule);

            log.info("Regla {} clonada como {} con {} mensajes", 
                    ruleId, newId, originalRule.getMessages().size());
            return Map.of("id", newId);

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error clonando regla {}: {}", ruleId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                    "Error interno: " + e.getMessage());
        }
    }

    /**
     * Exporta reglas en formato JSON o YAML.
     * GET /rules/export?format=json|yaml
     */
    @GetMapping("/export")
    public List<Rule> exportRules(@RequestParam(defaultValue = "json") String format,
                                 @RequestParam(defaultValue = "default") String tenantId) {
        try {
            List<Rule> rules = ruleRepository.findByTenantId(tenantId);
            
            // Para YAML, Spring Boot manejará la serialización automáticamente
            // basándose en el Accept header o el parámetro format
            
            log.info("Exportando {} reglas en formato {}", rules.size(), format);
            return rules;

        } catch (Exception e) {
            log.error("Error exportando reglas: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                    "Error exportando reglas: " + e.getMessage());
        }
    }

    /**
     * Importa reglas desde JSON o YAML.
     * POST /rules/import
     */
    @PostMapping("/import")
    @Transactional
    public Map<String, Object> importRules(@RequestBody String data,
                                          @RequestParam(defaultValue = "json") String format) {
        try {
            List<RuleRequest> rulesToImport;

            if ("yaml".equalsIgnoreCase(format)) {
                try {
                    // Intento directo a RuleRequest (YAML en snake_case como lista)
                    rulesToImport = yamlMapper.readValue(data, new TypeReference<List<RuleRequest>>() {});
                } catch (Exception parseEx) {
                    try {
                        // Fallback: YAML con estructura de mensajes anidada como lista
                        List<Map<String, Object>> rawList = yamlMapper.readValue(data, new TypeReference<List<Map<String, Object>>>() {});
                        rulesToImport = new ArrayList<>();
                        for (Map<String, Object> raw : rawList) {
                            rulesToImport.add(convertRawToRuleRequest(raw));
                        }
                    } catch (Exception parseEx2) {
                        // Fallback final: YAML con reglas como claves del nivel raíz (formato rules_basic.yaml)
                        Map<String, Object> rootMap = yamlMapper.readValue(data, new TypeReference<Map<String, Object>>() {});
                        rulesToImport = new ArrayList<>();
                        for (Map.Entry<String, Object> entry : rootMap.entrySet()) {
                            String ruleId = entry.getKey();
                            if (entry.getValue() instanceof Map<?, ?> ruleData) {
                                Map<String, Object> ruleMap = new HashMap<>((Map<String, Object>) ruleData);
                                ruleMap.put("id", ruleId); // Agregar el ID como campo
                                rulesToImport.add(convertRawToRuleRequest(ruleMap));
                            }
                        }
                    }
                }
            } else {
                rulesToImport = objectMapper.readValue(data, new TypeReference<List<RuleRequest>>() {});
            }

            if (rulesToImport == null || rulesToImport.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                        "Los datos deben contener una lista de reglas");
            }

            List<String> created = new ArrayList<>();
            List<String> updated = new ArrayList<>();

            for (RuleRequest ruleRequest : rulesToImport) {
                boolean exists = ruleRepository.existsById(ruleRequest.getId());
                
                if (exists) {
                    // Actualizar regla existente
                    Rule existingRule = ruleRepository.findById(ruleRequest.getId()).get();
                    updateRuleFromRequest(existingRule, ruleRequest);
                    updated.add(ruleRequest.getId());
                } else {
                    // Crear nueva regla
                    Rule newRule = createRuleFromRequest(ruleRequest);
                    ruleRepository.save(newRule);
                    created.add(ruleRequest.getId());
                }
            }

            log.info("Importación completada: {} creadas, {} actualizadas", 
                    created.size(), updated.size());
            
            return Map.of(
                    "created", created,
                    "updated", updated,
                    "total", created.size() + updated.size()
            );

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error importando reglas: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                    "Error interno: " + e.getMessage());
        }
    }

    /**
     * Convierte un mapa crudo (posible YAML) a RuleRequest, aceptando keys snake_case y
     * mensajes anidados (messages: { locale, candidates: [...] }).
     */
    @SuppressWarnings("unchecked")
    private RuleRequest convertRawToRuleRequest(Map<String, Object> raw) {
        String id = stringVal(raw.get("id"));
        if (id == null || id.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cada regla debe tener 'id'");
        }

        String tenantId = stringVal(firstNonNull(raw.get("tenantId"), raw.get("tenant_id"), "default"));
        String category = stringVal(raw.get("category"));
        Integer priority = intVal(firstNonNull(raw.get("priority"), 50));
        Integer severity = intVal(firstNonNull(raw.get("severity"), 1));
        Integer cooldownDays = intVal(firstNonNull(raw.get("cooldownDays"), raw.get("cooldown_days"), 0));
        Integer maxPerDay = intVal(firstNonNull(raw.get("maxPerDay"), raw.get("max_per_day"), 0));
        Boolean enabled = boolVal(firstNonNull(raw.get("enabled"), true));
        String locale = stringVal(firstNonNull(raw.get("locale"), "es-ES"));

        // Tags: puede venir como lista o como string JSON
        List<String> tags = new ArrayList<>();
        Object tagsRaw = raw.get("tags");
        if (tagsRaw instanceof List<?>) {
            for (Object t : (List<?>) tagsRaw) {
                if (t != null) tags.add(String.valueOf(t));
            }
        } else if (tagsRaw instanceof String s) {
            try {
                List<String> parsed = objectMapper.readValue(s, new TypeReference<List<String>>() {});
                if (parsed != null) tags = parsed;
            } catch (Exception ignore) { /* noop */ }
        }

        // Logic: puede venir como mapa o string JSON, o derivarse de 'when'
        Map<String, Object> logic = new HashMap<>();
        Object logicRaw = raw.get("logic");
        if (logicRaw instanceof Map<?, ?> m) {
            logic = (Map<String, Object>) m;
        } else if (logicRaw instanceof String s) {
            try {
                logic = objectMapper.readValue(s, new TypeReference<Map<String, Object>>() {});
            } catch (Exception ignore) { logic = Map.of(); }
        } else {
            logic = Map.of();
        }

        // Si no hay logic, intentar construirlo desde 'when'
        if (logic == null || logic.isEmpty()) {
            Object whenObj = raw.get("when");
            if (whenObj instanceof Map<?, ?> w) {
                logic = convertWhenToLogic((Map<String, Object>) w);
            }
        }

        // Mensajes: lista de objetos o estructura anidada { locale, candidates: [...] }
        List<MessageRequest> messages = new ArrayList<>();
        Object messagesRaw = raw.get("messages");
        if (messagesRaw instanceof List<?> lst) {
            for (Object o : lst) {
                if (o instanceof Map<?, ?> mm) {
                    messages.add(MessageRequest.builder()
                            .text(stringVal(mm.get("text")))
                            .weight(intVal(firstNonNull(mm.get("weight"), 1)))
                            .active(boolVal(firstNonNull(mm.get("active"), true)))
                            .locale(stringVal(firstNonNull(mm.get("locale"), locale)))
                            .build());
                }
            }
        } else if (messagesRaw instanceof Map<?, ?> mm) {
            String loc = stringVal(firstNonNull(mm.get("locale"), locale));
            Object candidates = mm.get("candidates");
            if (candidates instanceof List<?> cList) {
                for (Object c : cList) {
                    if (c instanceof Map<?, ?> cm) {
                        messages.add(MessageRequest.builder()
                                .text(stringVal(cm.get("text")))
                                .weight(intVal(firstNonNull(cm.get("weight"), 1)))
                                .active(boolVal(firstNonNull(cm.get("active"), true)))
                                .locale(stringVal(firstNonNull(cm.get("locale"), loc)))
                                .build());
                    }
                }
            }
        }

        // Fallback: si existen 'candidates' (lista de strings) en YAML tipo rules_basic, generar mensajes simples
        if (messages.isEmpty()) {
            Object candidates = raw.get("candidates");
            if (candidates instanceof List<?> cList) {
                int i = 1;
                for (Object c : cList) {
                    String text = String.valueOf(c);
                    messages.add(MessageRequest.builder()
                            .text(text != null && !text.isBlank() ? text : ("Mensaje " + i))
                            .weight(1)
                            .active(true)
                            .locale(locale)
                            .build());
                    i++;
                }
            }
        }
        if (messages.isEmpty()) {
            // Mensaje por defecto si no se proporcionó
            messages.add(MessageRequest.builder().text("Mensaje").weight(1).active(true).locale(locale).build());
        }

        return RuleRequest.builder()
                .id(id)
                .tenantId(tenantId)
                .category(category)
                .priority(priority)
                .severity(severity)
                .cooldownDays(cooldownDays)
                .maxPerDay(maxPerDay)
                .enabled(enabled)
                .tags(tags)
                .logic(logic)
                .locale(locale)
                .messages(messages)
                .build();
    }

    private static Object firstNonNull(Object... values) {
        for (Object v : values) if (v != null) return v; return null;
    }
    private static String stringVal(Object o) { return o == null ? null : String.valueOf(o); }
    private static Integer intVal(Object o) {
        if (o == null) return null;
        try { return (o instanceof Number n) ? n.intValue() : Integer.parseInt(String.valueOf(o)); } catch (Exception e) { return null; }
    }
    private static Boolean boolVal(Object o) {
        if (o == null) return null;
        if (o instanceof Boolean b) return b;
        String s = String.valueOf(o).trim().toLowerCase();
        return ("true".equals(s) || "1".equals(s) || "yes".equals(s));
    }

    private Map<String, Object> convertWhenToLogic(Map<String, Object> when) {
        List<Map<String, Object>> all = new ArrayList<>();

        for (Map.Entry<String, Object> e : when.entrySet()) {
            String key = e.getKey();
            Object val = e.getValue();
            if ("AND".equalsIgnoreCase(key) && val instanceof Map<?, ?> andMap) {
                for (Map.Entry<?, ?> se : ((Map<?, ?>) andMap).entrySet()) {
                    String var = String.valueOf(se.getKey());
                    addCondition(all, var, String.valueOf(se.getValue()));
                }
            } else {
                addCondition(all, key, String.valueOf(val));
            }
        }

        Map<String, Object> logic = new HashMap<>();
        logic.put("all", all);
        return logic;
    }

    private void addCondition(List<Map<String, Object>> all, String var, String raw) {
        String s = raw.trim();
        String op = "==";
        String valueStr = s;
        if (s.startsWith(">=")) { op = ">="; valueStr = s.substring(2).trim(); }
        else if (s.startsWith("<=")) { op = "<="; valueStr = s.substring(2).trim(); }
        else if (s.startsWith(">")) { op = ">"; valueStr = s.substring(1).trim(); }
        else if (s.startsWith("<")) { op = "<"; valueStr = s.substring(1).trim(); }
        else if (s.startsWith("==")) { op = "=="; valueStr = s.substring(2).trim(); }
        else if (s.startsWith("=")) { op = "=="; valueStr = s.substring(1).trim(); }

        Object value;
        try {
            if (valueStr.contains(".")) value = Double.parseDouble(valueStr);
            else value = Integer.parseInt(valueStr);
        } catch (Exception ex) {
            value = valueStr;
        }

        Map<String, Object> cond = new HashMap<>();
        cond.put("var", var);
        cond.put("agg", "current");
        cond.put("op", op);
        cond.put("value", value);
        all.add(cond);
    }

    /**
     * Importa reglas desde CSV.
     * POST /rules/import_csv
     */
    @PostMapping(value = "/import_csv", consumes = "text/csv")
    @Transactional
    public Map<String, Object> importRulesCsv(@RequestBody String csvData) {
        try {
            Map<String, List<Map<String, String>>> groupedRows = new HashMap<>();
            
            // Parsear CSV
            try (CSVReader reader = new CSVReaderBuilder(new StringReader(csvData))
                    .withSkipLines(1)
                    .build()) {
                
                String[] nextLine;
                while ((nextLine = reader.readNext()) != null) {
                    if (nextLine.length < 3) continue;
                    
                    String messageId = nextLine[0].trim();
                    String category = nextLine[1].trim();
                    String templateText = nextLine[2].trim();
                    
                    if (messageId.isEmpty() || templateText.isEmpty()) continue;
                    
                    // Extraer ID base (antes de _v1, _v2, etc.)
                    String baseId = extractBaseId(messageId);
                    
                    Map<String, String> row = Map.of(
                            "message_id", messageId,
                            "category", category,
                            "template_text", templateText
                    );
                    
                    groupedRows.computeIfAbsent(baseId, k -> new ArrayList<>()).add(row);
                }
            }
            
            int created = 0;
            int updated = 0;
            
            // Procesar cada grupo
            for (Map.Entry<String, List<Map<String, String>>> entry : groupedRows.entrySet()) {
                String baseId = entry.getKey();
                List<Map<String, String>> rows = entry.getValue();
                
                boolean exists = ruleRepository.existsById(baseId);
                Rule rule;
                
                if (exists) {
                    rule = ruleRepository.findById(baseId).get();
                    rule.getMessages().clear(); // Reemplazar mensajes existentes
                    updated++;
                } else {
                    // Crear nueva regla
                    String category = rows.get(0).get("category");
                    rule = Rule.builder()
                            .id(baseId)
                            .enabled(true)
                            .tenantId("default")
                            .category(category)
                            .priority(50)
                            .severity(1)
                            .cooldownDays(0)
                            .maxPerDay(0)
                            .locale("es-ES")
                            .build();
                    
                    rule.setTags(List.of());
                    rule.setLogic(Map.of("var", "steps", "op", ">", "value", 0)); // Lógica por defecto
                    created++;
                }
                
                // Agregar mensajes
                for (Map<String, String> row : rows) {
                    RuleMessage message = RuleMessage.builder()
                            .rule(rule)
                            .text(row.get("template_text"))
                            .weight(1)
                            .active(true)
                            .locale(rule.getLocale())
                            .build();
                    
                    rule.addMessage(message);
                }
                
                ruleRepository.save(rule);
            }
            
            log.info("Importación CSV completada: {} creadas, {} actualizadas", created, updated);
            return Map.of("created", created, "updated", updated);
            
        } catch (Exception e) {
            log.error("Error importando CSV: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                    "Error interno: " + e.getMessage());
        }
    }

    /**
     * Importa reglas desde CSV reformado (formato horizontal con variantes en columnas).
     * POST /rules/import_csv_reformed
     */
    @PostMapping(value = "/import_csv_reformed", consumes = "text/csv")
    @Transactional
    public Map<String, Object> importRulesCsvReformed(@RequestBody String csvData) {
        try {
            int created = 0;
            int updated = 0;
            
            // Parsear CSV reformado
            try (CSVReader reader = new CSVReaderBuilder(new StringReader(csvData))
                    .withSkipLines(1)
                    .build()) {
                
                String[] nextLine;
                while ((nextLine = reader.readNext()) != null) {
                    if (nextLine.length < 12) continue; // message_id + category + 10 variantes
                    
                    String messageId = nextLine[0].trim();
                    String category = nextLine[1].trim();
                    
                    if (messageId.isEmpty()) continue;
                    
                    boolean exists = ruleRepository.existsById(messageId);
                    Rule rule;
                    
                    if (exists) {
                        rule = ruleRepository.findById(messageId).get();
                        rule.getMessages().clear(); // Limpiar mensajes existentes
                        updated++;
                    } else {
                        // Crear nueva regla
                        rule = Rule.builder()
                                .id(messageId)
                                .enabled(true)
                                .tenantId("default")
                                .category(category)
                                .priority(50)
                                .severity(1)
                                .cooldownDays(0)
                                .maxPerDay(0)
                                .locale("es-ES")
                                .build();
                        
                        rule.setTags(List.of());
                        rule.setLogic(Map.of("var", "steps", "op", ">", "value", 0)); // Lógica por defecto
                        created++;
                    }
                    
                    // Agregar variantes como mensajes (template_text_v1 hasta template_text_v10)
                    for (int i = 2; i < Math.min(12, nextLine.length); i++) {
                        String templateText = nextLine[i].trim();
                        if (!templateText.isEmpty()) {
                            RuleMessage message = RuleMessage.builder()
                                    .rule(rule)
                                    .text(templateText)
                                    .weight(1)
                                    .active(true)
                                    .locale(rule.getLocale())
                                    .build();
                            
                            rule.addMessage(message);
                        }
                    }
                    
                    ruleRepository.save(rule);
                }
            }
            
            log.info("Importación CSV reformado completada: {} creadas, {} actualizadas", created, updated);
            return Map.of("created", created, "updated", updated);
            
        } catch (Exception e) {
            log.error("Error importando CSV reformado: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                    "Error interno: " + e.getMessage());
        }
    }

    /**
     * Extrae el ID base de un message_id (elimina sufijos como _v1, _v2, etc.)
     */
    private String extractBaseId(String messageId) {
        Pattern pattern = Pattern.compile("_v\\d+$");
        return pattern.matcher(messageId).replaceAll("");
    }

    /**
     * Crea una nueva regla desde RuleRequest para importación.
     */
    private Rule createRuleFromRequest(RuleRequest request) {
        // Crear entidad Rule
        Rule rule = Rule.builder()
                .id(request.getId())
                .version(request.getVersion())
                .enabled(request.getEnabled())
                .tenantId(request.getTenantId())
                .category(request.getCategory())
                .priority(request.getPriority())
                .severity(request.getSeverity())
                .cooldownDays(request.getCooldownDays())
                .maxPerDay(request.getMaxPerDay())
                .locale(request.getLocale())
                .createdBy(request.getCreatedBy())
                .updatedBy(request.getUpdatedBy())
                .build();

        rule.setTags(request.getTags());
        rule.setLogic(request.getLogic());

        // Guardar regla primero
        rule = ruleRepository.save(rule);

        // Crear mensajes
        for (MessageRequest msgReq : request.getMessages()) {
            RuleMessage message = RuleMessage.builder()
                    .rule(rule)
                    .text(msgReq.getText())
                    .weight(msgReq.getWeight())
                    .active(msgReq.getActive())
                    .locale(msgReq.getLocale() != null ? msgReq.getLocale() : rule.getLocale())
                    .build();
            
            rule.addMessage(message);
        }

        return ruleRepository.save(rule);
    }

    /**
     * Actualiza una regla existente con datos de RuleRequest para importación.
     */
    private void updateRuleFromRequest(Rule existingRule, RuleRequest request) {
        existingRule.setEnabled(request.getEnabled());
        existingRule.setCategory(request.getCategory());
        existingRule.setPriority(request.getPriority());
        existingRule.setSeverity(request.getSeverity());
        existingRule.setCooldownDays(request.getCooldownDays());
        existingRule.setMaxPerDay(request.getMaxPerDay());
        existingRule.setTags(request.getTags());
        existingRule.setLogic(request.getLogic());
        existingRule.setLocale(request.getLocale());
        
        // Reemplazar mensajes
        existingRule.getMessages().clear();
        for (MessageRequest msgReq : request.getMessages()) {
            RuleMessage message = RuleMessage.builder()
                    .rule(existingRule)
                    .text(msgReq.getText())
                    .weight(msgReq.getWeight())
                    .active(msgReq.getActive())
                    .locale(msgReq.getLocale() != null ? msgReq.getLocale() : existingRule.getLocale())
                    .build();
            
            existingRule.addMessage(message);
        }
        
        ruleRepository.save(existingRule);
    }

}

