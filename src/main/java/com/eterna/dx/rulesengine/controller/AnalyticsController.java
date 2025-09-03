package com.eterna.dx.rulesengine.controller;

import com.eterna.dx.rulesengine.dto.response.TriggerAnalytics;
import com.eterna.dx.rulesengine.entity.Audit;
import com.eterna.dx.rulesengine.entity.ChangeLog;
import com.eterna.dx.rulesengine.repository.AuditRepository;
import com.eterna.dx.rulesengine.repository.ChangeLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controlador para analytics y logs.
 * Equivalente a analytics.py en FastAPI.
 */
@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
@Slf4j
public class AnalyticsController {

    private final AuditRepository auditRepository;
    private final ChangeLogRepository changeLogRepository;

    /**
     * Obtiene estadísticas de triggers de reglas en un rango de fechas.
     * GET /analytics/triggers?start=YYYY-MM-DD&end=YYYY-MM-DD&tenant_id=...&rule_ids=...
     */
    @GetMapping("/triggers")
    public TriggerAnalytics getTriggerAnalytics(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate start,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end,
            @RequestParam(defaultValue = "default") String tenantId,
            @RequestParam(required = false) String ruleIds) {

        log.debug("Obteniendo analytics de triggers desde {} hasta {} para tenant {}", start, end, tenantId);

        // Parsear rule_ids si se proporciona
        List<String> ruleIdList = null;
        if (ruleIds != null && !ruleIds.trim().isEmpty()) {
            ruleIdList = Arrays.asList(ruleIds.split(","))
                    .stream()
                    .map(String::trim)
                    .filter(id -> !id.isEmpty())
                    .collect(Collectors.toList());
        }

        // Obtener auditorías de triggers
        List<Audit> triggers = auditRepository.findTriggersForAnalytics(tenantId, start, end, ruleIdList);

        // Agrupar por regla y fecha
        Map<String, Map<LocalDate, Long>> ruleStats = new HashMap<>();
        
        for (Audit audit : triggers) {
            String ruleId = audit.getRuleId();
            LocalDate date = audit.getDate();
            
            ruleStats.computeIfAbsent(ruleId, k -> new HashMap<>())
                    .merge(date, 1L, Long::sum);
        }

        // Construir series de datos
        List<TriggerAnalytics.RuleSeries> series = new ArrayList<>();
        
        for (Map.Entry<String, Map<LocalDate, Long>> entry : ruleStats.entrySet()) {
            String ruleId = entry.getKey();
            Map<LocalDate, Long> dateStats = entry.getValue();
            
            List<TriggerAnalytics.DataPoint> points = new ArrayList<>();
            
            // Llenar todos los días en el rango (incluyendo días con 0 triggers)
            LocalDate current = start;
            while (!current.isAfter(end)) {
                Long count = dateStats.getOrDefault(current, 0L);
                points.add(TriggerAnalytics.DataPoint.builder()
                        .date(current)
                        .count(count)
                        .build());
                current = current.plusDays(1);
            }
            
            series.add(TriggerAnalytics.RuleSeries.builder()
                    .ruleId(ruleId)
                    .points(points)
                    .build());
        }

        return TriggerAnalytics.builder()
                .start(start)
                .end(end)
                .series(series)
                .build();
    }

    /**
     * Obtiene logs de cambios con filtros.
     * GET /analytics/logs?start=YYYY-MM-DD&end=YYYY-MM-DD&rule_id=...&user=...&action=...&limit=...
     */
    @GetMapping("/logs")
    public Page<ChangeLog> getLogs(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityId,
            @RequestParam(required = false) String user,
            @RequestParam(required = false) String action,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int limit) {

        log.debug("Obteniendo logs con filtros: start={}, end={}, entityType={}, entityId={}, user={}, action={}", 
                start, end, entityType, entityId, user, action);

        // Convertir fechas a LocalDateTime si se proporcionan
        LocalDateTime startDateTime = start != null ? start.atStartOfDay() : null;
        LocalDateTime endDateTime = end != null ? end.plusDays(1).atStartOfDay() : null;

        Pageable pageable = PageRequest.of(page, Math.min(limit, 1000)); // Máximo 1000 por página

        return changeLogRepository.findLogsWithFilters(
                startDateTime, endDateTime, entityType, entityId, user, action, pageable);
    }

    /**
     * Descarga logs como JSON.
     * GET /analytics/logs/download
     */
    @GetMapping("/logs/download")
    public List<ChangeLog> downloadLogs(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {

        log.info("Descargando logs desde {} hasta {}", start, end);

        LocalDateTime startDateTime = start != null ? start.atStartOfDay() : null;
        LocalDateTime endDateTime = end != null ? end.plusDays(1).atStartOfDay() : null;

        if (startDateTime != null && endDateTime != null) {
            return changeLogRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(startDateTime, endDateTime);
        } else {
            // Si no se especifican fechas, devolver los últimos 1000 registros
            Pageable pageable = PageRequest.of(0, 1000);
            return changeLogRepository.findByOrderByCreatedAtDesc(pageable).getContent();
        }
    }
}
