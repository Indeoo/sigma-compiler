package org.sigma.transform;

import org.junit.jupiter.api.Test;
import org.sigma.parser.Ast;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ScriptWrappingTransformerTest {

    @Test
    void leavesPureClassProgramsAlone() {
        Ast.ClassDeclaration cls = new Ast.ClassDeclaration("Calculator", new ArrayList<>(), 0, 0);
        Ast.CompilationUnit unit = new Ast.CompilationUnit(List.of(cls));

        Ast.CompilationUnit wrapped = ScriptWrappingTransformer.wrap(unit);

        assertSame(unit, wrapped);
    }

    @Test
    void wrapsTopLevelStatementsIntoScriptClass() {
        Ast.VariableDeclaration var = new Ast.VariableDeclaration("int", "x", null, 0, 0);
        Ast.ExpressionStatement exprStmt = new Ast.ExpressionStatement(new Ast.Identifier("x", 0, 0), 0, 0);
        Ast.CompilationUnit unit = new Ast.CompilationUnit(List.of(var, exprStmt));

        Ast.CompilationUnit wrapped = ScriptWrappingTransformer.wrap(unit);

        assertNotSame(unit, wrapped);
        assertEquals(1, wrapped.statements.size());
        assertTrue(wrapped.statements.get(0) instanceof Ast.ClassDeclaration);
        Ast.ClassDeclaration scriptClass = (Ast.ClassDeclaration) wrapped.statements.get(0);
        assertEquals("Script", scriptClass.name);
        assertEquals(1, scriptClass.members.size());
        assertTrue(scriptClass.members.get(0) instanceof Ast.MethodDeclaration);
        Ast.MethodDeclaration method = (Ast.MethodDeclaration) scriptClass.members.get(0);
        assertEquals("run", method.name);
        assertEquals(2, method.body.statements.size());
    }
}
