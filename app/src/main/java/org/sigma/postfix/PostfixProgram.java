package org.sigma.postfix;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a textual Postfix Machine program (.postfix file).
 */
public class PostfixProgram {
    private final Map<String, String> variables;
    private final Map<String, Integer> labels;
    private final List<PostfixInstruction> instructions;

    public PostfixProgram(Map<String, String> variables,
                          Map<String, Integer> labels,
                          List<PostfixInstruction> instructions) {
        this.variables = new LinkedHashMap<>(variables);
        this.labels = new LinkedHashMap<>(labels);
        this.instructions = List.copyOf(instructions);
    }

    public Map<String, String> getVariables() {
        return variables;
    }

    public Map<String, Integer> getLabels() {
        return labels;
    }

    public List<PostfixInstruction> getInstructions() {
        return instructions;
    }

    public String toText() {
        StringBuilder sb = new StringBuilder();
        sb.append(".target: Postfix Machine\n");
        sb.append(".version: 0.3\n\n");

        sb.append(".vars(\n");
        if (variables.isEmpty()) {
            sb.append(")\n\n");
        } else {
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                sb.append("\t").append(entry.getKey()).append("\t").append(entry.getValue()).append("\n");
            }
            sb.append(")\n\n");
        }

        sb.append(".labels(\n");
        if (labels.isEmpty()) {
            sb.append(")\n\n");
        } else {
            for (Map.Entry<String, Integer> entry : labels.entrySet()) {
                sb.append("\t").append(entry.getKey())
                  .append("\t")
                  .append(entry.getValue())
                  .append("\n");
            }
            sb.append(")\n\n");
        }

        sb.append(".code(\n");
        for (PostfixInstruction ins : instructions) {
            sb.append("\t")
              .append(ins.lexeme())
              .append("\t")
              .append(ins.tokenType())
              .append("\n");
        }
        sb.append(")\n");
        return sb.toString();
    }

    @Override
    public String toString() {
        return toText();
    }
}

record PostfixInstruction(String lexeme, String tokenType, boolean labelDefinition) {
    PostfixInstruction(String lexeme, String tokenType) {
        this(lexeme, tokenType, false);
    }
}
