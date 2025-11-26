package org.sigma;

import org.sigma.syntax.parser.RecursiveDescentParser;
import org.sigma.syntax.parser.RecursiveDescentParser.ParseAstResult;
import org.sigma.ast.Ast;

public class TestParser {
    public static void main(String[] args) {
        // Test valid cases
        String[] tests = {
            "2 + 3;",
            "int x = 10;",
            "if (x > 0) y = 1;",
            "while (x < 10) x = x + 1;"
        };

        System.out.println("========== VALID TEST CASES ==========");

        for (String test : tests) {
            System.out.println("\n=== Testing: " + test + " ===");
            ParseAstResult result = RecursiveDescentParser.parseToAst(test);

            if (result.ast != null) {
                System.out.println("✓ AST created successfully");
                System.out.println("  Statements: " + result.ast.statements.size());
            } else {
                System.out.println("✗ AST is null");
            }

            if (result.errors.isEmpty()) {
                System.out.println("✓ No errors");
            } else {
                System.out.println("✗ Errors:");
                for (String error : result.errors) {
                    System.out.println("  " + error);
                }
            }
        }

        // Test error cases
        System.out.println("\n\n========== ERROR TEST CASES ==========");
        String[] errorTests = {
            "2 +",           // incomplete expression
            "int x",         // missing semicolon
            "if x > 0 y = 1;", // missing parentheses
            "int 123x = 5;"  // invalid identifier
        };

        for (String test : errorTests) {
            System.out.println("\n=== Testing: " + test + " ===");
            ParseAstResult result = RecursiveDescentParser.parseToAst(test);

            if (result.ast != null) {
                System.out.println("  AST created (may be partial)");
            } else {
                System.out.println("  AST is null");
            }

            if (result.errors.isEmpty()) {
                System.out.println("✗ Expected errors but got none");
            } else {
                System.out.println("✓ Errors detected:");
                for (String error : result.errors) {
                    System.out.println("  " + error);
                }
            }
        }
    }
}
