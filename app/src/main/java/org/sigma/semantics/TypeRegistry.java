package org.sigma.semantics;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Registry for all types in the Sigma language.
 * Manages built-in types and user-defined types (classes).
 */
public class TypeRegistry {
    private final Map<String, SigmaType> types = new HashMap<>();

    // Built-in types (singleton instances)
    public static final SigmaType.PrimitiveType INT = new SigmaType.PrimitiveType("int", 4);
    public static final SigmaType.PrimitiveType DOUBLE = new SigmaType.PrimitiveType("double", 8);
    public static final SigmaType.PrimitiveType FLOAT = new SigmaType.PrimitiveType("float", 4);
    public static final SigmaType.PrimitiveType BOOLEAN = new SigmaType.PrimitiveType("boolean", 1);
    public static final SigmaType.ClassType STRING = new SigmaType.ClassType("String");
    public static final SigmaType.VoidType VOID = SigmaType.VoidType.getInstance();
    public static final SigmaType.NullType NULL = SigmaType.NullType.getInstance();
    public static final SigmaType.ErrorType ERROR = SigmaType.ErrorType.getInstance();

    public TypeRegistry() {
        registerBuiltins();
    }

    /**
     * Register all built-in types
     */
    private void registerBuiltins() {
        register(INT);
        register(DOUBLE);
        register(FLOAT);
        register(BOOLEAN);
        register(STRING);
        register(VOID);
        // Note: null and error types are not registered (they're special)
    }

    /**
     * Register a type in the registry
     */
    public void register(SigmaType type) {
        types.put(type.getName(), type);
    }

    /**
     * Resolve a type by name.
     * Returns ErrorType if not found.
     */
    public SigmaType resolve(String typeName) {
        SigmaType type = types.get(typeName);
        return type != null ? type : ERROR;
    }

    /**
     * Check if a type is registered
     */
    public boolean isDefined(String typeName) {
        return types.containsKey(typeName);
    }

    /**
     * Register a user-defined class type
     */
    public void registerClass(String className) {
        SigmaType.ClassType classType = new SigmaType.ClassType(className);
        register(classType);
    }

    /**
     * Get all registered type names
     */
    public Set<String> getTypeNames() {
        return types.keySet();
    }
}
