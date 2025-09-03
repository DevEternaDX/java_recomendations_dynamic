package com.eterna.dx.rulesengine.dsl;

import java.util.List;
import java.util.Map;

/**
 * Interfaz base para todos los nodos del DSL de reglas.
 * Cada nodo puede evaluarse contra un conjunto de features.
 */
public interface Node {
    
    /**
     * Evalúa el nodo contra las features proporcionadas.
     * 
     * @param features Mapa de features con estructura: variable -> agregador -> valor
     * @param trace Lista para recopilar información de debug sobre la evaluación
     * @return true si la condición se cumple, false en caso contrario
     */
    boolean eval(Map<String, Map<String, Object>> features, List<Map<String, Object>> trace);
}
