package com.eterna.dx.rulesengine.dsl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Parser para convertir JSON de lógica de reglas a objetos Node del DSL.
 */
@Component
@Slf4j
public class DSLParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Parsea un Map (JSON) a un Node del DSL.
     */
    public Node parseNode(Object json) {
        if (json == null) {
            throw new IllegalArgumentException("JSON de lógica no puede ser nulo");
        }

        if (!(json instanceof Map)) {
            throw new IllegalArgumentException("JSON de lógica debe ser un objeto");
        }

        Map<String, Object> map = (Map<String, Object>) json;

        // Determinar el tipo de nodo basado en las claves presentes
        if (map.containsKey("all")) {
            return parseAllCondition(map);
        } else if (map.containsKey("any")) {
            return parseAnyCondition(map);
        } else if (map.containsKey("none")) {
            return parseNoneCondition(map);
        } else if (map.containsKey("var") && map.containsKey("op")) {
            // Podría ser NumericCondition o RelativeCondition
            if (map.containsKey("value")) {
                return parseNumericCondition(map);
            } else if (map.containsKey("left") && map.containsKey("right")) {
                return parseRelativeCondition(map);
            } else {
                throw new IllegalArgumentException("Condición con 'var' y 'op' debe tener 'value' o 'left'/'right'");
            }
        } else {
            throw new IllegalArgumentException("Formato de lógica desconocido: " + map.keySet());
        }
    }

    private AllCondition parseAllCondition(Map<String, Object> map) {
        List<Object> children = (List<Object>) map.get("all");
        List<Node> childNodes = new ArrayList<>();
        
        for (Object child : children) {
            childNodes.add(parseNode(child));
        }
        
        return AllCondition.builder().all(childNodes).build();
    }

    private AnyCondition parseAnyCondition(Map<String, Object> map) {
        List<Object> children = (List<Object>) map.get("any");
        List<Node> childNodes = new ArrayList<>();
        
        for (Object child : children) {
            childNodes.add(parseNode(child));
        }
        
        return AnyCondition.builder().any(childNodes).build();
    }

    private NoneCondition parseNoneCondition(Map<String, Object> map) {
        List<Object> children = (List<Object>) map.get("none");
        List<Node> childNodes = new ArrayList<>();
        
        for (Object child : children) {
            childNodes.add(parseNode(child));
        }
        
        return NoneCondition.builder().none(childNodes).build();
    }

    private NumericCondition parseNumericCondition(Map<String, Object> map) {
        String var = (String) map.get("var");
        String agg = (String) map.getOrDefault("agg", "current");
        String op = (String) map.get("op");
        Object value = map.get("value");
        boolean required = (Boolean) map.getOrDefault("required", false);

        return NumericCondition.builder()
                .var(var)
                .agg(agg)
                .op(op)
                .value(value)
                .required(required)
                .build();
    }

    private RelativeCondition parseRelativeCondition(Map<String, Object> map) {
        String op = (String) map.get("op");
        boolean required = (Boolean) map.getOrDefault("required", false);

        Map<String, Object> leftMap = (Map<String, Object>) map.get("left");
        Map<String, Object> rightMap = (Map<String, Object>) map.get("right");

        RelativeCondition.VarRef left = parseVarRef(leftMap);
        RelativeCondition.VarRef right = parseVarRef(rightMap);

        return RelativeCondition.builder()
                .left(left)
                .op(op)
                .right(right)
                .required(required)
                .build();
    }

    private RelativeCondition.VarRef parseVarRef(Map<String, Object> map) {
        String var = (String) map.get("var");
        String agg = (String) map.getOrDefault("agg", "current");
        Double scale = null;
        
        if (map.containsKey("scale")) {
            Object scaleObj = map.get("scale");
            if (scaleObj instanceof Number) {
                scale = ((Number) scaleObj).doubleValue();
            }
        }

        return RelativeCondition.VarRef.builder()
                .var(var)
                .agg(agg)
                .scale(scale)
                .build();
    }

    /**
     * Parsea un JSON string a Node.
     */
    public Node parseFromJson(String jsonString) {
        try {
            Map<String, Object> map = objectMapper.readValue(jsonString, Map.class);
            return parseNode(map);
        } catch (Exception e) {
            log.error("Error parseando JSON de lógica: {}", jsonString, e);
            throw new IllegalArgumentException("Error parseando JSON de lógica: " + e.getMessage());
        }
    }
}
