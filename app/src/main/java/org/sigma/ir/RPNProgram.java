package org.sigma.ir;

import org.sigma.semantics.SymbolTable;

import java.util.*;

/**
 * Represents a complete RPN intermediate representation program.
 *
 * Contains:
 * - A linear sequence of instructions
 * - A mapping from label names to instruction positions
 * - The symbol table from semantic analysis
 */
public class RPNProgram {
    private final List<RPNInstruction> instructions;
    private final Map<String, Integer> labelPositions;
    private final SymbolTable symbolTable;

    public RPNProgram(List<RPNInstruction> instructions, SymbolTable symbolTable) {
        this.instructions = new ArrayList<>(instructions);
        this.symbolTable = symbolTable;
        this.labelPositions = buildLabelPositions(instructions);
    }

    /**
     * Builds the label-to-position mapping.
     */
    private Map<String, Integer> buildLabelPositions(List<RPNInstruction> instructions) {
        Map<String, Integer> positions = new HashMap<>();
        for (int i = 0; i < instructions.size(); i++) {
            RPNInstruction instr = instructions.get(i);
            if (instr.getOpcode() == RPNOpcode.LABEL) {
                String labelName = (String) instr.getOperand();
                if (positions.containsKey(labelName)) {
                    throw new IllegalArgumentException(
                        "Duplicate label: " + labelName + " at instruction " + i
                    );
                }
                positions.put(labelName, i);
            }
        }
        return positions;
    }

    public List<RPNInstruction> getInstructions() {
        return Collections.unmodifiableList(instructions);
    }

    public Map<String, Integer> getLabelPositions() {
        return Collections.unmodifiableMap(labelPositions);
    }

    public SymbolTable getSymbolTable() {
        return symbolTable;
    }

    /**
     * Returns the instruction at the given position.
     */
    public RPNInstruction getInstruction(int index) {
        return instructions.get(index);
    }

    /**
     * Returns the total number of instructions.
     */
    public int getInstructionCount() {
        return instructions.size();
    }

    /**
     * Returns the position of a label, or -1 if not found.
     */
    public int getLabelPosition(String labelName) {
        return labelPositions.getOrDefault(labelName, -1);
    }

    /**
     * Validates that all jump targets exist.
     * Returns a list of errors (empty if valid).
     */
    public List<String> validate() {
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < instructions.size(); i++) {
            RPNInstruction instr = instructions.get(i);
            RPNOpcode op = instr.getOpcode();

            // Check that jump targets exist
            if (op == RPNOpcode.JUMP || op == RPNOpcode.JUMP_IF_FALSE || op == RPNOpcode.JUMP_IF_TRUE) {
                String target = (String) instr.getOperand();
                if (!labelPositions.containsKey(target)) {
                    errors.add(String.format(
                        "Instruction %d: Jump to undefined label '%s'",
                        i, target
                    ));
                }
            }
        }

        return errors;
    }

    /**
     * Returns a human-readable visualization of the program.
     */
    public String visualize() {
        StringBuilder sb = new StringBuilder();
        sb.append("RPN Program:\n");
        sb.append("=" .repeat(70)).append("\n");

        int maxIndexWidth = String.valueOf(instructions.size() - 1).length();

        for (int i = 0; i < instructions.size(); i++) {
            RPNInstruction instr = instructions.get(i);

            // Format index
            String indexStr = String.format("%" + maxIndexWidth + "d", i);
            sb.append(indexStr).append(": ");

            // Indent non-label instructions
            if (instr.getOpcode() != RPNOpcode.LABEL) {
                sb.append("    ");
            }

            // Instruction
            sb.append(instr.toString());

            // Add source location as comment
            sb.append(String.format("  ; line %d", instr.getSourceLine()));

            sb.append("\n");
        }

        // Show label positions
        if (!labelPositions.isEmpty()) {
            sb.append("\n");
            sb.append("Label Positions:\n");
            sb.append("-".repeat(70)).append("\n");
            List<String> sortedLabels = new ArrayList<>(labelPositions.keySet());
            Collections.sort(sortedLabels);
            for (String label : sortedLabels) {
                sb.append(String.format("  %s -> instruction %d\n", label, labelPositions.get(label)));
            }
        }

        // Show validation errors
        List<String> errors = validate();
        if (!errors.isEmpty()) {
            sb.append("\n");
            sb.append("Validation Errors:\n");
            sb.append("-".repeat(70)).append("\n");
            for (String error : errors) {
                sb.append("  ").append(error).append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * Returns a compact representation of the program.
     */
    @Override
    public String toString() {
        return String.format("RPNProgram[%d instructions, %d labels]",
            instructions.size(), labelPositions.size());
    }

    /**
     * Builder class for constructing RPN programs incrementally.
     */
    public static class Builder {
        private final List<RPNInstruction> instructions = new ArrayList<>();
        private final SymbolTable symbolTable;

        public Builder(SymbolTable symbolTable) {
            this.symbolTable = symbolTable;
        }

        public Builder add(RPNInstruction instruction) {
            instructions.add(instruction);
            return this;
        }

        public Builder addAll(List<RPNInstruction> instructions) {
            this.instructions.addAll(instructions);
            return this;
        }

        public RPNProgram build() {
            return new RPNProgram(instructions, symbolTable);
        }

        public int getInstructionCount() {
            return instructions.size();
        }
    }
}
