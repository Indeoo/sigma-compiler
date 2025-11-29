import os
import argparse
from typing import Any
from textwrap import indent

class Console:
    def __init__(self):
        self.indent = 0

    def add_indent(self):
        self.indent += 3

    def remove_indent(self):
        if self.indent > 0:
            self.indent -= 3

    def print(self, *args):
        full_str = " ".join(map(str, args))
        indented_str = indent(full_str, '\t' * self.indent)
        print(indented_str)

console = Console()

class VirtualPostfixMachine:
    def __init__(self, module: str, mode: str = 'main', parent=None, symbolic_labels: bool = False):
        self.module = module
        self.root_module = None
        self.raw_tokens = []
        self.stack = []
        self.variable_types = {}
        self.variable_values = {}
        self.labels = {}
        self.functions = {}
        self.instructions = []
        self.pc = 0
        self.code_start_num = 0
        self.debug = False
        self.symbolic_labels = symbolic_labels
        
        self.mode = mode
        self.globals = []
        self.parent = parent
        self.enclosing_module = None
        if parent is None:
            self.root_module = self
        else:
            self.root_module = parent.root_module

    def _debug_print(self, msg: str):
        if self.debug:
            console.print(msg)

    def load_module(self):
        filename = self.module + '.postfix'
        try:
            with open(filename, 'r', encoding='utf-8') as file:
                lines = file.readlines()
        except FileNotFoundError:
            console.print(f"ПОМИЛКА: Файл модуля '{filename}' не знайдений.")
            exit(1)

        supported_tokens = (
            "int", "float", "bool", "string", "l-val", "r-val", "label", "colon", "assign_op", "math_op", "rel_op",
            "pow_op", "out_op", "inp_op", "conv", "bool_op", "cat_op", "stack_op", "colon", "jf", "jump", "CALL", "RET"
        )
        label_set = set()
        current_section = None
        for line in lines:
            line = line.strip()

            if not line or line.startswith("//"):
                continue

            # Section headers
            if line.startswith("."):
                current_section = line[1:].split('(')[0].strip()  # Extract section name
                continue

            if current_section == "vars":
                parts = line.split("//")[0].strip().rsplit(maxsplit=1)
                if parts[0] == ")" or parts[0] == "(":
                    continue

                if len(parts) != 2:
                    console.print(f"ПОМИЛКА: неправильна декларація змінної: {line}")
                    exit(1)
                variable_name = parts[0].strip()
                variable_type = parts[1].strip()
                self.variable_types[variable_name] = variable_type

            elif not self.symbolic_labels and current_section == "labels":
                parts = line.split("//")[0].strip().rsplit(maxsplit=1)
                if parts[0] == ")" or parts[0] == "(":
                    continue
                if len(parts) != 2:
                    console.print(f"ПОМИЛКА: неправильна декларація мітки: {line}")
                    exit(1)

                label_name = parts[0].strip()
                if label_name in label_set:
                    console.print(f"ПОМИЛКА: Повторне оголошення мітки: {line}")
                    exit(1)
                else:
                    label_set.add(label_name)
                
                try:
                    if (label_index := int(parts[1].strip())) < 0:
                        raise ValueError
                except ValueError:
                    console.print(f"ПОМИЛКА: Значення мітки має бути цілим додатнім числом: {line}")
                    exit(1)

                self.labels[label_name] = label_index

            elif current_section == "globVarList":
                # Each line in globVarList is the name of a global variable.
                variable_name = line.split("//")[0].strip()
                if variable_name not in (")", "(") and variable_name:
                    self.globals.append(variable_name)

            elif current_section == "funcs":
                parts = line.split("//")[0].strip().rsplit(maxsplit=2)
                if parts[0] == ")" or parts[0] == "(":
                    continue

                if len(parts) != 3:
                    console.print(f"ПОМИЛКА: неправильна декларація функції: {line}")
                    exit(1)
                function_name, function_type, n_params = parts[0], parts[1], int(parts[2])
                if function_type not in ("int", "float", "bool", "string", "void"):
                    console.print(f"ПОМИЛКА: неправильно вказаний тип функції {function_name}: {function_type}")
                    exit(1)
                self.functions[function_name] = (function_type, int(n_params))


            elif current_section == "code":
                parts = line.split("//")[0].strip().rsplit(maxsplit=1)
                if len(parts) == 1 and (parts[0] == ")" or parts[0] == "("):
                    continue
                if parts[0] == 'RET':
                    self.raw_tokens.append((parts[0], parts[0]))
                    continue

                if len(parts) != 2 or parts[1] not in supported_tokens:
                    console.print(f"ПОМИЛКА: непідтримувана інструкція: {line}")
                    exit(1)
                self.raw_tokens.append((parts[0], parts[1]))

        self.code_start_num = lines.index('.code(\n')+1
        # Preprocess tokens to extract labels
        self.extract_labels_from_code()
        
    def extract_labels_from_code(self):
        i = 0
        seen_labels = set()
        while i < len(self.raw_tokens):
            token, tok_type = self.raw_tokens[i]
            # Find labels and record their positions. 
            if tok_type == "label" and i + 1 < len(self.raw_tokens) and self.raw_tokens[i + 1][1] == "colon":
                seen_labels.add(token)
                if self.symbolic_labels:
                    self.labels[token] = i
                self._debug_print(f"Знайдено мітку: {token}. Індекс інструкції {i}; рядок {i+1+self.code_start_num}; модуль {self.module} ")
                self.instructions.append((token, tok_type))
            else:
                if tok_type == "string":
                    if token.startswith('"') and token.endswith('"'):
                        token = token[1:-1]
                    else:
                        console.print(f"\nПОМИЛКА: рядковий літерал {token} повинен бути огорнутий подвійними лапками. Індекс інструкції {i}; рядок {i+1+self.code_start_num}; модуль {self.module} ")
                        exit(1)
                elif tok_type == 'float':
                    try:
                        token = float(token)
                    except ValueError:
                        console.print(f"\nПОМИЛКА: неправильне значення для float: {token}. Індекс інструкції {i}; рядок {i+1+self.code_start_num}; модуль {self.module}")
                        exit(1)
                elif tok_type == 'int':
                    try:
                        token = int(token)
                    except ValueError:
                        console.print(f"\nПОМИЛКА: неправильне значення для int: {token}. Індекс інструкції {i}; рядок {i+1+self.code_start_num}; модуль {self.module}")
                        exit(1)
                elif tok_type == 'bool':
                    if token.upper() == 'TRUE':
                        token = True
                    elif token.upper() == 'FALSE':
                        token = False
                    else:
                        console.print(f"\nПОМИЛКА: неправильне значення для bool: {token}. Індекс інструкції {i}; рядок {i+1+self.code_start_num}; модуль {self.module}")
                        exit(1)
                self.instructions.append((token, tok_type))
            i += 1
    
    def _get_var_type(self, var: str) -> str:
        if self.mode == 'main':
            if var in self.variable_types:
                return self.variable_types[var]
        else:
            if var in self.variable_types:
                return self.variable_types[var]
            elif var in self.globals:
                return self.root_module._get_var_type(var)
            elif self.enclosing_module:
                return self.enclosing_module._get_var_type(var)

        console.print(f"\nПОМИЛКА: Невідома змінна: {var}; рядок {self.pc -1 + self.code_start_num}, модуль {self.module}")
        exit(1)

    def _get_value(self, var: str) -> (Any, str):
        if self.mode == 'main':
            if var in self.variable_types:
                value = self.variable_values.get(var)
                type = self.variable_types[var]
                if value is None:
                    console.print(f"\nПОМИЛКА: Використання неініціалізованої змінної: {var}; рядок {self.pc -1 + self.code_start_num}, модуль {self.module}")
                    exit(1)
                return value, type
        else:
            if var in self.variable_types:
                value = self.variable_values.get(var)
                type = self.variable_types[var]
                if value is None:
                    console.print(f"\nПОМИЛКА: Використання неініціалізованої змінної: {var}; рядок {self.pc -1 + self.code_start_num}, модуль {self.module}")
                    exit(1)
                return value, type
            elif var in self.globals:
                return self.root_module._get_value(var)
            elif self.enclosing_module:
                return self.enclosing_module._get_value(var)

        console.print(f"\nПОМИЛКА: Невідома змінна: {var}; рядок {self.pc -1 + self.code_start_num}, модуль {self.module}")
        exit(1)

    def _set_value(self, var: str, value: Any):
        if self.mode == 'main':
            if var in self.variable_types:
                self.variable_values[var] = value
                return
        else:
            if var in self.variable_types:
                self.variable_values[var] = value
                return
            elif var in self.globals:
                self.root_module._set_value(var, value)
                return
            elif self.enclosing_module:
                self.enclosing_module._set_value(var, value)
                return

        console.print(f"\nПОМИЛКА: Невідома змінна: {var}; рядок {self.pc - 1 + self.code_start_num}, модуль {self.module}")
        exit(1)

    def _get_1_operand(self, op: str) -> (Any, str):
        try:
            lexeme, token = self.stack.pop()
        except IndexError:
            console.print(f"\nПОМИЛКА: Недостатньо операндів для операції {op}")
            exit(1)

        if token == "r-val":
            lexeme, token = self._get_value(lexeme)

        return lexeme, token

    def _get_2_operands(self, op: str) -> (Any, str, Any, str):
        try:
            r_lexeme, r_token = self.stack.pop()
            l_lexeme, l_token = self.stack.pop()
        except IndexError:
            console.print(f"\nПОМИЛКА: Недостатньо операндів для операції {op}")
            exit(1)

        if l_token == "r-val":
            l_lexeme, l_token = self._get_value(l_lexeme)

        if r_token == "r-val":
            r_lexeme, r_token = self._get_value(r_lexeme)

        return l_lexeme, l_token, r_lexeme, r_token

    def run(self, debug: bool = False):
        if debug:
            self.debug = True

        if self.debug:
            console.print("Список інструкцій:")
            for i, (t, typ) in enumerate(self.instructions):
                console.print(f"  {i}: {t} {typ}")
            console.print("Таблиця міток:", self.labels)

        while self.pc < len(self.instructions):
            token, tok_type = self.instructions[self.pc]
            self._debug_print(f"\n[PC={self.pc}] Обробка інструкції: {token} {tok_type}")

            if tok_type == "assign_op":
                self._do_assign()
            elif tok_type == "math_op" or tok_type == "pow_op":
                self._do_math(token)
            elif tok_type == "rel_op":
                self._do_relational(token)
            elif tok_type == "out_op":
                self._do_out()
            elif tok_type == "inp_op":
                self._do_input()
            elif tok_type == "conv":
                self._convert_type(token)
            elif tok_type == "bool_op":
                self._do_logical(token)
            elif tok_type == "cat_op":
                self._do_cat(token)
            elif tok_type == "stack_op":
                self._do_stack(token)
            elif tok_type == "colon":
                self._do_colon()
            elif tok_type == "jf":
                self._do_jump_if_false()
                continue  # jump already updated PC
            elif tok_type == "jump":
                self._do_jump()
                continue  # jump already updated PC
            elif tok_type == "CALL":
                console.add_indent()
                self._call_func(token)
            elif token == "RET":
                self._func_return(token)
                console.remove_indent()
                break
            else:
                self.stack.append((token, tok_type))
            self._debug_print(f"  Стек після виконання: {self.stack}")
            self._debug_print(f"  Змінні: {self.variable_values}")
            self._debug_print(f"  Глобальні змінні: {self.globals}")
            self.pc += 1


    def _do_assign(self):
        # Assignment: right operand (value) is popped first, then the left operand (variable_values reference)

        r_lexeme, r_token = self.stack.pop()
        l_lexeme, l_token = self.stack.pop()
        if l_token != "l-val":
            console.print(f"\nПОМИЛКА: Неможливо присвоїти не l-val значення: {l_lexeme} ({l_token})")
            exit(1)

        if r_token == "r-val":
            r_lexeme, r_token = self._get_value(r_lexeme)
            if r_token != self.variable_types[l_lexeme]:
                console.print(f"\nПОМИЛКА: Невідповідність типів у присвоєнні: {l_lexeme} ({self.variable_types[l_lexeme]}) та {r_lexeme} ({r_token}); рядок {self.pc + self.code_start_num}, модуль {self.module}")
                exit(1)
            self._set_value(l_lexeme, r_lexeme)
        else:
            try:
                l_token = self._get_var_type(l_lexeme)
                if l_token != r_token:
                    console.print(f"\nПОМИЛКА: Невідповідність типів у присвоєнні: {l_lexeme} ({l_token}) та {r_lexeme} ({r_token}); рядок {self.pc + self.code_start_num}, модуль {self.module}")
                    exit(1)
                self._set_value(l_lexeme, r_lexeme)
            except KeyError:
                console.print(f"\nПОМИЛКА: Невідома змінна: {l_lexeme}; рядок {self.pc + self.code_start_num}, модуль {self.module}")
                exit(1)

    def _do_math(self, op: str):
        if op == "NEG":                         # unary minus
            lexeme, token = self._get_1_operand(op)

            if token not in ("int", "float"):
                console.print(f"\nПОМИЛКА: Унарний мінус може бути застосований лише до числових типів, не до {token}")
                exit(1)

            result = -lexeme
            self.stack.append((result, token))
            self._debug_print(f"  Застовування унарного мінуса: -({lexeme}) = {result}")
        else:                                   # binary operations

            l_lexeme, l_type, r_lexeme, r_type = self._get_2_operands(op)

            # Check that both operands have the same type.
            if l_type != r_type:
                console.print(f"\nПОМИЛКА: Невідповідність типів у арифметичній операції: {l_type} та {r_type}; рядок {self.pc + self.code_start_num}, модуль {self.module}")
                exit(1)
            if op == "^" and l_type != 'float' and r_type != 'float':
                console.print(f"\nПОМИЛКА: Піднесення до степеня (^) вимагає обох операндів типу float")
                exit(1)

            # performing calculations
            if op == "+":
                result = l_lexeme + r_lexeme
            elif op == "-":
                result = l_lexeme - r_lexeme
            elif op == "*":
                result = l_lexeme * r_lexeme
            elif op == "/":
                if r_lexeme == 0:
                    console.print("\nПОМИЛКА: Ділення на нуль")
                    exit(1)
                result = l_lexeme / r_lexeme
            elif op == "^":
                result = l_lexeme ** r_lexeme
            elif op == "%":
                if r_lexeme == 0:
                    console.print("\nПОМИЛКА: Ділення на нуль")
                    exit(1)
                result = l_lexeme % r_lexeme
            else:
                console.print(f"\nПОМИЛКА: Невідомий арифметичний оператор: {op}")
                exit(1)

            result_type = type(result).__name__.lower()

            self.stack.append((result, result_type))
            self._debug_print(f"  Обчислено вираз: {l_lexeme} {op} {r_lexeme} = {result}")

    def _do_relational(self, op: str):
        l_lexeme, l_type, r_lexeme, r_type = self._get_2_operands(op)

        if l_type not in ("int", "float", "bool") or r_type not in ("int", "float", "bool"):
            console.print(f"\nПОМИЛКА: Оператор відношення може бути застосований лише до чисел та типу bool, отримано типи: {l_type} і {r_type}")
            exit(1)

        if l_type != r_type:
            console.print(f"\nПОМИЛКА: Невідповідність типів у операції відношення: {l_type} та {r_type}")
            exit(1)

        if op == ">":
            result = l_lexeme > r_lexeme
        elif op == "<":
            result = l_lexeme < r_lexeme
        elif op == ">=":
            result = l_lexeme >= r_lexeme
        elif op == "<=":
            result = l_lexeme <= r_lexeme
        elif op == "!=":
            result = l_lexeme != r_lexeme
        elif op == "==":
            result = l_lexeme == r_lexeme
        else:
            console.print(f"\nПОМИЛКА: Невідомий оператор порівняння: {op}")
            exit(1)

        self.stack.append((result, "bool"))
        self._debug_print(f"  Обчислено відношення: {l_lexeme} {op} {r_lexeme} -> {result}")

    def _convert_type(self, op: str):
        value, token = self._get_1_operand(op)
        if op == "i2f":
            if token != "int":
                console.print(f"\nПОМИЛКА: Конвертація i2f може застосовуватись лише до int, не до {token}")
                exit(1)
            converted = float(value)
            self.stack.append((converted, "float"))
            self._debug_print(f"  Конвертація int у float: {value} -> {converted}")
        elif op == "f2i":
            if token != "float":
                console.print(f"\nПОМИЛКА: Конвертація f2i може застосовуватись лише до float, не до {token}")
                exit(1)
            converted = int(value)
            self.stack.append((converted, "int"))
            self._debug_print(f"  Конвертація float у int: {value} -> {converted}")
        elif op == "i2s":
            if token != "int":
                console.print(f"\nПОМИЛКА: Конвертація i2s може застосовуватись лише до int, не до {token}")
                exit(1)
            converted = str(value)
            self.stack.append((converted, "string"))
            self._debug_print(f"  Конвертація int у string: {value} -> {converted}")
        elif op == "s2i":
            if token != "string":
                console.print(f"\nПОМИЛКА: Конвертація s2i може застосовуватись лише до string, не до {token}")
                exit(1)
            try:
                converted = int(value)
            except ValueError:
                console.print(f"\nПОМИЛКА: Неможливо конвертувати {value} у int")
                exit(1)
            self.stack.append((converted, "int"))
            self._debug_print(f"  Конвертація string у int: {value} -> {converted}")
        elif op == "f2s":
            if token != "float":
                console.print(f"\nПОМИЛКА: Конвертація f2s може застосовуватись лише до float, не до {token}")
                exit(1)
            converted = str(value)
            self.stack.append((converted, "string"))
            self._debug_print(f"  Конвертація float у string: {value} -> {converted}")
        elif op == "s2f":
            if token != "string":
                console.print(f"\nПОМИЛКА: Конвертація s2f може застосовуватись лише до string, не до {token}")
                exit(1)
            try:
                converted = float(value)
            except ValueError:
                console.print(f"\nПОМИЛКА: Неможливо конвертувати {value} у float")
                exit(1)
            self.stack.append((converted, "float"))
            self._debug_print(f"  Конвертація string у float: {value} -> {converted}")
        elif op == "i2b":
            if token != "int":
                console.print(f"\nПОМИЛКА: Конвертація i2b може застосовуватись лише до int, не до {token}")
                exit(1)
            converted = bool(value)
            self.stack.append((converted, "bool"))
            self._debug_print(f"  Конвертація int у bool: {value} -> {converted}")
        elif op == "b2i":
            if token != "bool":
                console.print(f"\nПОМИЛКА: Конвертація b2i може застосовуватись лише до bool, не до {token}")
                exit(1)
            converted = int(value)
            self.stack.append((converted, "int"))
            self._debug_print(f"  Конвертація bool у int: {value} -> {converted}")

    def _do_out(self):
        lexeme, token = self._get_1_operand("OUT")
        print(lexeme)
        self._debug_print(f"  Надруковано: {lexeme}")

    def _do_input(self):
        value = input(f": ")
        self.stack.append((value, "string"))
        self._debug_print(f"  Введено: {value}")

    def _do_colon(self):
        #label, _ = self.stack.pop()
        lexeme, token = self.stack.pop()
        if token != "label":
            console.print(f"\nПОМИЛКА: Очікувалась мітка для оператора 'colon', натомість знайдено: {lexeme}")
            exit(1)
        else:
            pass
            # self.pc += 1

        # self._debug_print(f"  Оператор JF: аргумент 1 - мітка {label}, аргумент 2 - логічне значення {condition}")
        
    def _do_jump_if_false(self):
        label, _ = self.stack.pop()
        lexeme, token = self._get_1_operand("JF")
        condition = lexeme              # already boolean. _get_1_operand converts string lexeme literal to bool

        self._debug_print(f"  Оператор JF: аргумент 1 - мітка {label}, аргумент 2 - логічне значення {condition}")
        if not condition:
            if label not in self.labels:
                console.print(f"\nПОМИЛКА: Невідома мітка для JF переходу: {label}")
                exit(1)
            self.pc = self.labels[label]
            self._debug_print(f"  Перехід до мітки '{label}' індекс інструкції {self.pc}.")
        else:
            self.pc += 1  # advance to the next instruction
            self._debug_print("  Перехід не здійснюється. Виконання наступної інструкції (без стрибка).")

    def _do_jump(self):
        # Unconditional jump: pop label operand and set pc.
        label, _ = self.stack.pop()
        if label not in self.labels:
            console.print(f"\nПОМИЛКА: Невідома мітка для JMP переходу: {label}")
            exit(1)
        self._debug_print(f"  Оператор JMP: аргумент 1 - мітка {label}")
        self.pc = self.labels[label]
        self._debug_print(f"  Безумовний перехід до мітки '{label}' індекс інструкції {self.pc}")

    def _do_logical(self, op: str):
        if op == "NOT":
            lexeme, token = self._get_1_operand(op)
            if token != "bool":
                console.print(f"\nПОМИЛКА: NOT може бути застосовано лише до булевих типів, отримано тип: {token}")
                exit(1)
            result = not (lexeme == True)
            self.stack.append((result, "bool"))
            self._debug_print(f"  Обчислено логічний вираз: NOT {lexeme} -> {result}")
        elif op in ("AND", "OR"):
            l_lexeme, l_type, r_lexeme, r_type = self._get_2_operands(op)

            if l_type != "bool" or r_type != "bool":
                console.print(f"\nПОМИЛКА: Логічна операція може бути застосована лише до булевих типів, отримано типи: {l_type} та {r_type}")
                exit(1)
            if op == "AND":
                result = l_lexeme == True and r_lexeme == True
            elif op == "OR":
                result = l_lexeme == True or r_lexeme == True

            self.stack.append((result, "bool"))     # noqa
            self._debug_print(f"  Обчислено логічний вираз: {l_lexeme} {op} {r_lexeme} -> {result}")
        else:
            console.print(f"\nПОМИЛКА: Невідомий логічний оператор: {op}")
            exit(1)

    def _do_cat(self, op: str):
        l_lexeme, l_type, r_lexeme, r_type = self._get_2_operands(op)

        if l_type != "string" or r_type != "string":
            console.print(f"\nПОМИЛКА: Конкатенація може бути застосована лише до рядків, отримано типи: {l_type} та {r_type}; рядок {self.pc + self.code_start_num}, модуль {self.module}")
            exit(1)

        result = str(l_lexeme) + str(r_lexeme)
        self.stack.append((result, "string"))
        self._debug_print(f"  Конкатенація рядків: {l_lexeme} + {r_lexeme} -> {result}")

    def _do_stack(self, op: str):
        if op == "POP":
            if not self.stack:
                console.print(f"\nПОМИЛКА: Неможливо POP — стек порожній")
                exit(1)
            lexeme, token = self.stack.pop()
            self._debug_print(f"  Витягнуто з стеку: {lexeme} ({token})")
        elif op == "DUP":
            if not self.stack:
                console.print(f"\nПОМИЛКА: Неможливо DUP — стек порожній")
                exit(1)
            lexeme, token = self.stack[-1]
            self.stack.append((lexeme, token))
            self._debug_print(f"  Копіювання верхнього елемента стеку: {lexeme} ({token})")
        elif op == "SWAP":
            if len(self.stack) < 2:
                console.print(f"\nПОМИЛКА: Неможливо SWAP — в стеку менше ніж два елементи")
                exit(1)
            lexeme1, token1 = self.stack.pop()
            lexeme2, token2 = self.stack.pop()
            self.stack.append((lexeme1, token1))
            self.stack.append((lexeme2, token2))
            self._debug_print(f"  Обмін верхніх двох елементів стеку: {lexeme1} ({token1}) <-> {lexeme2} ({token2})")
        elif op == "NOP":
            self._debug_print("   Нічого не робимо (NOP)")

    def _call_func(self, func_name: str):
        if func_name not in self.functions:
            console.print(f"\nПОМИЛКА: Невідома функція: {func_name}")
            exit(1)

        self._debug_print(f" Виклик функції: {func_name}")
        self._debug_print(f"----------------{func_name}--------------------")
        # Create a new function context.
        module_full_name = self.module.split('$')[0] + '$' + func_name

        func_executor = VirtualPostfixMachine(module_full_name, "func", self, self.symbolic_labels)
        # enclosing function scope (function in function); determining this by module names
        if func_name.startswith(self.module.split('$', 1)[-1]):
            func_executor.enclosing_module = self

        func_executor.load_module()

        ret_type, n_params = self.functions[func_name]
        if n_params > 0:
            stack_top = self.stack[-n_params:]
            self.stack = self.stack[:-n_params]
            for parameter_var, potential_value in zip(func_executor.variable_types, stack_top):
                parameter_var_type = func_executor.variable_types[parameter_var]
                potential_value_type = potential_value[1]
                if potential_value_type == "r-val":
                    potential_value, potential_value_type = self._get_value(potential_value[0])
                else:
                    potential_value = potential_value[0]

                if potential_value_type != parameter_var_type:
                    console.print(f"\nПОМИЛКА: Невідповідність типів параметра функції {func_name}: {parameter_var} ({parameter_var_type}) та {potential_value} ({potential_value_type})")
                    exit(1)
                func_executor.variable_values[parameter_var] = potential_value

        func_executor.run(debug=self.debug)

    def _func_return(self, token: str):
        if self.parent:
            parent_func_definition = self.parent.functions[self.module.split('$', 1)[1]]
            ret_type, _ = parent_func_definition
            if ret_type == "void":
                self._debug_print(f"  Функція {self.module.split('$', 1)[1]} не повертає значення (void)")
            else:
                lexeme, token = self._get_1_operand(token)
                if token != ret_type:
                    console.print(
                        f"\nПОМИЛКА: Невідповідність типів при поверненні значення функції: Очікувалося {ret_type}, отримано {token}")
                    exit(1)
                self.parent.stack.append((lexeme, token))
                self._debug_print(f"  Функція {self.module.split('$', 1)[1]} повертає значення: {lexeme} ({token})")
        else:
            console.print(f"\nПОМИЛКА: Невідома функція для повернення: {token}")
            exit(1)
        self._debug_print(f"------END----------{self.module.split('$', 1)[1]}--------------------\n")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Запуск PSM")
    parser.add_argument("-p", "--path", type=str, help="Шлях до папки з .postfix файлами", required=True)
    parser.add_argument("-m", "--module", type=str, help="Ім'я модуля для запуску", required=True)
    parser.add_argument("-d", "--debug", action="store_true", help="Режим налагодження")
    parser.add_argument('--symbolic-labels', action='store_true', help="Використовувати символічні мітки .postfix файлів")
    # parser.add_argument('--without-colon', action='store_true', help="Видалити послідовні інструкції 'label' та 'colon'у .postfix коді")

    args = parser.parse_args()
    if not os.path.isdir(args.path):
        console.print(f"Помилка при ініціалізації PSM: Папка '{args.path}' не знайдена.")
        exit(1)
    if not os.path.isfile(os.path.join(args.path, args.module + ".postfix")):
        console.print(f"Помилка при ініціалізації PSM: Файл '{args.module}.postfix' не знайдено в папці '{args.path}'.")
        exit(1)

    os.chdir(args.path)
    vm = VirtualPostfixMachine(args.module, symbolic_labels=args.symbolic_labels)
    vm.load_module()
    vm.run(debug=args.debug)
