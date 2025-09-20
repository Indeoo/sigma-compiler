package org.example.interpreter;

import org.example.semantic.Symbol;
import org.example.semantic.SymbolTable;

import java.util.HashMap;
import java.util.Map;

/**
 * Runtime environment for variable storage during interpretation
 */
public class Environment {

    private Map<String, SigmaValue> variables;
    private Environment parent;
    private String environmentName;

    public Environment(String environmentName) {
        this.environmentName = environmentName;
        this.variables = new HashMap<>();
        this.parent = null;
    }

    public Environment(String environmentName, Environment parent) {
        this.environmentName = environmentName;
        this.variables = new HashMap<>();
        this.parent = parent;
    }

    /**
     * Define a variable in this environment
     */
    public void define(String name, SigmaValue value) {
        variables.put(name, value);
    }

    /**
     * Get a variable from this environment or parent environments
     */
    public SigmaValue get(String name) {
        if (variables.containsKey(name)) {
            return variables.get(name);
        }

        if (parent != null) {
            return parent.get(name);
        }

        throw new RuntimeException("Undefined variable '" + name + "'");
    }

    /**
     * Set a variable in this environment or parent environments
     */
    public void set(String name, SigmaValue value) {
        if (variables.containsKey(name)) {
            variables.put(name, value);
            return;
        }

        if (parent != null) {
            parent.set(name, value);
            return;
        }

        throw new RuntimeException("Undefined variable '" + name + "'");
    }

    /**
     * Check if a variable is defined in this environment (not parent)
     */
    public boolean isDefinedHere(String name) {
        return variables.containsKey(name);
    }

    /**
     * Check if a variable is defined in this environment or parent environments
     */
    public boolean isDefined(String name) {
        if (variables.containsKey(name)) {
            return true;
        }
        return parent != null && parent.isDefined(name);
    }

    public Environment getParent() {
        return parent;
    }

    public String getEnvironmentName() {
        return environmentName;
    }

    @Override
    public String toString() {
        return String.format("Environment{name='%s', variables=%d, hasParent=%s}",
                           environmentName, variables.size(), parent != null);
    }
}