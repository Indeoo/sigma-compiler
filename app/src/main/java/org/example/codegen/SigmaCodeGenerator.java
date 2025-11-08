package org.example.codegen;

/**
 * Clean stub SigmaCodeGenerator used when the ANTLR/ASM-based codegen is not available.
 * This class provides minimal methods so the rest of the build can compile and
 * the recursive-descent frontend can be exercised. It intentionally does not
 * generate real bytecode.
 */
public class SigmaCodeGenerator {
    public SigmaCodeGenerator() {}

    /**
     * Placeholder for bytecode generation. Returns null to indicate no bytecode
     * produced in this stub implementation.
     */
    public byte[] generateBytecode(Object ast) {
        return null;
    }

    /**
     * Return any errors collected during code generation. Stub returns empty list.
     */
    public java.util.List<String> getErrors() {
        return java.util.Collections.emptyList();
    }

    /**
     * Whether code generation succeeded. Always true for stub.
     */
    public boolean isSuccessful() {
        return true;
    }
}
