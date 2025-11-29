package org.sigma.ir;

import org.sigma.semantics.SigmaType;

/**
 * Represents a single instruction in the RPN intermediate representation.
 *
 * Each instruction consists of:
 * - An opcode (the operation to perform)
 * - An optional operand (constant value, variable name, label, etc.)
 * - Type information from semantic analysis
 * - Source location for debugging
 */
public class RPNInstruction {
    private final RPNOpcode opcode;
    private final Object operand;       // null if opcode doesn't require an operand
    private final SigmaType type;       // Type information (may be null for non-expression instructions)
    private final int sourceLine;       // Line number in source code
    private final int sourceColumn;     // Column number in source code
    private int slotIndex = -1;         // JVM local variable slot index (-1 = not set)

    /**
     * Creates an instruction without an operand or type information.
     */
    public RPNInstruction(RPNOpcode opcode, int sourceLine, int sourceColumn) {
        this(opcode, null, null, sourceLine, sourceColumn);
    }

    /**
     * Creates an instruction with an operand but no type information.
     */
    public RPNInstruction(RPNOpcode opcode, Object operand, int sourceLine, int sourceColumn) {
        this(opcode, operand, null, sourceLine, sourceColumn);
    }

    /**
     * Creates a fully-specified instruction.
     */
    public RPNInstruction(RPNOpcode opcode, Object operand, SigmaType type, int sourceLine, int sourceColumn) {
        this.opcode = opcode;
        this.operand = operand;
        this.type = type;
        this.sourceLine = sourceLine;
        this.sourceColumn = sourceColumn;

        // Validate that operand is provided if required
        if (opcode.requiresOperand() && operand == null) {
            throw new IllegalArgumentException(
                "Opcode " + opcode + " requires an operand but none was provided"
            );
        }
    }

    public RPNOpcode getOpcode() {
        return opcode;
    }

    public Object getOperand() {
        return operand;
    }

    public SigmaType getType() {
        return type;
    }

    public int getSourceLine() {
        return sourceLine;
    }

    public int getSourceColumn() {
        return sourceColumn;
    }

    /**
     * Returns true if this instruction has an operand.
     */
    public boolean hasOperand() {
        return operand != null;
    }

    /**
     * Returns true if this instruction has type information.
     */
    public boolean hasType() {
        return type != null;
    }

    /**
     * Gets the JVM local variable slot index for LOAD/STORE instructions.
     *
     * @return the slot index, or -1 if not set
     */
    public int getSlotIndex() {
        return slotIndex;
    }

    /**
     * Sets the JVM local variable slot index for LOAD/STORE instructions.
     *
     * @param slotIndex the slot index (0, 1, 2, ...)
     */
    public void setSlotIndex(int slotIndex) {
        this.slotIndex = slotIndex;
    }

    /**
     * Returns true if this instruction has a slot index set.
     */
    public boolean hasSlotIndex() {
        return slotIndex >= 0;
    }

    /**
     * Gets the variable name from the operand (for LOAD/STORE instructions).
     * Returns null if operand is not a String.
     */
    public String getVariableName() {
        return (operand instanceof String) ? (String) operand : null;
    }

    /**
     * Returns a human-readable representation of this instruction.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(opcode.name());

        if (operand != null) {
            sb.append(" ");
            if (operand instanceof String) {
                sb.append("\"").append(operand).append("\"");
            } else {
                sb.append(operand);
            }
        }

        // Show slot index if set (for LOAD/STORE instructions)
        if (hasSlotIndex()) {
            sb.append(" [slot=").append(slotIndex).append("]");
        }

        if (type != null) {
            sb.append(" : ").append(type.getName());
        }

        return sb.toString();
    }

    /**
     * Returns a detailed representation including source location.
     */
    public String toDetailedString() {
        return String.format("%s (line %d, col %d)",
            toString(), sourceLine, sourceColumn);
    }

    /**
     * Creates a PUSH instruction for a constant value.
     */
    public static RPNInstruction push(Object value, SigmaType type, int line, int col) {
        return new RPNInstruction(RPNOpcode.PUSH, value, type, line, col);
    }

    /**
     * Creates a LOAD instruction for a variable.
     */
    public static RPNInstruction load(String varName, SigmaType type, int line, int col) {
        return new RPNInstruction(RPNOpcode.LOAD, varName, type, line, col);
    }

    /**
     * Creates a STORE instruction for a variable.
     */
    public static RPNInstruction store(String varName, SigmaType type, int line, int col) {
        return new RPNInstruction(RPNOpcode.STORE, varName, type, line, col);
    }

    /**
     * Creates a LABEL instruction.
     */
    public static RPNInstruction label(String labelName, int line, int col) {
        return new RPNInstruction(RPNOpcode.LABEL, labelName, line, col);
    }

    /**
     * Creates a JUMP instruction.
     */
    public static RPNInstruction jump(String labelName, int line, int col) {
        return new RPNInstruction(RPNOpcode.JUMP, labelName, line, col);
    }

    /**
     * Creates a JUMP_IF_FALSE instruction.
     */
    public static RPNInstruction jumpIfFalse(String labelName, int line, int col) {
        return new RPNInstruction(RPNOpcode.JUMP_IF_FALSE, labelName, line, col);
    }

    /**
     * Creates a JUMP_IF_TRUE instruction.
     */
    public static RPNInstruction jumpIfTrue(String labelName, int line, int col) {
        return new RPNInstruction(RPNOpcode.JUMP_IF_TRUE, labelName, line, col);
    }

    /**
     * Creates a CALL instruction.
     */
    public static RPNInstruction call(String methodName, int argCount, SigmaType returnType, int line, int col) {
        // Store both method name and arg count as a CallOperand
        CallOperand callOp = new CallOperand(methodName, argCount);
        return new RPNInstruction(RPNOpcode.CALL, callOp, returnType, line, col);
    }

    /**
     * Creates a simple arithmetic/logical instruction.
     */
    public static RPNInstruction simple(RPNOpcode opcode, SigmaType type, int line, int col) {
        if (opcode.requiresOperand()) {
            throw new IllegalArgumentException("Opcode " + opcode + " requires an operand");
        }
        return new RPNInstruction(opcode, null, type, line, col);
    }

    /**
     * Helper class to store CALL instruction operands.
     */
    public static class CallOperand {
        private final String methodName;
        private final int argCount;

        public CallOperand(String methodName, int argCount) {
            this.methodName = methodName;
            this.argCount = argCount;
        }

        public String getMethodName() {
            return methodName;
        }

        public int getArgCount() {
            return argCount;
        }

        @Override
        public String toString() {
            return methodName + "/" + argCount;
        }
    }
}
