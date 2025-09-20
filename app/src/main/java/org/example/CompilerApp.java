package org.example;

import org.example.runner.SigmaRunner;

public class CompilerApp {

    public static void main(String[] args) {
        SigmaCompiler compiler = new SigmaCompiler();
        SigmaRunner runner = new SigmaRunner();

        if (args.length == 0) {
            // Interactive mode - simple example
            System.out.println("Sigma Compiler - Interactive Mode");
            String sampleCode = """
                int x = 10;
                println("Hello, Sigma!");
                """;

            // Step 1: Compile
            CompilationResult result = compiler.compile(sampleCode);

            // Step 2: Handle compilation result
            if (!result.isSuccessful()) {
                System.err.println("Compilation failed:");
                System.err.println(result.getAllMessagesAsString());
                return;
            }

            // Step 3: Execute (separate responsibility)
            try {
                runner.run(result);
            } catch (Exception e) {
                System.err.println("Execution failed: " + e.getMessage());
            }

        } else {
            // File mode
            for (String filename : args) {
                System.out.println("Compiling: " + filename);

                // Step 1: Compile file
                CompilationResult result = compiler.compileFile(filename);

                // Step 2: Handle compilation result
                if (!result.isSuccessful()) {
                    System.err.println("Compilation of " + filename + " failed:");
                    System.err.println(result.getAllMessagesAsString());
                    continue; // Try next file
                }

                // Step 3: Execute (separate responsibility)
                System.out.println("Executing: " + filename);
                try {
                    runner.run(result);
                } catch (Exception e) {
                    System.err.println("Execution of " + filename + " failed: " + e.getMessage());
                }
            }
        }
    }

}
