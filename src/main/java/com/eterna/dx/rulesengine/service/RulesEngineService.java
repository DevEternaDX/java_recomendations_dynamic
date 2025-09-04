package com.eterna.dx.rulesengine.service;

import com.eterna.dx.rulesengine.config.AppProperties;
import com.eterna.dx.rulesengine.dsl.DSLParser;
import com.eterna.dx.rulesengine.dsl.Node;
import com.eterna.dx.rulesengine.dto.response.RecommendationEvent;
import com.eterna.dx.rulesengine.dto.response.RuleDebugInfo;
import com.eterna.dx.rulesengine.dto.response.SimulationResult;
import com.eterna.dx.rulesengine.entity.Audit;
import com.eterna.dx.rulesengine.entity.Rule;
import com.eterna.dx.rulesengine.entity.RuleMessage;
import com.eterna.dx.rulesengine.features.CombinedRecord;
import com.eterna.dx.rulesengine.features.FeatureService;
import com.eterna.dx.rulesengine.repository.AuditRepository;
import com.eterna.dx.rulesengine.repository.RuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio principal del motor de reglas.
 * Equivalente a evaluate_user() en Python.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RulesEngineService {

    private final FeatureService featureService;
    private final MessageService messageService;
    private final DSLParser dslParser;
    private final RuleRepository ruleRepository;
    private final AuditRepository auditRepository;
    private final AppProperties appProperties;

    /**
     * Evalúa todas las reglas para un usuario en una fecha específica.
     * Equivalente a evaluate_user() en Python.
     */
    @Transactional
    public SimulationResult evaluateUser(String userId, LocalDate targetDate, String tenantId, boolean debug) {
        log.info("Evaluando reglas para usuario {} en fecha {} (tenant: {}, debug: {})", 
                userId, targetDate, tenantId, debug);

        List<RecommendationEvent> events = new ArrayList<>();
        List<RuleDebugInfo> debugInfo = new ArrayList<>();

        try {
            // 1. Generar features
            Map<String, Map<String, Object>> features = buildUserFeatures(userId, targetDate);
            
            if (features.isEmpty()) {
                log.warn("No hay datos para usuario {} hasta fecha {}", userId, targetDate);
                return SimulationResult.builder()
                        .events(events)
                        .debug(debug ? Map.of("message", "No hay datos para el usuario", "features", features) : null)
                        .build();
            }

            // 2. Obtener reglas activas
            List<Rule> activeRules = ruleRepository.findByTenantIdAndEnabledOrderByPriorityDescSeverityDesc(tenantId, true);
            log.debug("Encontradas {} reglas activas para tenant {}", activeRules.size(), tenantId);

            // 3. Evaluar cada regla
            for (Rule rule : activeRules) {
                try {
                    RecommendationEvent event = evaluateRule(rule, features, userId, targetDate, tenantId);
                    
                    // Crear información de debug
                    if (debug) {
                        RuleDebugInfo ruleDebug = RuleDebugInfo.builder()
                                .ruleId(rule.getId())
                                .fired(event != null)
                                .priority(rule.getPriority())
                                .severity(rule.getSeverity())
                                .build();
                        debugInfo.add(ruleDebug);
                    }

                    if (event != null) {
                        events.add(event);
                    }

                } catch (Exception e) {
                    log.error("Error evaluando regla {}: {}", rule.getId(), e.getMessage(), e);
                    
                    if (debug) {
                        RuleDebugInfo ruleDebug = RuleDebugInfo.builder()
                                .ruleId(rule.getId())
                                .fired(false)
                                .priority(rule.getPriority())
                                .severity(rule.getSeverity())
                                .build();
                        debugInfo.add(ruleDebug);
                    }
                }
            }

            // 4. Post-procesamiento: aplicar cooldowns y resolver conflictos
            events = postProcessEvents(events, userId, targetDate);

            log.info("Evaluación completada: {} eventos generados para usuario {}", events.size(), userId);

            // 5. Construir respuesta
            Map<String, Object> debugData = null;
            if (debug) {
                debugData = Map.of(
                        "user_features", features,
                        "rules_evaluated", debugInfo.size(),
                        "events_before_postprocess", events.size(),
                        "rules_debug", debugInfo
                );
            }

            return SimulationResult.builder()
                    .events(events)
                    .debug(debugData)
                    .build();

        } catch (Exception e) {
            log.error("Error en evaluación de usuario {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Error en evaluación de reglas: " + e.getMessage(), e);
        }
    }

    /**
     * Construye las features para un usuario.
     */
    private Map<String, Map<String, Object>> buildUserFeatures(String userId, LocalDate targetDate) {
        try {
            List<CombinedRecord> allRecords = featureService.loadBaseDataframe();
            return featureService.buildFeatures(allRecords, targetDate, userId);
        } catch (Exception e) {
            log.error("Error construyendo features para usuario {}: {}", userId, e.getMessage());
            return Map.of();
        }
    }

    /**
     * Evalúa una regla específica contra las features.
     */
    private RecommendationEvent evaluateRule(Rule rule, Map<String, Map<String, Object>> features, 
                                           String userId, LocalDate targetDate, String tenantId) {
        List<Map<String, Object>> whyTrace = new ArrayList<>();
        boolean fired = false;
        RuleMessage selectedMessage = null;
        String messageText = "";

        try {
            // 1. Parsear y evaluar la lógica de la regla
            Node ruleNode = dslParser.parseNode(rule.getLogic());
            fired = ruleNode.eval(features, whyTrace);

            log.debug("Regla {} evaluada: fired={}", rule.getId(), fired);

            // 2. Si la regla se dispara, seleccionar mensaje
            if (fired) {
                selectedMessage = messageService.selectMessageForRule(rule, userId, targetDate);
                
                if (selectedMessage != null) {
                    MessageService.MessageRenderResult renderResult = 
                            messageService.renderMessage(selectedMessage.getText(), features);
                    messageText = renderResult.getText();
                    
                    if (renderResult.hasWarnings()) {
                        log.debug("Advertencias en renderizado de mensaje para regla {}: {}", 
                                rule.getId(), renderResult.getWarnings());
                    }
                } else {
                    log.warn("Regla {} disparada pero sin mensajes disponibles", rule.getId());
                    fired = false; // Sin mensaje, no generar evento
                }
            }

        } catch (Exception e) {
            log.error("Error evaluando lógica de regla {}: {}", rule.getId(), e.getMessage());
            fired = false;
        }

        // 3. Registrar auditoría
        try {
            Audit audit = Audit.builder()
                    .userId(userId)
                    .date(targetDate)
                    .tenantId(tenantId)
                    .ruleId(rule.getId())
                    .fired(fired)
                    .messageId(selectedMessage != null ? selectedMessage.getId() : null)
                    .build();

            // Serializar información de debug
            if (!whyTrace.isEmpty()) {
                audit.setWhy(Map.of("conditions", whyTrace));
            }
            audit.setValues(Map.of("features", features));

            auditRepository.save(audit);
        } catch (Exception e) {
            log.error("Error guardando auditoría para regla {}: {}", rule.getId(), e.getMessage());
        }

        // 4. Crear evento si la regla se disparó
        if (fired && selectedMessage != null) {
            return RecommendationEvent.builder()
                    .date(targetDate)
                    .tenantId(tenantId)
                    .userId(userId)
                    .ruleId(rule.getId())
                    .ruleName(rule.getId()) // Por ahora usar el ID como nombre
                    .category(rule.getCategory())
                    .severity(rule.getSeverity())
                    .priority(rule.getPriority())
                    .messageId(selectedMessage.getId())
                    .messageText(messageText)
                    .locale(rule.getLocale())
                    .why(whyTrace)
                    .build();
        }

        return null;
    }

    /**
     * Post-procesa los eventos aplicando cooldowns y resolviendo conflictos.
     * Equivalente a enforce_cooldowns() y resolve_conflicts() en Python.
     */
    private List<RecommendationEvent> postProcessEvents(List<RecommendationEvent> events, 
                                                       String userId, LocalDate targetDate) {
        // 1. Aplicar cooldowns
        List<RecommendationEvent> afterCooldowns = enforceCooldowns(events, userId, targetDate);
        
        // 2. Resolver conflictos (ordenar por prioridad y aplicar límites)
        List<RecommendationEvent> afterConflicts = resolveConflicts(afterCooldowns);

        log.debug("Post-procesamiento: {} -> {} -> {} eventos", 
                events.size(), afterCooldowns.size(), afterConflicts.size());

        return afterConflicts;
    }

    /**
     * Aplica cooldowns eliminando eventos de reglas que dispararon recientemente.
     */
    private List<RecommendationEvent> enforceCooldowns(List<RecommendationEvent> events, 
                                                      String userId, LocalDate targetDate) {
        List<RecommendationEvent> filtered = new ArrayList<>();

        for (RecommendationEvent event : events) {
            // Obtener la regla para verificar su cooldown
            Optional<Rule> ruleOpt = ruleRepository.findById(event.getRuleId());
            if (ruleOpt.isEmpty()) {
                continue; // Regla no encontrada, saltar
            }

            Rule rule = ruleOpt.get();
            int cooldownDays = rule.getCooldownDays();

            if (cooldownDays <= 0) {
                // Sin cooldown, incluir evento
                filtered.add(event);
                continue;
            }

            // Verificar si la regla disparó en los últimos N días
            LocalDate sinceDate = targetDate.minusDays(cooldownDays);
            boolean recentFire = auditRepository.existsByUserIdAndRuleIdAndFiredAndDateBetween(
                    userId, rule.getId(), true, sinceDate, targetDate.minusDays(1));

            if (!recentFire) {
                filtered.add(event);
            } else {
                log.debug("Evento de regla {} filtrado por cooldown ({} días)", rule.getId(), cooldownDays);
            }
        }

        return filtered;
    }

    /**
     * Resuelve conflictos aplicando límites por categoría y totales.
     * Equivalente a resolve_conflicts() en Python.
     */
    private List<RecommendationEvent> resolveConflicts(List<RecommendationEvent> events) {
        // 1. Ordenar por prioridad desc, severidad desc
        events.sort(Comparator.comparing(RecommendationEvent::getPriority, Comparator.reverseOrder())
                .thenComparing(RecommendationEvent::getSeverity, Comparator.reverseOrder()));

        // 2. Aplicar límites
        List<RecommendationEvent> filtered = new ArrayList<>();
        Map<String, Integer> perCategoryCount = new HashMap<>();

        int maxPerDay = appProperties.getMaxRecsPerDay();
        int maxPerCategory = appProperties.getMaxRecsPerCategoryPerDay();

        for (RecommendationEvent event : events) {
            String category = event.getCategory();

            // Verificar límite por categoría
            if (maxPerCategory > 0) {
                int currentCategoryCount = perCategoryCount.getOrDefault(category, 0);
                if (currentCategoryCount >= maxPerCategory) {
                    log.debug("Evento de categoría {} filtrado por límite de categoría ({})", 
                            category, maxPerCategory);
                    continue;
                }
            }

            // Verificar límite total
            if (maxPerDay > 0 && filtered.size() >= maxPerDay) {
                log.debug("Evento filtrado por límite total diario ({})", maxPerDay);
                break;
            }

            // Incluir evento
            filtered.add(event);
            perCategoryCount.put(category, perCategoryCount.getOrDefault(category, 0) + 1);
        }

        return filtered;
    }

    /**
     * Obtiene features calculadas para un usuario (endpoint de debug).
     */
    public Map<String, Map<String, Object>> getFeatures(String userId, LocalDate date) {
        return buildUserFeatures(userId, date);
    }
}

