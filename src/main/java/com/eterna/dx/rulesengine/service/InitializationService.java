package com.eterna.dx.rulesengine.service;

import com.eterna.dx.rulesengine.entity.Rule;
import com.eterna.dx.rulesengine.entity.RuleMessage;
import com.eterna.dx.rulesengine.entity.Variable;
import com.eterna.dx.rulesengine.repository.RuleRepository;
import com.eterna.dx.rulesengine.repository.VariableRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Servicio de inicialización que carga datos de seed al arrancar la aplicación.
 * Equivalente a la funcionalidad de startup en FastAPI.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InitializationService implements CommandLineRunner {

    private final VariableRepository variableRepository;
    private final RuleRepository ruleRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("Iniciando carga de datos de seed...");

        // Cargar variables
        seedVariables();

        // Cargar reglas
        seedRules();

        log.info("Carga de datos de seed completada");
    }

    /**
     * Carga variables desde el archivo seed.
     */
    private void seedVariables() {
        try {
            if (variableRepository.count() > 0) {
                log.info("Variables ya existen en la base de datos, saltando seed");
                return;
            }

            ClassPathResource resource = new ClassPathResource("seeds/variables_seed.json");
            if (!resource.exists()) {
                log.warn("Archivo variables_seed.json no encontrado, saltando seed de variables");
                return;
            }

            try (InputStream inputStream = resource.getInputStream()) {
                List<Map<String, Object>> variableData = objectMapper.readValue(inputStream, 
                        new TypeReference<List<Map<String, Object>>>() {});

                int loaded = 0;
                for (Map<String, Object> varData : variableData) {
                    try {
                        Variable variable = Variable.builder()
                                .key((String) varData.get("key"))
                                .label((String) varData.get("label"))
                                .description((String) varData.get("description"))
                                .unit((String) varData.get("unit"))
                                .type((String) varData.getOrDefault("type", "number"))
                                .validMin(getDoubleValue(varData, "valid_min"))
                                .validMax(getDoubleValue(varData, "valid_max"))
                                .missingPolicy((String) varData.getOrDefault("missing_policy", "skip"))
                                .decimals(getIntegerValue(varData, "decimals"))
                                .category((String) varData.get("category"))
                                .tenantId((String) varData.getOrDefault("tenant_id", "default"))
                                .build();

                        // Manejar allowed_aggregators
                        Object allowedAgg = varData.get("allowed_aggregators");
                        if (allowedAgg instanceof List) {
                            variable.setAllowedAggregators((List<String>) allowedAgg);
                        }

                        // Manejar examples
                        Object examples = varData.get("examples");
                        if (examples instanceof Map) {
                            variable.setExamples((Map<String, Object>) examples);
                        }

                        // Manejar valid_range
                        Object validRange = varData.get("valid_range");
                        if (validRange instanceof List) {
                            List<?> range = (List<?>) validRange;
                            if (range.size() >= 2) {
                                if (range.get(0) != null) {
                                    variable.setValidMin(((Number) range.get(0)).doubleValue());
                                }
                                if (range.get(1) != null) {
                                    variable.setValidMax(((Number) range.get(1)).doubleValue());
                                }
                            }
                        }

                        variableRepository.save(variable);
                        loaded++;

                    } catch (Exception e) {
                        log.error("Error cargando variable {}: {}", varData.get("key"), e.getMessage());
                    }
                }

                log.info("Cargadas {} variables desde seed", loaded);
            }

        } catch (Exception e) {
            log.error("Error cargando variables seed: {}", e.getMessage(), e);
        }
    }

    /**
     * Carga reglas desde el archivo seed.
     */
    private void seedRules() {
        try {
            if (ruleRepository.count() > 0) {
                log.info("Reglas ya existen en la base de datos, saltando seed");
                return;
            }

            ClassPathResource resource = new ClassPathResource("seeds/rules_seed.json");
            if (!resource.exists()) {
                log.warn("Archivo rules_seed.json no encontrado, saltando seed de reglas");
                return;
            }

            try (InputStream inputStream = resource.getInputStream()) {
                JsonNode rootNode = objectMapper.readTree(inputStream);

                int loaded = 0;
                for (JsonNode ruleNode : rootNode) {
                    try {
                        Rule rule = Rule.builder()
                                .id(ruleNode.get("id").asText())
                                .version(ruleNode.get("version").asInt(1))
                                .enabled(ruleNode.get("enabled").asBoolean(true))
                                .tenantId(ruleNode.get("tenant_id").asText("default"))
                                .category(ruleNode.get("category").asText())
                                .priority(ruleNode.get("priority").asInt(50))
                                .severity(ruleNode.get("severity").asInt(1))
                                .cooldownDays(ruleNode.get("cooldown_days").asInt(0))
                                .maxPerDay(ruleNode.get("max_per_day").asInt(0))
                                .locale(ruleNode.get("locale").asText("es-ES"))
                                .build();

                        // Procesar tags
                        JsonNode tagsNode = ruleNode.get("tags");
                        if (tagsNode != null && tagsNode.isArray()) {
                            List<String> tags = new ArrayList<>();
                            for (JsonNode tagNode : tagsNode) {
                                tags.add(tagNode.asText());
                            }
                            rule.setTags(tags);
                        }

                        // Procesar logic
                        JsonNode logicNode = ruleNode.get("logic");
                        if (logicNode != null) {
                            Map<String, Object> logic = objectMapper.convertValue(logicNode, 
                                    new TypeReference<Map<String, Object>>() {});
                            rule.setLogic(logic);
                        }

                        rule = ruleRepository.save(rule);

                        // Procesar mensajes
                        JsonNode messagesNode = ruleNode.get("messages");
                        if (messagesNode != null) {
                            String locale = messagesNode.get("locale").asText(rule.getLocale());
                            JsonNode candidatesNode = messagesNode.get("candidates");
                            
                            if (candidatesNode != null && candidatesNode.isArray()) {
                                for (JsonNode candidateNode : candidatesNode) {
                                    RuleMessage message = RuleMessage.builder()
                                            .rule(rule)
                                            .text(candidateNode.get("text").asText())
                                            .weight(candidateNode.get("weight").asInt(1))
                                            .active(candidateNode.get("active").asBoolean(true))
                                            .locale(locale)
                                            .build();
                                    
                                    rule.addMessage(message);
                                }
                            }
                        }

                        ruleRepository.save(rule);
                        loaded++;

                    } catch (Exception e) {
                        log.error("Error cargando regla {}: {}", 
                                ruleNode.has("id") ? ruleNode.get("id").asText() : "unknown", 
                                e.getMessage());
                    }
                }

                log.info("Cargadas {} reglas desde seed", loaded);
            }

        } catch (Exception e) {
            log.error("Error cargando reglas seed: {}", e.getMessage(), e);
        }
    }

    /**
     * Obtiene un valor Double de un Map de forma segura.
     */
    private Double getDoubleValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) return null;
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Obtiene un valor Integer de un Map de forma segura.
     */
    private Integer getIntegerValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) return null;
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
