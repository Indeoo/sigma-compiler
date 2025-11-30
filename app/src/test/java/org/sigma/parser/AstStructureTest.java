package org.sigma.parser;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to verify the new AST structure with proper nodes for methods, classes, and member access.
 */
public class AstStructureTest {

    private final SigmaParserWrapper parser = new SigmaParserWrapper();

    private Ast.CompilationUnit parseUnit(String code) {
        ParseResult result = parser.parse(code);
        assertTrue(result.isSuccessful(), "Parse should succeed");
        assertNotNull(result.getAst());
        return result.getAst();
    }

    private List<Ast.Statement> scriptStatements(Ast.CompilationUnit ast) {
        for (Ast.Statement stmt : ast.statements) {
            if (stmt instanceof Ast.ClassDeclaration) {
                Ast.ClassDeclaration cls = (Ast.ClassDeclaration) stmt;
                if ("Script".equals(cls.name)) {
                    for (Ast.Statement member : cls.members) {
                        if (member instanceof Ast.MethodDeclaration) {
                            Ast.MethodDeclaration method = (Ast.MethodDeclaration) member;
                            if ("run".equals(method.name)) {
                                return method.body.statements;
                            }
                        }
                    }
                }
            }
        }
        return ast.statements;
    }

    private Ast.ClassDeclaration findClass(Ast.CompilationUnit ast, String name) {
        for (Ast.Statement stmt : ast.statements) {
            if (stmt instanceof Ast.ClassDeclaration) {
                Ast.ClassDeclaration cls = (Ast.ClassDeclaration) stmt;
                if (name.equals(cls.name)) {
                    return cls;
                }
            }
        }
        fail("Class " + name + " not found");
        return null;
    }

    @Test
    void testMethodDeclarationStructure() {
        String code = """
            int add(int a, int b) {
                return a + b;
            }
            """;

        Ast.CompilationUnit ast = parseUnit(code);
        List<Ast.Statement> stmts = scriptStatements(ast);
        assertEquals(1, stmts.size(), "Should have 1 statement");

        Ast.Statement stmt = stmts.get(0);
        assertTrue(stmt instanceof Ast.MethodDeclaration, "Should be MethodDeclaration");

        Ast.MethodDeclaration method = (Ast.MethodDeclaration) stmt;
        assertEquals("int", method.returnType);
        assertEquals("add", method.name);
        assertEquals(2, method.parameters.size());

        // Check parameters
        Ast.Parameter param1 = method.parameters.get(0);
        assertEquals("int", param1.type);
        assertEquals("a", param1.name);

        Ast.Parameter param2 = method.parameters.get(1);
        assertEquals("int", param2.type);
        assertEquals("b", param2.name);

        // Check body exists
        assertNotNull(method.body);
        assertEquals(1, method.body.statements.size());
        assertTrue(method.body.statements.get(0) instanceof Ast.ReturnStatement);
    }

    @Test
    void testClassDeclarationStructure() {
        String code = """
            class Calculator {
                int add(int a, int b) {
                    return a + b;
                }
            }
            """;

        Ast.CompilationUnit ast = parseUnit(code);
        Ast.ClassDeclaration classDecl = findClass(ast, "Calculator");
        assertEquals("Calculator", classDecl.name);
        assertEquals(1, classDecl.members.size());

        // Check class contains method
        Ast.Statement member = classDecl.members.get(0);
        assertTrue(member instanceof Ast.MethodDeclaration, "Member should be MethodDeclaration");

        Ast.MethodDeclaration method = (Ast.MethodDeclaration) member;
        assertEquals("add", method.name);
    }

    @Test
    void testClassWithFieldsAndMethods() {
        String code = """
            class Circle {
                double pi = 3.14159;

                double area(double radius) {
                    return pi * radius * radius;
                }
            }
            """;

        Ast.CompilationUnit ast = parseUnit(code);
        Ast.ClassDeclaration classDecl = findClass(ast, "Circle");

        assertEquals("Circle", classDecl.name);
        assertEquals(2, classDecl.members.size());

        // First member should be field (variable declaration)
        Ast.Statement field = classDecl.members.get(0);
        assertTrue(field instanceof Ast.VariableDeclaration, "First member should be VariableDeclaration");
        Ast.VariableDeclaration fieldDecl = (Ast.VariableDeclaration) field;
        assertEquals("pi", fieldDecl.name);
        assertEquals("double", fieldDecl.typeName);

        // Second member should be method
        Ast.Statement method = classDecl.members.get(1);
        assertTrue(method instanceof Ast.MethodDeclaration, "Second member should be MethodDeclaration");
        Ast.MethodDeclaration methodDecl = (Ast.MethodDeclaration) method;
        assertEquals("area", methodDecl.name);
    }

    @Test
    void testMemberAccessStructure() {
        String code = """
            int x = obj.field;
            """;

        Ast.CompilationUnit ast = parseUnit(code);
        Ast.VariableDeclaration varDecl = (Ast.VariableDeclaration) scriptStatements(ast).get(0);

        assertNotNull(varDecl.init, "Should have initializer");
        assertTrue(varDecl.init instanceof Ast.MemberAccess, "Init should be MemberAccess");

        Ast.MemberAccess memberAccess = (Ast.MemberAccess) varDecl.init;
        assertTrue(memberAccess.object instanceof Ast.Identifier);
        assertEquals("obj", ((Ast.Identifier) memberAccess.object).name);
        assertEquals("field", memberAccess.memberName);
    }

    @Test
    void testChainedMemberAccess() {
        String code = """
            int x = obj.field.subfield;
            """;

        Ast.CompilationUnit ast = parseUnit(code);
        Ast.VariableDeclaration varDecl = (Ast.VariableDeclaration) scriptStatements(ast).get(0);

        assertNotNull(varDecl.init);
        assertTrue(varDecl.init instanceof Ast.MemberAccess, "Should be MemberAccess");

        Ast.MemberAccess outerAccess = (Ast.MemberAccess) varDecl.init;
        assertEquals("subfield", outerAccess.memberName);

        // Object should be another member access
        assertTrue(outerAccess.object instanceof Ast.MemberAccess);
        Ast.MemberAccess innerAccess = (Ast.MemberAccess) outerAccess.object;
        assertEquals("field", innerAccess.memberName);

        // Innermost object should be identifier
        assertTrue(innerAccess.object instanceof Ast.Identifier);
        assertEquals("obj", ((Ast.Identifier) innerAccess.object).name);
    }

    @Test
    void testMethodCallOnMemberAccess() {
        String code = """
            int result = obj.method();
            """;

        Ast.CompilationUnit ast = parseUnit(code);
        Ast.VariableDeclaration varDecl = (Ast.VariableDeclaration) scriptStatements(ast).get(0);

        assertNotNull(varDecl.init);
        assertTrue(varDecl.init instanceof Ast.Call, "Should be Call expression");

        Ast.Call call = (Ast.Call) varDecl.init;
        assertTrue(call.target instanceof Ast.MemberAccess, "Call target should be MemberAccess");

        Ast.MemberAccess memberAccess = (Ast.MemberAccess) call.target;
        assertEquals("method", memberAccess.memberName);
        assertTrue(memberAccess.object instanceof Ast.Identifier);
        assertEquals("obj", ((Ast.Identifier) memberAccess.object).name);
    }

    @Test
    void testMultipleMethodDeclarations() {
        String code = """
            int add(int a, int b) {
                return a + b;
            }

            void greet(String name) {
                println("Hello");
            }
            """;

        Ast.CompilationUnit ast = parseUnit(code);
        List<Ast.Statement> stmts = scriptStatements(ast);
        assertEquals(2, stmts.size());

        Ast.MethodDeclaration method1 = (Ast.MethodDeclaration) stmts.get(0);
        assertEquals("add", method1.name);
        assertEquals("int", method1.returnType);

        Ast.MethodDeclaration method2 = (Ast.MethodDeclaration) stmts.get(1);
        assertEquals("greet", method2.name);
        assertEquals("void", method2.returnType);
    }
}
