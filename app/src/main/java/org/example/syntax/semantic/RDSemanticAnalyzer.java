package org.example.syntax.semantic;

import org.example.ast.Ast;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple semantic analyzer that operates on the RD AST.
 * It performs a minimal pass: builds a SymbolTable with variable declarations and reports basic errors.
 */
public class RDSemanticAnalyzer {

    public SemanticResult analyze(Ast.CompilationUnit cu) {
        SymbolTable st = new SymbolTable();
        List<String> errors = new ArrayList<>();

        if (cu == null) {
            errors.add("Parse failed: no AST produced");
            return SemanticResult.failure(errors);
        }

        for (Ast.Statement s : cu.statements) {
            if (s instanceof Ast.VariableDeclaration) {
                Ast.VariableDeclaration vd = (Ast.VariableDeclaration)s;
                String name = vd.name;
                SigmaType type = SigmaType.fromString(vd.typeName);
                if (st.isDefinedInCurrentScope(name)) {
                    errors.add("Variable '" + name + "' is already declared in this scope");
                } else {
                    Symbol sym = new Symbol(name, type, Symbol.SymbolType.VARIABLE);
                    st.define(sym);
                }
            } else if (s instanceof Ast.ExpressionStatement) {
                // nothing to do for now
            }
            // other statements are accepted but not deeply checked yet
        }

        if (!errors.isEmpty()) return SemanticResult.failure(errors);
        return SemanticResult.success(st);
    }

}
