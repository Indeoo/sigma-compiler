package org.sigma.semantics;

import org.junit.jupiter.api.Test;
import org.sigma.parser.Ast;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SemanticAnalyzerTest {

    @Test
    void testEmptyProgram() {
        SemanticAnalyzer analyzer = new SemanticAnalyzer();
        Ast.CompilationUnit ast = new Ast.CompilationUnit(new ArrayList<>());

        SemanticResult result = analyzer.analyze(ast);

        assertTrue(result.isSuccessful());
        assertEquals(0, result.getErrorCount());
    }

    @Test
    void testVariableDeclarationWithValidType() {
        SemanticAnalyzer analyzer = new SemanticAnalyzer();

        // int x = 5;
        Ast.VariableDeclaration varDecl = new Ast.VariableDeclaration(
            "int",
            "x",
            new Ast.IntLiteral(5, 0, 0),
            0, 0
        );
        List<Ast.Statement> stmts = new ArrayList<>();
        stmts.add(varDecl);
        Ast.CompilationUnit ast = new Ast.CompilationUnit(stmts);

        SemanticResult result = analyzer.analyze(ast);

        assertTrue(result.isSuccessful());
        assertEquals(0, result.getErrorCount());

        Symbol x = result.getSymbolTable().lookup("x");
        assertNotNull(x);
        assertEquals(TypeRegistry.INT, x.getType());
    }

    @Test
    void testConstantDeclarationRegisteredAsConstant() {
        SemanticAnalyzer analyzer = new SemanticAnalyzer();

        Ast.VariableDeclaration constDecl = new Ast.VariableDeclaration(
            "int",
            "MAX",
            new Ast.IntLiteral(5, 0, 0),
            0, 0,
            true
        );
        Ast.CompilationUnit ast = new Ast.CompilationUnit(List.of(constDecl));

        SemanticResult result = analyzer.analyze(ast);
        assertTrue(result.isSuccessful());

        Symbol max = result.getSymbolTable().lookup("MAX");
        assertNotNull(max);
        assertTrue(max.isConstant());
    }

    @Test
    void testVariableDeclarationTypeMismatch() {
        SemanticAnalyzer analyzer = new SemanticAnalyzer();

        // int x = "hello";
        Ast.VariableDeclaration varDecl = new Ast.VariableDeclaration(
            "int",
            "x",
            new Ast.StringLiteral("hello", 0, 0),
            0, 0
        );
        List<Ast.Statement> stmts = new ArrayList<>();
        stmts.add(varDecl);
        Ast.CompilationUnit ast = new Ast.CompilationUnit(stmts);

        SemanticResult result = analyzer.analyze(ast);

        assertFalse(result.isSuccessful());
        assertEquals(1, result.getErrorCount());
        assertTrue(result.getErrors().get(0).getMessage().contains("Cannot assign"));
    }

    @Test
    void testConstantReassignmentIsError() {
        SemanticAnalyzer analyzer = new SemanticAnalyzer();

        Ast.VariableDeclaration constDecl = new Ast.VariableDeclaration(
            "int",
            "CONST",
            new Ast.IntLiteral(1, 0, 0),
            0, 0,
            true
        );
        Ast.Assignment assign = new Ast.Assignment("CONST", new Ast.IntLiteral(2, 0, 0));

        Ast.CompilationUnit ast = new Ast.CompilationUnit(List.of(constDecl, assign));
        SemanticResult result = analyzer.analyze(ast);

        assertFalse(result.isSuccessful());
        assertTrue(result.getErrors().stream()
            .anyMatch(err -> err.getType() == SemanticError.SemanticErrorType.CONSTANT_REASSIGNMENT));
    }

    @Test
    void testConstantRequiresInitializer() {
        SemanticAnalyzer analyzer = new SemanticAnalyzer();

        Ast.VariableDeclaration constDecl = new Ast.VariableDeclaration(
            "int",
            "NO_INIT",
            null,
            0, 0,
            true
        );

        Ast.CompilationUnit ast = new Ast.CompilationUnit(List.of(constDecl));
        SemanticResult result = analyzer.analyze(ast);

        assertFalse(result.isSuccessful());
        assertTrue(result.getErrors().stream()
            .anyMatch(err -> err.getType() == SemanticError.SemanticErrorType.CONSTANT_WITHOUT_INITIALIZER));
    }

    @Test
    void testMethodCallArgumentTypeMismatch() {
        SemanticAnalyzer analyzer = new SemanticAnalyzer();

        Ast.Parameter parameter = new Ast.Parameter("int", "arg", 0, 0);
        Ast.ReturnStatement returnStmt = new Ast.ReturnStatement(
            new Ast.StringLiteral("arg", 0, 0),
            0,
            0
        );
        Ast.MethodDeclaration methodDecl = new Ast.MethodDeclaration(
            "String",
            "duplicateParameterMethod",
            List.of(parameter),
            new Ast.Block(List.of(returnStmt)),
            0,
            0
        );

        Ast.Call badCall = new Ast.Call(
            new Ast.Identifier("duplicateParameterMethod", 0, 0),
            List.of(new Ast.StringLiteral("1", 0, 0)),
            0,
            0
        );
        Ast.ExpressionStatement callStmt = new Ast.ExpressionStatement(badCall, 0, 0);

        Ast.CompilationUnit ast = new Ast.CompilationUnit(List.of(methodDecl, callStmt));
        SemanticResult result = analyzer.analyze(ast);

        assertFalse(result.isSuccessful());
        assertTrue(result.getErrors().stream()
            .anyMatch(err -> err.getType() == SemanticError.SemanticErrorType.TYPE_MISMATCH));
    }

    @Test
    void testVariableDeclarationWithInvalidType() {
        SemanticAnalyzer analyzer = new SemanticAnalyzer();

        // UnknownType x = 5;
        Ast.VariableDeclaration varDecl = new Ast.VariableDeclaration(
            "UnknownType",
            "x",
            new Ast.IntLiteral(5, 0, 0),
            0, 0
        );
        List<Ast.Statement> stmts = new ArrayList<>();
        stmts.add(varDecl);
        Ast.CompilationUnit ast = new Ast.CompilationUnit(stmts);

        SemanticResult result = analyzer.analyze(ast);

        assertFalse(result.isSuccessful());
        assertTrue(result.getErrors().get(0).getMessage().contains("Undefined type"));
    }

    @Test
    void testDuplicateVariableDeclaration() {
        SemanticAnalyzer analyzer = new SemanticAnalyzer();

        // int x = 5;
        // int x = 10;
        List<Ast.Statement> stmts = new ArrayList<>();
        stmts.add(new Ast.VariableDeclaration("int", "x", new Ast.IntLiteral(5, 0, 0), 0, 0));
        stmts.add(new Ast.VariableDeclaration("int", "x", new Ast.IntLiteral(10, 0, 0), 0, 0));
        Ast.CompilationUnit ast = new Ast.CompilationUnit(stmts);

        SemanticResult result = analyzer.analyze(ast);

        assertFalse(result.isSuccessful());
        assertTrue(result.getErrors().stream()
            .anyMatch(err -> err.getMessage().contains("already defined")));
    }

    @Test
    void testNumericWidening() {
        SemanticAnalyzer analyzer = new SemanticAnalyzer();

        // double x = 5; (int -> double widening)
        Ast.VariableDeclaration varDecl = new Ast.VariableDeclaration(
            "double",
            "x",
            new Ast.IntLiteral(5, 0, 0),
            0, 0
        );
        List<Ast.Statement> stmts = new ArrayList<>();
        stmts.add(varDecl);
        Ast.CompilationUnit ast = new Ast.CompilationUnit(stmts);

        SemanticResult result = analyzer.analyze(ast);

        assertTrue(result.isSuccessful());
        assertEquals(0, result.getErrorCount());
    }

    @Test
    void testBinaryExpressionAddition() {
        SemanticAnalyzer analyzer = new SemanticAnalyzer();

        // int x = 5 + 10;
        Ast.Binary binary = new Ast.Binary(
            "+",
            new Ast.IntLiteral(5, 0, 0),
            new Ast.IntLiteral(10, 0, 0),
            0, 0
        );
        Ast.VariableDeclaration varDecl = new Ast.VariableDeclaration("int", "x", binary, 0, 0);
        List<Ast.Statement> stmts = new ArrayList<>();
        stmts.add(varDecl);
        Ast.CompilationUnit ast = new Ast.CompilationUnit(stmts);

        SemanticResult result = analyzer.analyze(ast);

        assertTrue(result.isSuccessful());
        SigmaType exprType = result.getExpressionType(binary);
        assertEquals(TypeRegistry.INT, exprType);
    }

    @Test
    void testBinaryExpressionMixedTypes() {
        SemanticAnalyzer analyzer = new SemanticAnalyzer();

        // double x = 5 + 3.14; (int + double = double)
        Ast.Binary binary = new Ast.Binary(
            "+",
            new Ast.IntLiteral(5, 0, 0),
            new Ast.DoubleLiteral(3.14, 0, 0),
            0, 0
        );
        Ast.VariableDeclaration varDecl = new Ast.VariableDeclaration("double", "x", binary, 0, 0);
        List<Ast.Statement> stmts = new ArrayList<>();
        stmts.add(varDecl);
        Ast.CompilationUnit ast = new Ast.CompilationUnit(stmts);

        SemanticResult result = analyzer.analyze(ast);

        assertTrue(result.isSuccessful());
        SigmaType exprType = result.getExpressionType(binary);
        assertEquals(TypeRegistry.DOUBLE, exprType);
    }

    @Test
    void testBinaryExpressionInvalidTypes() {
        SemanticAnalyzer analyzer = new SemanticAnalyzer();

        // int x = 5 + "hello";
        Ast.Binary binary = new Ast.Binary(
            "+",
            new Ast.IntLiteral(5, 0, 0),
            new Ast.StringLiteral("hello", 0, 0),
            0, 0
        );
        Ast.VariableDeclaration varDecl = new Ast.VariableDeclaration("int", "x", binary, 0, 0);
        List<Ast.Statement> stmts = new ArrayList<>();
        stmts.add(varDecl);
        Ast.CompilationUnit ast = new Ast.CompilationUnit(stmts);

        SemanticResult result = analyzer.analyze(ast);

        assertFalse(result.isSuccessful());
        // Error can be either from binary operation or from type mismatch in assignment
        assertTrue(result.getErrorCount() > 0);
    }

    @Test
    void testLogicalExpression() {
        SemanticAnalyzer analyzer = new SemanticAnalyzer();

        // boolean x = true && false;
        Ast.Binary binary = new Ast.Binary(
            "&&",
            new Ast.BooleanLiteral(true),
            new Ast.BooleanLiteral(false),
            0, 0
        );
        Ast.VariableDeclaration varDecl = new Ast.VariableDeclaration("boolean", "x", binary, 0, 0);
        List<Ast.Statement> stmts = new ArrayList<>();
        stmts.add(varDecl);
        Ast.CompilationUnit ast = new Ast.CompilationUnit(stmts);

        SemanticResult result = analyzer.analyze(ast);

        assertTrue(result.isSuccessful());
        SigmaType exprType = result.getExpressionType(binary);
        assertEquals(TypeRegistry.BOOLEAN, exprType);
    }

    @Test
    void testUnaryNegation() {
        SemanticAnalyzer analyzer = new SemanticAnalyzer();

        // int x = -5;
        Ast.Unary unary = new Ast.Unary("-", new Ast.IntLiteral(5, 0, 0), 0, 0);
        Ast.VariableDeclaration varDecl = new Ast.VariableDeclaration("int", "x", unary, 0, 0);
        List<Ast.Statement> stmts = new ArrayList<>();
        stmts.add(varDecl);
        Ast.CompilationUnit ast = new Ast.CompilationUnit(stmts);

        SemanticResult result = analyzer.analyze(ast);

        assertTrue(result.isSuccessful());
        SigmaType exprType = result.getExpressionType(unary);
        assertEquals(TypeRegistry.INT, exprType);
    }

    @Test
    void testUnaryLogicalNot() {
        SemanticAnalyzer analyzer = new SemanticAnalyzer();

        // boolean x = !true;
        Ast.Unary unary = new Ast.Unary("!", new Ast.BooleanLiteral(true), 0, 0);
        Ast.VariableDeclaration varDecl = new Ast.VariableDeclaration("boolean", "x", unary, 0, 0);
        List<Ast.Statement> stmts = new ArrayList<>();
        stmts.add(varDecl);
        Ast.CompilationUnit ast = new Ast.CompilationUnit(stmts);

        SemanticResult result = analyzer.analyze(ast);

        assertTrue(result.isSuccessful());
        SigmaType exprType = result.getExpressionType(unary);
        assertEquals(TypeRegistry.BOOLEAN, exprType);
    }

    @Test
    void testUnaryInvalidType() {
        SemanticAnalyzer analyzer = new SemanticAnalyzer();

        // int x = -"hello";
        Ast.Unary unary = new Ast.Unary("-", new Ast.StringLiteral("hello", 0, 0), 0, 0);
        Ast.VariableDeclaration varDecl = new Ast.VariableDeclaration("int", "x", unary, 0, 0);
        List<Ast.Statement> stmts = new ArrayList<>();
        stmts.add(varDecl);
        Ast.CompilationUnit ast = new Ast.CompilationUnit(stmts);

        SemanticResult result = analyzer.analyze(ast);

        assertFalse(result.isSuccessful());
        assertTrue(result.getErrors().stream()
            .anyMatch(err -> err.getMessage().contains("numeric operand")));
    }

    @Test
    void testIfStatementWithValidCondition() {
        SemanticAnalyzer analyzer = new SemanticAnalyzer();

        // if (true) { int x = 5; }
        Ast.Block thenBlock = new Ast.Block(List.of(
            new Ast.VariableDeclaration("int", "x", new Ast.IntLiteral(5, 0, 0), 0, 0)
        ));
        Ast.IfStatement ifStmt = new Ast.IfStatement(
            new Ast.BooleanLiteral(true),
            thenBlock,
            null
        );
        List<Ast.Statement> stmts = new ArrayList<>();
        stmts.add(ifStmt);
        Ast.CompilationUnit ast = new Ast.CompilationUnit(stmts);

        SemanticResult result = analyzer.analyze(ast);

        assertTrue(result.isSuccessful());
    }

    @Test
    void testIfStatementWithNonBooleanCondition() {
        SemanticAnalyzer analyzer = new SemanticAnalyzer();

        // if (5) { int x = 5; }
        Ast.Block thenBlock = new Ast.Block(List.of(
            new Ast.VariableDeclaration("int", "x", new Ast.IntLiteral(5, 0, 0), 0, 0)
        ));
        Ast.IfStatement ifStmt = new Ast.IfStatement(
            new Ast.IntLiteral(5, 0, 0),
            thenBlock,
            null
        );
        List<Ast.Statement> stmts = new ArrayList<>();
        stmts.add(ifStmt);
        Ast.CompilationUnit ast = new Ast.CompilationUnit(stmts);

        SemanticResult result = analyzer.analyze(ast);

        assertFalse(result.isSuccessful());
        assertTrue(result.getErrors().stream()
            .anyMatch(err -> err.getMessage().contains("condition must be boolean")));
    }

    @Test
    void testWhileStatementWithValidCondition() {
        SemanticAnalyzer analyzer = new SemanticAnalyzer();

        // while (true) { int x = 5; }
        Ast.Block body = new Ast.Block(List.of(
            new Ast.VariableDeclaration("int", "x", new Ast.IntLiteral(5, 0, 0), 0, 0)
        ));
        Ast.WhileStatement whileStmt = new Ast.WhileStatement(
            new Ast.BooleanLiteral(true),
            body
        );
        List<Ast.Statement> stmts = new ArrayList<>();
        stmts.add(whileStmt);
        Ast.CompilationUnit ast = new Ast.CompilationUnit(stmts);

        SemanticResult result = analyzer.analyze(ast);

        assertTrue(result.isSuccessful());
    }

    @Test
    void testWhileStatementWithNonBooleanCondition() {
        SemanticAnalyzer analyzer = new SemanticAnalyzer();

        // while (5) { int x = 5; }
        Ast.Block body = new Ast.Block(List.of(
            new Ast.VariableDeclaration("int", "x", new Ast.IntLiteral(5, 0, 0), 0, 0)
        ));
        Ast.WhileStatement whileStmt = new Ast.WhileStatement(
            new Ast.IntLiteral(5, 0, 0),
            body
        );
        List<Ast.Statement> stmts = new ArrayList<>();
        stmts.add(whileStmt);
        Ast.CompilationUnit ast = new Ast.CompilationUnit(stmts);

        SemanticResult result = analyzer.analyze(ast);

        assertFalse(result.isSuccessful());
        assertTrue(result.getErrors().stream()
            .anyMatch(err -> err.getMessage().contains("condition must be boolean")));
    }

    @Test
    void testMethodDeclaration() {
        SemanticAnalyzer analyzer = new SemanticAnalyzer();

        // def int add(int a, int b) { return a + b; }
        List<Ast.Parameter> params = List.of(
            new Ast.Parameter("int", "a", 0, 0),
            new Ast.Parameter("int", "b", 0, 0)
        );
        Ast.Binary binary = new Ast.Binary(
            "+",
            new Ast.Identifier("a", 0, 0),
            new Ast.Identifier("b", 0, 0),
            0, 0
        );
        Ast.ReturnStatement returnStmt = new Ast.ReturnStatement(binary, 0, 0);
        Ast.Block body = new Ast.Block(List.of(returnStmt));
        Ast.MethodDeclaration methodDecl = new Ast.MethodDeclaration(
            "int",
            "add",
            params,
            body,
            0, 0
        );
        List<Ast.Statement> stmts = new ArrayList<>();
        stmts.add(methodDecl);
        Ast.CompilationUnit ast = new Ast.CompilationUnit(stmts);

        SemanticResult result = analyzer.analyze(ast);

        assertTrue(result.isSuccessful());
        Symbol addMethod = result.getSymbolTable().lookup("add");
        assertNotNull(addMethod);
        assertTrue(addMethod.isMethod());
    }

    @Test
    void testReturnTypeMismatch() {
        SemanticAnalyzer analyzer = new SemanticAnalyzer();

        // def int getValue() { return "hello"; }
        Ast.ReturnStatement returnStmt = new Ast.ReturnStatement(new Ast.StringLiteral("hello", 0, 0), 0, 0);
        Ast.Block body = new Ast.Block(List.of(returnStmt));
        Ast.MethodDeclaration methodDecl = new Ast.MethodDeclaration(
            "int",
            "getValue",
            new ArrayList<>(),
            body,
            0, 0
        );
        List<Ast.Statement> stmts = new ArrayList<>();
        stmts.add(methodDecl);
        Ast.CompilationUnit ast = new Ast.CompilationUnit(stmts);

        SemanticResult result = analyzer.analyze(ast);

        assertFalse(result.isSuccessful());
        assertTrue(result.getErrors().stream()
            .anyMatch(err -> err.getMessage().contains("doesn't match method return type")));
    }

    @Test
    void testVoidMethodWithReturn() {
        SemanticAnalyzer analyzer = new SemanticAnalyzer();

        // def void doSomething() { return; }
        Ast.ReturnStatement returnStmt = new Ast.ReturnStatement(null, 0, 0);
        Ast.Block body = new Ast.Block(List.of(returnStmt));
        Ast.MethodDeclaration methodDecl = new Ast.MethodDeclaration(
            "void",
            "doSomething",
            new ArrayList<>(),
            body,
            0, 0
        );
        List<Ast.Statement> stmts = new ArrayList<>();
        stmts.add(methodDecl);
        Ast.CompilationUnit ast = new Ast.CompilationUnit(stmts);

        SemanticResult result = analyzer.analyze(ast);

        assertTrue(result.isSuccessful());
    }

    @Test
    void testVoidMethodWithValueReturn() {
        SemanticAnalyzer analyzer = new SemanticAnalyzer();

        // def void doSomething() { return 5; }
        Ast.ReturnStatement returnStmt = new Ast.ReturnStatement(new Ast.IntLiteral(5, 0, 0), 0, 0);
        Ast.Block body = new Ast.Block(List.of(returnStmt));
        Ast.MethodDeclaration methodDecl = new Ast.MethodDeclaration(
            "void",
            "doSomething",
            new ArrayList<>(),
            body,
            0, 0
        );
        List<Ast.Statement> stmts = new ArrayList<>();
        stmts.add(methodDecl);
        Ast.CompilationUnit ast = new Ast.CompilationUnit(stmts);

        SemanticResult result = analyzer.analyze(ast);

        assertFalse(result.isSuccessful());
        assertTrue(result.getErrors().stream()
            .anyMatch(err -> err.getMessage().contains("doesn't match method return type")));
    }

    @Test
    void testClassDeclaration() {
        SemanticAnalyzer analyzer = new SemanticAnalyzer();

        // class Point { int x; int y; }
        List<Ast.Statement> members = List.of(
            new Ast.FieldDeclaration("int", "x", null, 0, 0),
            new Ast.FieldDeclaration("int", "y", null, 0, 0)
        );
        Ast.ClassDeclaration classDecl = new Ast.ClassDeclaration("Point", members, 0, 0);
        List<Ast.Statement> stmts = new ArrayList<>();
        stmts.add(classDecl);
        Ast.CompilationUnit ast = new Ast.CompilationUnit(stmts);

        SemanticResult result = analyzer.analyze(ast);

        assertTrue(result.isSuccessful());
        Symbol pointClass = result.getSymbolTable().lookup("Point");
        assertNotNull(pointClass);
        assertTrue(pointClass.isClass());
    }

    @Test
    void testInstanceMethodCanReadField() {
        SemanticAnalyzer analyzer = new SemanticAnalyzer();

        Ast.FieldDeclaration field = new Ast.FieldDeclaration("double", "pi", null, 0, 0);
        Ast.Identifier piRef = new Ast.Identifier("pi", 1, 0);
        Ast.Parameter radiusParam = new Ast.Parameter("double", "radius", 0, 0);
        Ast.Binary bodyExpr = new Ast.Binary("*", piRef, new Ast.Identifier("radius", 1, 2), 1, 0);
        Ast.ReturnStatement returnStatement = new Ast.ReturnStatement(bodyExpr, 1, 0);
        Ast.Block body = new Ast.Block(List.of(returnStatement));
        Ast.MethodDeclaration method = new Ast.MethodDeclaration(
            "double",
            "circleArea",
            List.of(radiusParam),
            body,
            0, 0
        );

        Ast.ClassDeclaration calculator = new Ast.ClassDeclaration(
            "Calculator",
            List.of(field, method),
            0, 0
        );

        Ast.CompilationUnit ast = new Ast.CompilationUnit(List.of(calculator));

        SemanticResult result = analyzer.analyze(ast);
        assertTrue(result.isSuccessful(), () -> "Semantic errors: " + result.getErrorCount());
        SigmaType identifierType = result.getExpressionType(piRef);
        assertEquals(TypeRegistry.DOUBLE, identifierType);
    }

    @Test
    void testScriptWrapperLocalsResolve() {
        SemanticAnalyzer analyzer = new SemanticAnalyzer();

        Ast.VariableDeclaration declA = new Ast.VariableDeclaration("int", "a", new Ast.IntLiteral(10, 0, 0), 0, 0);
        Ast.Identifier aRef = new Ast.Identifier("a", 1, 0);
        Ast.VariableDeclaration declB = new Ast.VariableDeclaration("int", "b",
            new Ast.Binary("+", aRef, new Ast.IntLiteral(5, 0, 0), 1, 0), 1, 0);

        Ast.Block body = new Ast.Block(List.of(declA, declB));
        Ast.MethodDeclaration run = new Ast.MethodDeclaration("void", "run", List.of(), body, 0, 0);
        Ast.ClassDeclaration script = new Ast.ClassDeclaration("Script", List.of(run), 0, 0);
        Ast.CompilationUnit ast = new Ast.CompilationUnit(List.of(script));

        SemanticResult result = analyzer.analyze(ast);
        assertTrue(result.isSuccessful(), result::getErrorsAsString);
        assertEquals(TypeRegistry.INT, result.getExpressionType(aRef));
    }

    @Test
    void testPrintStatementAcceptsString() {
        SemanticAnalyzer analyzer = new SemanticAnalyzer();
        Ast.PrintStatement printStmt = new Ast.PrintStatement(
            new Ast.StringLiteral("hello", 0, 0),
            0,
            0
        );
        Ast.CompilationUnit ast = new Ast.CompilationUnit(List.of(printStmt));

        SemanticResult result = analyzer.analyze(ast);
        assertTrue(result.isSuccessful());
    }

    @Test
    void testPrintStatementRejectsNonPrintableType() {
        SemanticAnalyzer analyzer = new SemanticAnalyzer();

        Ast.ClassDeclaration fooClass = new Ast.ClassDeclaration("Foo", List.of(), 0, 0);
        Ast.VariableDeclaration fooVar = new Ast.VariableDeclaration("Foo", "foo", null, 0, 0);
        Ast.PrintStatement printStmt = new Ast.PrintStatement(
            new Ast.Identifier("foo", 0, 0),
            0,
            0
        );

        Ast.CompilationUnit ast = new Ast.CompilationUnit(List.of(fooClass, fooVar, printStmt));
        SemanticResult result = analyzer.analyze(ast);

        assertFalse(result.isSuccessful());
        assertTrue(result.getErrors().stream()
            .anyMatch(err -> err.getMessage().contains("print(...)")));
    }
}
