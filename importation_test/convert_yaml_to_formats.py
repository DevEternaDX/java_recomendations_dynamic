#!/usr/bin/env python3
"""
Script para convertir archivos YAML de reglas a los formatos esperados por la UI
"""

import json
import csv
import re
from pathlib import Path

# Simple YAML parser para nuestro caso específico
def simple_yaml_load(content):
    """
    Parser YAML simple para nuestros archivos específicos
    """
    lines = content.strip().split('\n')
    result = {}
    current_key = None
    current_dict = {}
    indent_stack = []
    
    for line in lines:
        line = line.rstrip()
        if not line or line.strip().startswith('#'):
            continue
            
        # Calcular indentación
        indent = len(line) - len(line.lstrip())
        content = line.strip()
        
        if content.endswith(':') and not content.startswith('-'):
            # Es una clave
            key = content[:-1].strip()
            
            if indent == 0:
                # Clave de nivel superior
                if current_key and current_dict:
                    result[current_key] = current_dict
                current_key = key
                current_dict = {}
                indent_stack = [0]
            elif indent > 0:
                # Clave anidada
                if key in ['when', 'AND']:
                    if key not in current_dict:
                        current_dict[key] = {}
                    indent_stack.append(indent)
        else:
            # Es un valor
            if ':' in content:
                parts = content.split(':', 1)
                key = parts[0].strip()
                value = parts[1].strip()
                
                # Limpiar comillas y brackets
                if value.startswith('[') and value.endswith(']'):
                    # Es una lista
                    value = value[1:-1]
                    items = [item.strip().strip('"') for item in value.split(',')]
                    value = [item for item in items if item]
                elif value.startswith('"') and value.endswith('"'):
                    value = value[1:-1]
                
                # Determinar dónde colocar el valor basado en la indentación
                if len(indent_stack) > 1 and 'when' in current_dict:
                    if 'AND' in current_dict['when']:
                        current_dict['when']['AND'][key] = value
                    else:
                        if indent > indent_stack[-1]:
                            current_dict['when'][key] = value
                        else:
                            current_dict['when'][key] = value
                else:
                    current_dict[key] = value
    
    # Agregar la última regla
    if current_key and current_dict:
        result[current_key] = current_dict
    
    return result

def parse_condition(condition_str):
    """
    Convierte una condición string como "< 2500" a formato UI
    """
    condition_str = condition_str.strip()
    
    # Patrones para diferentes operadores
    patterns = [
        (r'^>=\s*(.+)$', '>='),
        (r'^<=\s*(.+)$', '<='),
        (r'^>\s*(.+)$', '>'),
        (r'^<\s*(.+)$', '<'),
        (r'^==\s*(.+)$', '=='),
        (r'^=\s*(.+)$', '=='),  # Normalizar = a ==
        (r'^(.+)$', '==')  # Por defecto
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
            
            return {
                'op': op,
                'value': value
            }
    
    # Fallback
    return {
        'op': '==',
        'value': condition_str
    }

def convert_yaml_logic_to_ui_format(yaml_logic):
    """
    Convierte la lógica YAML al formato esperado por la UI
    """
    conditions = []
    
    # Procesar la sección 'when'
    when_section = yaml_logic.get('when', {})
    
    for key, value in when_section.items():
        if key == 'AND':
            # Procesar condiciones AND anidadas
            if isinstance(value, dict):
                for sub_key, sub_value in value.items():
                    condition = parse_condition(str(sub_value))
                    conditions.append({
                        'var': sub_key,
                        'agg': 'current',
                        'op': condition['op'],
                        'value': condition['value']
                    })
        else:
            # Condición directa
            condition = parse_condition(str(value))
            conditions.append({
                'var': key,
                'agg': 'current',
                'op': condition['op'],
                'value': condition['value']
            })
    
    return {'all': conditions}

def convert_yaml_to_ui_rule(rule_id, rule_data):
    """
    Convierte una regla YAML al formato esperado por la UI
    """
    # Convertir lógica
    logic = convert_yaml_logic_to_ui_format(rule_data)
    
    # Obtener candidatos de mensajes
    candidates = rule_data.get('candidates', [])
    
    # Crear mensajes en formato UI
    messages = []
    for i, candidate in enumerate(candidates):
        messages.append({
            'text': f'Mensaje {i+1} para {rule_id}',
            'weight': 1,
            'active': True
        })
    
    # Si no hay candidatos, crear un mensaje por defecto
    if not messages:
        messages.append({
            'text': f'Mensaje por defecto para {rule_id}',
            'weight': 1,
            'active': True
        })
    
    return {
        'id': rule_id,
        'tenantId': 'default',
        'category': rule_data.get('category', 'general'),
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

def load_yaml_files():
    """
    Carga todos los archivos YAML y combina las reglas
    """
    all_rules = {}
    
    yaml_files = ['rules_basic.yaml', 'rules_pro.yaml']
    
    for yaml_file in yaml_files:
        file_path = Path(yaml_file)
        if file_path.exists():
            print(f"Cargando {yaml_file}...")
            with open(file_path, 'r', encoding='utf-8') as f:
                try:
                    content = f.read()
                    data = simple_yaml_load(content)
                    if data:
                        # Filtrar solo las reglas (omitir 'windows' y otras configuraciones)
                        for key, value in data.items():
                            if isinstance(value, dict) and 'when' in value:
                                all_rules[key] = value
                except Exception as e:
                    print(f"Error al cargar {yaml_file}: {e}")
        else:
            print(f"Archivo {yaml_file} no encontrado")
    
    return all_rules

def create_json_export(rules):
    """
    Crea el JSON para importación en la UI
    """
    json_rules = []
    
    for rule_id, rule_data in rules.items():
        ui_rule = convert_yaml_to_ui_rule(rule_id, rule_data)
        json_rules.append(ui_rule)
    
    return json_rules

def create_csv_export(rules):
    """
    Crea los datos CSV con toda la información necesaria
    """
    csv_data = []
    
    for rule_id, rule_data in rules.items():
        ui_rule = convert_yaml_to_ui_rule(rule_id, rule_data)
        
        # Serializar la lógica como JSON string para CSV
        logic_json = json.dumps(ui_rule['logic'])
        messages_json = json.dumps(ui_rule['messages'])
        tags_json = json.dumps(ui_rule['tags'])
        
        csv_row = {
            'id': ui_rule['id'],
            'tenant_id': ui_rule['tenantId'],
            'category': ui_rule['category'],
            'priority': ui_rule['priority'],
            'severity': ui_rule['severity'],
            'cooldown_days': ui_rule['cooldownDays'],
            'max_per_day': ui_rule['maxPerDay'],
            'enabled': ui_rule['enabled'],
            'tags': tags_json,
            'logic': logic_json,
            'locale': ui_rule['locale'],
            'messages': messages_json
        }
        
        csv_data.append(csv_row)
    
    return csv_data

def main():
    """
    Función principal
    """
    print("=== Conversor de YAML a formatos de UI ===")
    
    # Cargar reglas YAML
    print("\n1. Cargando archivos YAML...")
    rules = load_yaml_files()
    print(f"Cargadas {len(rules)} reglas")
    
    # Crear JSON
    print("\n2. Generando JSON para importación...")
    json_rules = create_json_export(rules)
    
    with open('rules_for_import.json', 'w', encoding='utf-8') as f:
        json.dump(json_rules, f, indent=2, ensure_ascii=False)
    
    print(f"JSON creado: rules_for_import.json ({len(json_rules)} reglas)")
    
    # Crear CSV
    print("\n3. Generando CSV completo...")
    csv_data = create_csv_export(rules)
    
    if csv_data:
        fieldnames = csv_data[0].keys()
        
        with open('rules_complete.csv', 'w', newline='', encoding='utf-8') as f:
            writer = csv.DictWriter(f, fieldnames=fieldnames)
            writer.writeheader()
            writer.writerows(csv_data)
        
        print(f"CSV creado: rules_complete.csv ({len(csv_data)} reglas)")
    
    # Mostrar ejemplo de regla convertida
    if json_rules:
        print("\n4. Ejemplo de regla convertida:")
        example = json_rules[0]
        print(f"ID: {example['id']}")
        print(f"Categoría: {example['category']}")
        print(f"Lógica: {json.dumps(example['logic'], indent=2)}")
        print(f"Mensajes: {len(example['messages'])} mensaje(s)")
    
    print("\n✅ Conversión completada!")
    print("\nArchivos generados:")
    print("- rules_for_import.json (para importar en UI)")
    print("- rules_complete.csv (CSV completo con toda la información)")

if __name__ == '__main__':
    main()
