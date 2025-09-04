package com.eterna.dx.rulesengine.controller;

import com.eterna.dx.rulesengine.dto.request.VariableRequest;
import com.eterna.dx.rulesengine.entity.Variable;
import com.eterna.dx.rulesengine.repository.VariableRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador para gestión de variables.
 * Equivalente a variables.py en FastAPI.
 */
@RestController
@RequestMapping("/variables")
@RequiredArgsConstructor
@Slf4j
public class VariableController {

    private final VariableRepository variableRepository;

    /**
     * Lista todas las variables.
     * GET /variables
     */
    @GetMapping
    public List<Variable> listVariables(@RequestParam(defaultValue = "default") String tenantId) {
        return variableRepository.findByTenantIdOrderByKeyAsc(tenantId);
    }

    /**
     * Crea o actualiza una variable (upsert).
     * POST /variables
     */
    @PostMapping
    public Map<String, String> createOrUpdateVariable(@Valid @RequestBody VariableRequest request) {
        try {
            Variable variable = variableRepository.findByKey(request.getKey())
                    .orElse(new Variable());

            // Actualizar campos
            variable.setKey(request.getKey());
            variable.setLabel(request.getLabel());
            variable.setDescription(request.getDescription());
            variable.setUnit(request.getUnit());
            variable.setType(request.getType());
            variable.setAllowedAggregators(request.getAllowedAggregators());
            variable.setValidMin(request.getValidMin());
            variable.setValidMax(request.getValidMax());
            variable.setMissingPolicy(request.getMissingPolicy());
            variable.setDecimals(request.getDecimals());
            variable.setCategory(request.getCategory());
            variable.setTenantId(request.getTenantId());
            variable.setExamples(request.getExamples());

            variableRepository.save(variable);

            log.info("Variable {} creada/actualizada exitosamente", request.getKey());
            return Map.of("key", request.getKey());

        } catch (Exception e) {
            log.error("Error creando/actualizando variable {}: {}", request.getKey(), e.getMessage());
            throw new RuntimeException("Error procesando variable: " + e.getMessage());
        }
    }
}

