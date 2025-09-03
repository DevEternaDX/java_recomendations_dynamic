package com.eterna.dx.rulesengine.dsl;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;
import java.util.Map;

/**
 * Condición que compara dos referencias de variables.
 * Equivalente a RelativeLeaf en Python.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RelativeCondition implements Node {

    private VarRef left;
    
    private String op;
    
    private VarRef right;
    
    @Builder.Default
    private boolean required = false;

    @Override
    public boolean eval(Map<String, Map<String, Object>> features, List<Map<String, Object>> trace) {
        // Obtener valores de ambas referencias
        Object leftValue = getVarValue(left, features);
        Object rightValue = getVarValue(right, features);

        boolean result = false;
        
        try {
            if (leftValue == null || rightValue == null) {
                if (required) {
                    result = false;
                } else {
                    result = false; // Por defecto, valor nulo es false
                }
            } else {
                result = compare(op, leftValue, rightValue);
            }
        } catch (Exception e) {
            result = false;
        }

        // Agregar información de debug
        trace.add(Map.of(
            "type", "relative",
            "left", Map.of("var", left.getVar(), "agg", left.getAgg(), "value", leftValue),
            "right", Map.of("var", right.getVar(), "agg", right.getAgg(), "value", rightValue),
            "op", op,
            "result", result,
            "required", required
        ));

        return result;
    }

    /**
     * Obtiene el valor de una referencia de variable, aplicando escala si está definida.
     */
    private Object getVarValue(VarRef varRef, Map<String, Map<String, Object>> features) {
        if (varRef == null) return null;
        
        Object value = null;
        if (features.containsKey(varRef.getVar()) && features.get(varRef.getVar()).containsKey(varRef.getAgg())) {
            value = features.get(varRef.getVar()).get(varRef.getAgg());
        }
        
        if (value != null && varRef.getScale() != null) {
            try {
                Double doubleValue = toDouble(value);
                if (doubleValue != null) {
                    value = doubleValue * varRef.getScale();
                }
            } catch (Exception e) {
                // Ignorar errores de escala
            }
        }
        
        return value;
    }

    /**
     * Compara dos valores según el operador especificado.
     */
    private boolean compare(String op, Object left, Object right) {
        if (left == null || right == null) {
            return false;
        }

        try {
            switch (op) {
                case "<":
                    return toDouble(left) < toDouble(right);
                case "<=":
                    return toDouble(left) <= toDouble(right);
                case ">":
                    return toDouble(left) > toDouble(right);
                case ">=":
                    return toDouble(left) >= toDouble(right);
                case "==":
                    return toDouble(left).equals(toDouble(right));
                default:
                    return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Convierte un objeto a Double de forma segura.
     */
    private Double toDouble(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number) {
            return ((Number) obj).doubleValue();
        }
        if (obj instanceof String) {
            try {
                return Double.parseDouble((String) obj);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VarRef {
        private String var;
        
        @Builder.Default
        private String agg = "current";
        
        private Double scale;
    }
}
