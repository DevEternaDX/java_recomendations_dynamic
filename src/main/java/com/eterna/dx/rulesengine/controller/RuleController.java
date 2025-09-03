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
import java.util.stream.Collectors;

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
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

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
    public Rule getRule(@PathVariable String ruleId) {
        return ruleRepository.findById(ruleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                        "Regla no encontrada: " + ruleId));
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
            List<Rule> rulesToImport;
            
            if ("yaml".equalsIgnoreCase(format)) {
                rulesToImport = yamlMapper.readValue(data, new TypeReference<List<Rule>>() {});
            } else {
                rulesToImport = objectMapper.readValue(data, new TypeReference<List<Rule>>() {});
            }

            if (rulesToImport == null || rulesToImport.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                        "Los datos deben contener una lista de reglas");
            }

            List<String> created = new ArrayList<>();
            List<String> updated = new ArrayList<>();

            for (Rule ruleData : rulesToImport) {
                boolean exists = ruleRepository.existsById(ruleData.getId());
                
                if (exists) {
                    // Actualizar regla existente
                    Rule existingRule = ruleRepository.findById(ruleData.getId()).get();
                    updateRuleFromData(existingRule, ruleData);
                    updated.add(ruleData.getId());
                } else {
                    // Crear nueva regla
                    ruleRepository.save(ruleData);
                    created.add(ruleData.getId());
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
     * Extrae el ID base de un message_id (elimina sufijos como _v1, _v2, etc.)
     */
    private String extractBaseId(String messageId) {
        Pattern pattern = Pattern.compile("_v\\d+$");
        return pattern.matcher(messageId).replaceAll("");
    }

    /**
     * Actualiza una regla existente con datos de importación.
     */
    private void updateRuleFromData(Rule existingRule, Rule ruleData) {
        existingRule.setEnabled(ruleData.getEnabled());
        existingRule.setCategory(ruleData.getCategory());
        existingRule.setPriority(ruleData.getPriority());
        existingRule.setSeverity(ruleData.getSeverity());
        existingRule.setCooldownDays(ruleData.getCooldownDays());
        existingRule.setMaxPerDay(ruleData.getMaxPerDay());
        existingRule.setTags(ruleData.getTags());
        existingRule.setLogic(ruleData.getLogic());
        existingRule.setLocale(ruleData.getLocale());
        
        // Reemplazar mensajes
        existingRule.getMessages().clear();
        for (RuleMessage message : ruleData.getMessages()) {
            message.setRule(existingRule);
            existingRule.addMessage(message);
        }
        
        ruleRepository.save(existingRule);
    }
}
