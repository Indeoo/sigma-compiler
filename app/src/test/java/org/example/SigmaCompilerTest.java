package org.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * Comprehensive test suite for the Sigma compiler
 */
public class SigmaCompilerTest {

    private SigmaCompiler compiler;
    private ByteArrayOutputStream outputStream;
    private PrintStream originalOut;

    @BeforeEach
    void setUp() {
        compiler = new SigmaCompiler();
        // Capture System.out for testing print statements
        originalOut = System.out;
        outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
    }

    void tearDown() {
        System.setOut(originalOut);
    }

    @Test
    void testVariableDeclarations() {
        String code = """
            int x = 10;
            double y = 3.14;
            String greeting = "Hello, World!";
            boolean flag = true;
            """;

        assertDoesNotThrow(() -> compiler.compileAndRun(code));
    }

    @Test
    void testArithmeticOperations() {
        String code = """
            int a = 10;
            int b = 5;
            int sum = a + b;
            int diff = a - b;
            int product = a * b;
            int quotient = a / b;
            int remainder = a % b;

            println("Sum: " + sum);
            println("Difference: " + diff);
            println("Product: " + product);
            println("Quotient: " + quotient);
            println("Remainder: " + remainder);
            """;

        assertDoesNotThrow(() -> compiler.compileAndRun(code));
        tearDown();

        String output = outputStream.toString();
        assertTrue(output.contains("Sum: 15"));
        assertTrue(output.contains("Difference: 5"));
        assertTrue(output.contains("Product: 50"));
        assertTrue(output.contains("Quotient: 2"));
        assertTrue(output.contains("Remainder: 0"));
    }

    @Test
    void testStringOperations() {
        String code = """
            String firstName = "John";
            String lastName = "Doe";
            String fullName = firstName + " " + lastName;

            println("Full name: " + fullName);
            """;

        assertDoesNotThrow(() -> compiler.compileAndRun(code));
        tearDown();

        String output = outputStream.toString();
        assertTrue(output.contains("Full name: John Doe"));
    }

    @Test
    void testConditionalStatements() {
        String code = """
            int x = 10;

            if (x > 5) {
                println("x is greater than 5");
            } else {
                println("x is not greater than 5");
            }

            if (x < 0) {
                println("x is negative");
            } else {
                println("x is non-negative");
            }
            """;

        assertDoesNotThrow(() -> compiler.compileAndRun(code));
        tearDown();

        String output = outputStream.toString();
        assertTrue(output.contains("x is greater than 5"));
        assertTrue(output.contains("x is non-negative"));
    }

    @Test
    void testLoops() {
        String code = """
            int i = 0;

            while (i < 3) {
                println("Count: " + i);
                i = i + 1;
            }
            """;

        assertDoesNotThrow(() -> compiler.compileAndRun(code));
        tearDown();

        String output = outputStream.toString();
        assertTrue(output.contains("Count: 0"));
        assertTrue(output.contains("Count: 1"));
        assertTrue(output.contains("Count: 2"));
    }

    @Test
    void testMethodDeclaration() {
        String code = """
            int add(int a, int b) {
                return a + b;
            }

            int result = add(5, 3);
            println("Result: " + result);
            """;

        assertDoesNotThrow(() -> compiler.compileAndRun(code));
        tearDown();

        String output = outputStream.toString();
        assertTrue(output.contains("Result: 8"));
    }

    @Test
    void testFactorialFunction() {
        String code = """
            int factorial(int n) {
                if (n <= 1) {
                    return 1;
                } else {
                    return n * factorial(n - 1);
                }
            }

            int result = factorial(5);
            println("Factorial of 5: " + result);
            """;

        assertDoesNotThrow(() -> compiler.compileAndRun(code));
        tearDown();

        String output = outputStream.toString();
        assertTrue(output.contains("Factorial of 5: 120"));
    }

    @Test
    void testBooleanOperations() {
        String code = """
            boolean a = true;
            boolean b = false;

            boolean andResult = a && b;
            boolean orResult = a || b;
            boolean notResult = !a;

            println("AND: " + andResult);
            println("OR: " + orResult);
            println("NOT: " + notResult);
            """;

        assertDoesNotThrow(() -> compiler.compileAndRun(code));
        tearDown();

        String output = outputStream.toString();
        assertTrue(output.contains("AND: false"));
        assertTrue(output.contains("OR: true"));
        assertTrue(output.contains("NOT: false"));
    }

    @Test
    void testComparisonOperations() {
        String code = """
            int x = 10;
            int y = 20;

            boolean less = x < y;
            boolean greater = x > y;
            boolean equal = x == y;
            boolean notEqual = x != y;
            boolean lessEqual = x <= y;
            boolean greaterEqual = x >= y;

            println("Less: " + less);
            println("Greater: " + greater);
            println("Equal: " + equal);
            println("Not Equal: " + notEqual);
            println("Less or Equal: " + lessEqual);
            println("Greater or Equal: " + greaterEqual);
            """;

        assertDoesNotThrow(() -> compiler.compileAndRun(code));
        tearDown();

        String output = outputStream.toString();
        assertTrue(output.contains("Less: true"));
        assertTrue(output.contains("Greater: false"));
        assertTrue(output.contains("Equal: false"));
        assertTrue(output.contains("Not Equal: true"));
        assertTrue(output.contains("Less or Equal: true"));
        assertTrue(output.contains("Greater or Equal: false"));
    }

    @Test
    void testNullValues() {
        String code = """
            String nullString = null;

            if (nullString == null) {
                println("String is null");
            } else {
                println("String is not null");
            }
            """;

        assertDoesNotThrow(() -> compiler.compileAndRun(code));
        tearDown();

        String output = outputStream.toString();
        assertTrue(output.contains("String is null"));
    }

    @Test
    void testTypeConversion() {
        String code = """
            int intValue = 42;
            double doubleValue = 3.14;

            double result = intValue + doubleValue;
            println("Result: " + result);
            """;

        assertDoesNotThrow(() -> compiler.compileAndRun(code));
        tearDown();

        String output = outputStream.toString();
        assertTrue(output.contains("Result: 45.14"));
    }

    @Test
    void testComplexGroovyLikeProgram() {
        String code = """
            // Groovy-like program demonstrating various features

            // Variable declarations with type inference-like syntax
            String name = "Sigma";
            int version = 1;
            double pi = 3.14159;
            boolean isReady = true;

            // Method to calculate area of circle
            double circleArea(double radius) {
                return pi * radius * radius;
            }

            // Method to greet user
            void greetUser(String userName) {
                println("Hello, " + userName + "!");
                println("Welcome to Sigma version " + version);
            }

            // Main execution
            greetUser(name);

            double radius = 5.0;
            double area = circleArea(radius);

            println("Circle with radius " + radius + " has area: " + area);

            // Conditional logic
            if (area > 50.0) {
                println("That's a big circle!");
            } else {
                println("That's a small circle!");
            }

            // Loop example
            int count = 1;
            while (count <= 3) {
                println("Iteration " + count);
                count = count + 1;
            }
            """;

        assertDoesNotThrow(() -> compiler.compileAndRun(code));
        tearDown();

        String output = outputStream.toString();
        assertTrue(output.contains("Hello, Sigma!"));
        assertTrue(output.contains("Welcome to Sigma version 1"));
        assertTrue(output.contains("Circle with radius 5.0 has area:"));
        assertTrue(output.contains("That's a big circle!"));
        assertTrue(output.contains("Iteration 1"));
        assertTrue(output.contains("Iteration 2"));
        assertTrue(output.contains("Iteration 3"));
    }

    @Test
    void testErrorHandling() {
        // Test undefined variable
        String code1 = """
            int x = undefinedVariable;
            """;

        // This should not throw an exception but should report semantic errors
        assertDoesNotThrow(() -> compiler.compileAndRun(code1));

        // Test type mismatch
        String code2 = """
            int x = "string";
            """;

        assertDoesNotThrow(() -> compiler.compileAndRun(code2));
    }

    @Test
    void testNestedScopes() {
        String code = """
            int x = 10;

            if (x > 5) {
                int y = 20;
                println("x: " + x);
                println("y: " + y);

                if (y > 15) {
                    int z = 30;
                    println("z: " + z);
                }
            }

            println("Back to outer scope");
            """;

        assertDoesNotThrow(() -> compiler.compileAndRun(code));
        tearDown();

        String output = outputStream.toString();
        assertTrue(output.contains("x: 10"));
        assertTrue(output.contains("y: 20"));
        assertTrue(output.contains("z: 30"));
        assertTrue(output.contains("Back to outer scope"));
    }
}