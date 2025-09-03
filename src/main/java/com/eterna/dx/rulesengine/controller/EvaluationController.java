package com.eterna.dx.rulesengine.controller;

import com.eterna.dx.rulesengine.dto.request.SimulateRequest;
import com.eterna.dx.rulesengine.dto.response.SimulationResult;
import com.eterna.dx.rulesengine.service.RulesEngineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * Controlador para evaluación de reglas y simulación.
 * Equivalente a evaluate.py en FastAPI.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class EvaluationController {

    private final RulesEngineService rulesEngineService;

    /**
     * Simula la evaluación de reglas para un usuario en una fecha.
     * POST /simulate
     */
    @PostMapping("/simulate")
    public SimulationResult simulate(@Valid @RequestBody SimulateRequest request) {
        log.info("Simulando evaluación para usuario {} en fecha {}", 
                request.getUserId(), request.getDate());

        return rulesEngineService.evaluateUser(
                request.getUserId(),
                request.getDate(),
                request.getTenantId(),
                request.isDebug()
        );
    }

    /**
     * Obtiene features calculadas para un usuario (endpoint de debug).
     * GET /features?user_id=...&date=...
     */
    @GetMapping("/features")
    public Map<String, Map<String, Object>> getFeatures(
            @RequestParam("user_id") String userId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        
        log.debug("Obteniendo features para usuario {} en fecha {}", userId, date);
        return rulesEngineService.getFeatures(userId, date);
    }
}
