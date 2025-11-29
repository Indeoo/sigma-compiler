package org.sigma.ir;

import org.sigma.semantics.SigmaType;
import org.sigma.syntax.parser.Ast;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages JVM local variable slot allocation for methods.
 *
 * The JVM uses numeric slots for local variables:
 * - Instance methods: slot 0 = "this", parameters start at slot 1
 * - Static methods: parameters start at slot 0
 * - double/long types occupy 2 consecutive slots
 * - Local variables allocated after parameters
 */
public class LocalVariableAllocator {
    private final boolean isInstanceMethod;
    private final Map<String, Integer> variableSlots;
    private int nextSlot;

    /**
     * Creates a new allocator for a method.
     *
     * @param isInstanceMethod true if this is an instance method (reserves slot 0 for "this")
     */
    public LocalVariableAllocator(boolean isInstanceMethod) {
        this.isInstanceMethod = isInstanceMethod;
        this.variableSlots = new HashMap<>();
        this.nextSlot = isInstanceMethod ? 1 : 0; // Reserve slot 0 for "this" if instance method

        // If instance method, explicitly map "this" to slot 0
        if (isInstanceMethod) {
            variableSlots.put("this", 0);
        }
    }

    /**
     * Allocates slots for method parameters in declaration order.
     * Must be called before allocating any local variables.
     *
     * @param parameters the method parameters
     */
    public void allocateParameters(List<Ast.Parameter> parameters) {
        for (Ast.Parameter param : parameters) {
            // Get parameter type from the type name string
            int slotWidth = getSlotWidthForTypeName(param.type);

            if (variableSlots.containsKey(param.name)) {
                throw new IllegalArgumentException("Duplicate parameter name: " + param.name);
            }

            variableSlots.put(param.name, nextSlot);
            nextSlot += slotWidth;
        }
    }

    /**
     * Allocates a slot for a local variable.
     *
     * @param varName the variable name
     * @param type the variable type
     * @return the allocated slot index
     * @throws IllegalArgumentException if variable already exists
     */
    public int allocateVariable(String varName, SigmaType type) {
        if (variableSlots.containsKey(varName)) {
            throw new IllegalArgumentException("Variable already allocated: " + varName);
        }

        int slot = nextSlot;
        variableSlots.put(varName, slot);
        nextSlot += getSlotWidth(type);

        return slot;
    }

    /**
     * Gets the slot index for a variable.
     *
     * @param varName the variable name
     * @return the slot index
     * @throws IllegalArgumentException if variable not found
     */
    public int getSlot(String varName) {
        Integer slot = variableSlots.get(varName);
        if (slot == null) {
            throw new IllegalArgumentException("Variable not found: " + varName);
        }
        return slot;
    }

    /**
     * Returns the maximum number of local variable slots used.
     * This value is needed for the JVM Code attribute.
     *
     * @return the max locals count
     */
    public int getMaxLocals() {
        return nextSlot;
    }

    /**
     * Checks if a variable has been allocated.
     *
     * @param varName the variable name
     * @return true if the variable has been allocated
     */
    public boolean hasVariable(String varName) {
        return variableSlots.containsKey(varName);
    }

    /**
     * Returns the number of slots a type occupies.
     * Most types occupy 1 slot, but double and long occupy 2.
     *
     * @param type the Sigma type
     * @return 1 or 2 depending on the type width
     */
    private int getSlotWidth(SigmaType type) {
        if (type instanceof SigmaType.PrimitiveType) {
            SigmaType.PrimitiveType primType = (SigmaType.PrimitiveType) type;
            String typeName = primType.getName();

            // double and long occupy 2 slots in JVM
            if ("double".equals(typeName) || "long".equals(typeName)) {
                return 2;
            }
        }

        // All other types (int, boolean, float, references) occupy 1 slot
        return 1;
    }

    /**
     * Returns the slot width for a type name string.
     * Used for parameter allocation where we have type names instead of SigmaType objects.
     *
     * @param typeName the type name (e.g., "int", "double", "String")
     * @return 1 or 2 depending on the type width
     */
    private int getSlotWidthForTypeName(String typeName) {
        if ("double".equals(typeName) || "long".equals(typeName)) {
            return 2;
        }
        return 1;
    }

    /**
     * Returns true if this allocator is for an instance method.
     */
    public boolean isInstanceMethod() {
        return isInstanceMethod;
    }

    /**
     * Returns a debug string showing all allocated variables.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("LocalVariableAllocator[");
        sb.append(isInstanceMethod ? "instance" : "static");
        sb.append(", maxLocals=").append(nextSlot);
        sb.append(", vars=").append(variableSlots);
        sb.append("]");
        return sb.toString();
    }
}
