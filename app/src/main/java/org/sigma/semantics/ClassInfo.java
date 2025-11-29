package org.sigma.semantics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Captures metadata for a user-defined class, including its fields and methods.
 * Semantic analysis uses this to resolve member access and to share information
 * with later compilation stages.
 */
public class ClassInfo {
    private final String name;
    private final SigmaType.ClassType classType;
    private final Map<String, Symbol> fields = new HashMap<>();
    private final Map<String, MethodInfo> methods = new HashMap<>();

    public ClassInfo(String name, SigmaType.ClassType classType) {
        this.name = name;
        this.classType = classType;
    }

    public String getName() {
        return name;
    }

    public SigmaType.ClassType getClassType() {
        return classType;
    }

    public void addField(String fieldName, SigmaType fieldType, int line, int col) {
        fields.put(fieldName, new Symbol(fieldName, fieldType, Symbol.SymbolKind.FIELD, line, col));
    }

    public Symbol getField(String fieldName) {
        return fields.get(fieldName);
    }

    public Map<String, Symbol> getFields() {
        return Collections.unmodifiableMap(fields);
    }

    public void addMethod(String methodName,
                          SigmaType returnType,
                          List<SigmaType> parameterTypes,
                          int line,
                          int col) {
        methods.put(methodName, new MethodInfo(methodName, returnType, parameterTypes, line, col));
    }

    public MethodInfo getMethod(String methodName) {
        return methods.get(methodName);
    }

    public Map<String, MethodInfo> getMethods() {
        return Collections.unmodifiableMap(methods);
    }

    /**
     * Method metadata used for validating member calls.
     */
    public static class MethodInfo {
        private final String name;
        private final SigmaType returnType;
        private final List<SigmaType> parameterTypes;
        private final int definitionLine;
        private final int definitionCol;

        public MethodInfo(String name,
                          SigmaType returnType,
                          List<SigmaType> parameterTypes,
                          int line,
                          int col) {
            this.name = name;
            this.returnType = returnType;
            this.parameterTypes = new ArrayList<>(parameterTypes);
            this.definitionLine = line;
            this.definitionCol = col;
        }

        public String getName() {
            return name;
        }

        public SigmaType getReturnType() {
            return returnType;
        }

        public List<SigmaType> getParameterTypes() {
            return Collections.unmodifiableList(parameterTypes);
        }

        public int getParameterCount() {
            return parameterTypes.size();
        }

        public int getDefinitionLine() {
            return definitionLine;
        }

        public int getDefinitionCol() {
            return definitionCol;
        }
    }
}
