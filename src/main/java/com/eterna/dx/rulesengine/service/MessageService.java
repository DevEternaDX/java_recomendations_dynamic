package com.eterna.dx.rulesengine.service;

import com.eterna.dx.rulesengine.config.AppProperties;
import com.eterna.dx.rulesengine.entity.Audit;
import com.eterna.dx.rulesengine.entity.Rule;
import com.eterna.dx.rulesengine.entity.RuleMessage;
import com.eterna.dx.rulesengine.repository.AuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Servicio para manejo de mensajes de reglas.
 * Incluye selección aleatoria con pesos y renderizado de templates.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {

    private final AppProperties appProperties;
    private final AuditRepository auditRepository;
    private final Random random = new Random();

    // Patrón para placeholders: {{variable:aggregator:format}}
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile(
            "\\{\\{\\s*([a-zA-Z0-9_]+)(?::([a-zA-Z0-9_]+))?(?::([^}]+))?\\s*\\}\\}"
    );

    /**
     * Selecciona un mensaje para una regla aplicando anti-repetición y pesos.
     * Equivalente a select_message_for_rule en Python.
     */
    public RuleMessage selectMessageForRule(Rule rule, String userId, LocalDate date) {
        List<RuleMessage> activeMessages = rule.getMessages().stream()
                .filter(m -> Boolean.TRUE.equals(m.getActive()))
                .collect(Collectors.toList());

        if (activeMessages.isEmpty()) {
            log.warn("Regla {} no tiene mensajes activos", rule.getId());
            return null;
        }

        // Aplicar anti-repetición: filtrar mensajes usados recientemente
        List<RuleMessage> preferredMessages = applyAntiRepetition(activeMessages, rule.getId(), userId, date);

        // Si no hay mensajes preferidos (todos fueron usados recientemente), usar todos
        List<RuleMessage> candidateMessages = preferredMessages.isEmpty() ? activeMessages : preferredMessages;

        // Selección aleatoria con pesos
        return selectWeightedRandom(candidateMessages);
    }

    /**
     * Aplica la lógica de anti-repetición filtrando mensajes usados recientemente.
     */
    private List<RuleMessage> applyAntiRepetition(List<RuleMessage> messages, String ruleId, String userId, LocalDate date) {
        if (appProperties.getAntiRepeatDays() <= 0) {
            return messages; // Anti-repetición deshabilitada
        }

        LocalDate sinceDate = date.minusDays(appProperties.getAntiRepeatDays());

        // Obtener IDs de mensajes usados recientemente
        List<Audit> recentAudits = auditRepository.findByUserIdAndRuleIdAndFired(userId, ruleId, true);
        
        List<Integer> recentMessageIds = recentAudits.stream()
                .filter(audit -> audit.getDate().isAfter(sinceDate))
                .filter(audit -> audit.getMessageId() != null)
                .map(Audit::getMessageId)
                .distinct()
                .collect(Collectors.toList());

        if (recentMessageIds.isEmpty()) {
            return messages; // No hay mensajes recientes, usar todos
        }

        // Filtrar mensajes no usados recientemente
        return messages.stream()
                .filter(message -> !recentMessageIds.contains(message.getId()))
                .collect(Collectors.toList());
    }

    /**
     * Selecciona un mensaje aleatoriamente basado en pesos.
     * Equivalente a random.choices con weights en Python.
     */
    private RuleMessage selectWeightedRandom(List<RuleMessage> messages) {
        if (messages.isEmpty()) {
            return null;
        }

        if (messages.size() == 1) {
            return messages.get(0);
        }

        // Calcular peso total
        int totalWeight = messages.stream()
                .mapToInt(m -> m.getWeight() != null ? m.getWeight() : 1)
                .sum();

        if (totalWeight <= 0) {
            // Si todos los pesos son 0 o negativos, seleccionar uniformemente
            return messages.get(random.nextInt(messages.size()));
        }

        // Selección aleatoria basada en pesos
        int randomValue = random.nextInt(totalWeight);
        int cumulativeWeight = 0;

        for (RuleMessage message : messages) {
            int weight = message.getWeight() != null ? message.getWeight() : 1;
            cumulativeWeight += weight;
            
            if (randomValue < cumulativeWeight) {
                return message;
            }
        }

        // Fallback (no debería llegar aquí)
        return messages.get(messages.size() - 1);
    }

    /**
     * Renderiza un template de mensaje reemplazando placeholders con valores de features.
     * Equivalente a la función de renderizado en Python.
     */
    public MessageRenderResult renderMessage(String template, Map<String, Map<String, Object>> features) {
        if (template == null || template.isEmpty()) {
            return new MessageRenderResult("", List.of());
        }

        List<String> warnings = new ArrayList<>();
        String result = template;

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        
        while (matcher.find()) {
            String fullMatch = matcher.group(0);
            String variable = matcher.group(1);
            String aggregator = matcher.group(2) != null ? matcher.group(2) : "current";
            String format = matcher.group(3);

            try {
                // Obtener valor de las features
                Object value = null;
                if (features.containsKey(variable) && features.get(variable).containsKey(aggregator)) {
                    value = features.get(variable).get(aggregator);
                }

                if (value == null) {
                    warnings.add(String.format("Placeholder %s no encontrado en features", fullMatch));
                    result = result.replace(fullMatch, "[?]");
                    continue;
                }

                // Formatear valor
                String formattedValue = formatValue(value, format);
                result = result.replace(fullMatch, formattedValue);

            } catch (Exception e) {
                log.warn("Error procesando placeholder {}: {}", fullMatch, e.getMessage());
                warnings.add(String.format("Error en placeholder %s: %s", fullMatch, e.getMessage()));
                result = result.replace(fullMatch, "[Error]");
            }
        }

        return new MessageRenderResult(result, warnings);
    }

    /**
     * Formatea un valor según el formato especificado.
     */
    private String formatValue(Object value, String format) {
        if (value == null) {
            return "";
        }

        try {
            // Si no hay formato específico, usar toString
            if (format == null || format.trim().isEmpty()) {
                return value.toString();
            }

            // Intentar formatear como número
            if (value instanceof Number) {
                double doubleValue = ((Number) value).doubleValue();
                
                // Formatos comunes
                switch (format.toLowerCase()) {
                    case ".0f":
                        return String.format("%.0f", doubleValue);
                    case ".1f":
                        return String.format("%.1f", doubleValue);
                    case ".2f":
                        return String.format("%.2f", doubleValue);
                    case "d":
                        return String.valueOf((int) doubleValue);
                    default:
                        // Intentar usar el formato directamente
                        return String.format("%" + format, doubleValue);
                }
            }

            // Para otros tipos, usar toString
            return value.toString();
            
        } catch (Exception e) {
            log.warn("Error formateando valor {} con formato {}: {}", value, format, e.getMessage());
            return value.toString();
        }
    }

    /**
     * Resultado del renderizado de un mensaje.
     */
    public static class MessageRenderResult {
        private final String text;
        private final List<String> warnings;

        public MessageRenderResult(String text, List<String> warnings) {
            this.text = text;
            this.warnings = warnings != null ? warnings : List.of();
        }

        public String getText() {
            return text;
        }

        public List<String> getWarnings() {
            return warnings;
        }

        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }
    }
}
