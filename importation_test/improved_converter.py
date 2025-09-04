#!/usr/bin/env python3
"""
Conversor mejorado de YAML a formatos de UI
"""

import json
import csv
import re
from pathlib import Path

def parse_yaml_rule_section(content):
    """
    Parser específico para nuestros archivos YAML de reglas
    """
    lines = content.strip().split('\n')
    rules = {}
    current_rule = None
    current_rule_data = {}
    current_section = None
    
    for line in lines:
        line = line.rstrip()
        if not line or line.strip().startswith('#'):
            continue
            
        # Calcular indentación
        indent = len(line) - len(line.lstrip())
        content_line = line.strip()
        
        # Regla de nivel superior (sin indentación)
        if indent == 0 and content_line.endswith(':') and content_line.startswith('R-'):
            # Guardar regla anterior
            if current_rule and current_rule_data:
                rules[current_rule] = current_rule_data
            
            # Nueva regla
            current_rule = content_line[:-1]
            current_rule_data = {}
            current_section = None
        
        # Propiedades directas de la regla
        elif indent == 2 and ':' in content_line:
            key, value = content_line.split(':', 1)
            key = key.strip()
            value = value.strip()
            
            # Si es la sección when, cambiar el contexto
            if key == 'when':
                current_section = 'when'
                current_rule_data['when'] = {}
                continue
            
            # Procesar listas
            if value.startswith('[') and value.endswith(']'):
                value = value[1:-1]
                items = []
                for item in value.split(','):
                    item = item.strip().strip('"').strip("'")
                    if item:
                        items.append(item)
                value = items
            elif value.startswith('"') and value.endswith('"'):
                value = value[1:-1]
            elif value.startswith("'") and value.endswith("'"):
                value = value[1:-1]
            
            current_rule_data[key] = value
        
        # Condiciones dentro de when
        elif current_section == 'when' and indent >= 4 and ':' in content_line:
            key, value = content_line.split(':', 1)
            key = key.strip()
            value = value.strip()
            
            # Limpiar comillas
            if value.startswith('"') and value.endswith('"'):
                value = value[1:-1]
            elif value.startswith("'") and value.endswith("'"):
                value = value[1:-1]
            
            if key == 'AND':
                if 'AND' not in current_rule_data['when']:
                    current_rule_data['when']['AND'] = {}
            else:
                if 'AND' in current_rule_data['when']:
                    current_rule_data['when']['AND'][key] = value
                else:
                    current_rule_data['when'][key] = value
        
        # Condiciones AND anidadas
        elif current_section == 'when' and indent >= 6 and ':' in content_line:
            key, value = content_line.split(':', 1)
            key = key.strip()
            value = value.strip()
            
            # Limpiar comillas
            if value.startswith('"') and value.endswith('"'):
                value = value[1:-1]
            elif value.startswith("'") and value.endswith("'"):
                value = value[1:-1]
            
            if 'AND' not in current_rule_data['when']:
                current_rule_data['when']['AND'] = {}
            current_rule_data['when']['AND'][key] = value
    
    # Guardar última regla
    if current_rule and current_rule_data:
        rules[current_rule] = current_rule_data
    
    return rules

def parse_condition_value(condition_str):
    """
    Convierte una condición string como "< 2500" a operador y valor
    """
    condition_str = str(condition_str).strip()
    
    # Patrones para diferentes operadores
    patterns = [
        (r'^>=\s*(.+)$', '>='),
        (r'^<=\s*(.+)$', '<='),
        (r'^>\s*(.+)$', '>'),
        (r'^<\s*(.+)$', '<'),
        (r'^==\s*(.+)$', '=='),
        (r'^=\s*(.+)$', '=='),
    ]
    
    for pattern, op in patterns:
        match = re.match(pattern, condition_str)
        if match:
            value_str = match.group(1).strip()
            # Intentar convertir a número
            try:
                if '.' in value_str:
                    value = float(value_str)
                else:
                    value = int(value_str)
            except ValueError:
                value = value_str
            
            return op, value
    
    # Si no coincide con ningún patrón, asumir igualdad
    try:
        if '.' in condition_str:
            value = float(condition_str)
        else:
            value = int(condition_str)
    except ValueError:
        value = condition_str
    
    return '==', value

def convert_yaml_to_ui_logic(rule_data):
    """
    Convierte la lógica YAML al formato esperado por la UI
    """
    conditions = []
    
    if 'when' not in rule_data:
        return {'all': []}
    
    when_section = rule_data['when']
    
    # Procesar condiciones directas
    for key, value in when_section.items():
        if key == 'AND':
            # Procesar condiciones AND
            for and_key, and_value in value.items():
                op, parsed_value = parse_condition_value(and_value)
                conditions.append({
                    'var': and_key,
                    'agg': 'current',
                    'op': op,
                    'value': parsed_value
                })
        else:
            # Condición directa
            op, parsed_value = parse_condition_value(value)
            conditions.append({
                'var': key,
                'agg': 'current',
                'op': op,
                'value': parsed_value
            })
    
    return {'all': conditions}

def convert_yaml_to_ui_rule(rule_id, rule_data):
    """
    Convierte una regla YAML completa al formato UI
    """
    # Convertir lógica
    logic = convert_yaml_to_ui_logic(rule_data)
    
    # Obtener categoría
    category = rule_data.get('category', 'general')
    
    # Obtener candidatos de mensajes
    candidates = rule_data.get('candidates', [])
    
    # Crear mensajes
    messages = []
    for i, candidate in enumerate(candidates[:3]):  # Limitar a 3 mensajes
        messages.append({
            'text': f'Mensaje para {rule_id} - Variante {i+1}',
            'weight': 1,
            'active': True
        })
    
    # Si no hay candidatos, crear un mensaje por defecto
    if not messages:
        messages.append({
            'text': f'Recomendación para {rule_id}',
            'weight': 1,
            'active': True
        })
    
    return {
        'id': rule_id,
        'tenantId': 'default',
        'category': category,
        'priority': 50,
        'severity': 1,
        'cooldownDays': 0,
        'maxPerDay': 0,
        'enabled': True,
        'tags': [],
        'logic': logic,
        'locale': 'es-ES',
        'messages': messages
    }

def main():
    """
    Función principal mejorada
    """
    print("=== Conversor Mejorado de YAML a formatos de UI ===")
    
    # Cargar y procesar archivos YAML
    all_rules = {}
    yaml_files = ['rules_basic.yaml', 'rules_pro.yaml']
    
    for yaml_file in yaml_files:
        file_path = Path(yaml_file)
        if file_path.exists():
            print(f"Procesando {yaml_file}...")
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
                rules = parse_yaml_rule_section(content)
                all_rules.update(rules)
                print(f"  -> {len(rules)} reglas encontradas")
        else:
            print(f"Archivo {yaml_file} no encontrado")
    
    print(f"\nTotal de reglas cargadas: {len(all_rules)}")
    
    # Convertir a formato UI
    ui_rules = []
    for rule_id, rule_data in all_rules.items():
        ui_rule = convert_yaml_to_ui_rule(rule_id, rule_data)
        ui_rules.append(ui_rule)
    
    # Generar JSON
    with open('rules_ui_format.json', 'w', encoding='utf-8') as f:
        json.dump(ui_rules, f, indent=2, ensure_ascii=False)
    
    print(f"JSON creado: rules_ui_format.json ({len(ui_rules)} reglas)")
    
    # Generar CSV
    if ui_rules:
        csv_data = []
        for rule in ui_rules:
            csv_row = {
                'id': rule['id'],
                'tenant_id': rule['tenantId'],
                'category': rule['category'],
                'priority': rule['priority'],
                'severity': rule['severity'],
                'cooldown_days': rule['cooldownDays'],
                'max_per_day': rule['maxPerDay'],
                'enabled': rule['enabled'],
                'tags': json.dumps(rule['tags']),
                'logic': json.dumps(rule['logic']),
                'locale': rule['locale'],
                'messages': json.dumps(rule['messages'])
            }
            csv_data.append(csv_row)
        
        with open('rules_ui_format.csv', 'w', newline='', encoding='utf-8') as f:
            writer = csv.DictWriter(f, fieldnames=csv_data[0].keys())
            writer.writeheader()
            writer.writerows(csv_data)
        
        print(f"CSV creado: rules_ui_format.csv ({len(csv_data)} reglas)")
    
    # Mostrar ejemplo
    if ui_rules:
        print("\n=== Ejemplo de regla convertida ===")
        example = ui_rules[0]
        print(f"ID: {example['id']}")
        print(f"Categoría: {example['category']}")
        print(f"Lógica: {json.dumps(example['logic'], indent=2)}")
        print(f"Mensajes: {len(example['messages'])} mensaje(s)")
        
        # Mostrar algunas condiciones
        conditions = example['logic']['all']
        if conditions:
            print("Condiciones:")
            for cond in conditions[:3]:  # Mostrar solo las primeras 3
                print(f"  - {cond['var']} {cond['op']} {cond['value']}")
    
    print("\n✅ Conversión mejorada completada!")

if __name__ == '__main__':
    main()
