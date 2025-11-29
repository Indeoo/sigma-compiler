package org.sigma.transform;

import org.sigma.parser.Ast;

import java.util.ArrayList;
import java.util.List;

/**
 * Wraps top-level (non-class) statements into a synthetic script class.
 */
public final class ScriptWrappingTransformer {
    private ScriptWrappingTransformer() {}

    private static final String SCRIPT_CLASS_NAME = "Script";
    private static final String SCRIPT_METHOD_NAME = "run";

    public static Ast.CompilationUnit wrap(Ast.CompilationUnit unit) {
        if (unit == null || unit.statements == null) {
            return unit;
        }

        List<Ast.Statement> classes = new ArrayList<>();
        List<Ast.Statement> scriptStatements = new ArrayList<>();

        for (Ast.Statement stmt : unit.statements) {
            if (stmt instanceof Ast.ClassDeclaration) {
                classes.add(stmt);
            } else {
                scriptStatements.add(stmt);
            }
        }

        if (scriptStatements.isEmpty()) {
            return unit;
        }

        Ast.Block body = new Ast.Block(scriptStatements);
        Ast.MethodDeclaration scriptMethod = new Ast.MethodDeclaration(
            "void",
            SCRIPT_METHOD_NAME,
            List.of(),
            body,
            0,
            0
        );

        List<Ast.Statement> members = new ArrayList<>();
        members.add(scriptMethod);
        Ast.ClassDeclaration scriptClass = new Ast.ClassDeclaration(
            SCRIPT_CLASS_NAME,
            members,
            0,
            0
        );

        List<Ast.Statement> newStatements = new ArrayList<>();
        newStatements.add(scriptClass);
        newStatements.addAll(classes);
        return new Ast.CompilationUnit(newStatements);
    }
}
