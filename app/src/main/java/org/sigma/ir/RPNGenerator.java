package org.sigma.ir;

import org.sigma.semantics.*;
import org.sigma.parser.Ast;

import java.util.*;

/**
 * Generates RPN intermediate representation from a semantically-analyzed AST.
 *
 * This generator performs a traversal of the AST and emits RPN instructions.
 * It uses type information from the semantic analysis phase.
 */
public class RPNGenerator {
    private final RPNProgram.Builder programBuilder;
    private final SymbolTable symbolTable;
    private final Map<Ast.Expression, SigmaType> expressionTypes;
    private int labelCounter = 0;
    private LocalVariableAllocator currentAllocator = null;  // Active allocator for current method

    public RPNGenerator(SemanticResult semanticResult) {
        this.symbolTable = semanticResult.getSymbolTable();
        this.expressionTypes = semanticResult.getExpressionTypes();
        this.programBuilder = new RPNProgram.Builder(symbolTable);
    }

    /**
     * Generates an RPN program from a compilation unit.
     */
    public RPNProgram generate(Ast.CompilationUnit compilationUnit) {
        for (Ast.Statement stmt : compilationUnit.statements) {
            generateStatement(stmt);
        }

        // Add HALT at the end
        emit(new RPNInstruction(RPNOpcode.HALT, 0, 0));

        return programBuilder.build();
    }

    // ============ Statement Generation ============

    private void generateStatement(Ast.Statement stmt) {
        if (stmt instanceof Ast.VariableDeclaration) {
            generateVariableDeclaration((Ast.VariableDeclaration) stmt);
        } else if (stmt instanceof Ast.Assignment) {
            generateAssignment((Ast.Assignment) stmt);
        } else if (stmt instanceof Ast.IfStatement) {
            generateIfStatement((Ast.IfStatement) stmt);
        } else if (stmt instanceof Ast.WhileStatement) {
            generateWhileStatement((Ast.WhileStatement) stmt);
        } else if (stmt instanceof Ast.ReturnStatement) {
            generateReturnStatement((Ast.ReturnStatement) stmt);
        } else if (stmt instanceof Ast.ExpressionStatement) {
            generateExpressionStatement((Ast.ExpressionStatement) stmt);
        } else if (stmt instanceof Ast.MethodDeclaration) {
            generateMethodDeclaration((Ast.MethodDeclaration) stmt);
        } else if (stmt instanceof Ast.ClassDeclaration) {
            generateClassDeclaration((Ast.ClassDeclaration) stmt);
        } else if (stmt instanceof Ast.Block) {
            generateBlock((Ast.Block) stmt);
        } else if (stmt instanceof Ast.FieldDeclaration) {
            generateFieldDeclaration((Ast.FieldDeclaration) stmt);
        } else {
            throw new UnsupportedOperationException(
                "Unsupported statement type: " + stmt.getClass().getSimpleName()
            );
        }
    }

    private void generateVariableDeclaration(Ast.VariableDeclaration decl) {
        // Generate the initializer expression
        if (decl.init != null) {
            generateExpression(decl.init);

            // Store the value in the variable
            SigmaType varType = typeOf(decl.init);
            RPNInstruction storeInstr = RPNInstruction.store(decl.name, varType, decl.line, decl.col);

            // Allocate slot and set it on the instruction
            if (currentAllocator != null) {
                int slot = currentAllocator.allocateVariable(decl.name, varType);
                storeInstr.setSlotIndex(slot);
            }

            emit(storeInstr);
        }
    }

    private void generateAssignment(Ast.Assignment assign) {
        // Generate the value expression
        generateExpression(assign.value);

        // Store in the target variable
        SigmaType valueType = typeOf(assign.value);
        // Assignment doesn't have line/col, use 0
        RPNInstruction storeInstr = RPNInstruction.store(assign.name, valueType, 0, 0);

        // Set slot index if allocator is available
        if (currentAllocator != null && currentAllocator.hasVariable(assign.name)) {
            int slot = currentAllocator.getSlot(assign.name);
            storeInstr.setSlotIndex(slot);
        }

        emit(storeInstr);
    }

    private void generateIfStatement(Ast.IfStatement ifStmt) {
        String elseLabel = generateLabel("else");
        String endLabel = generateLabel("end_if");

        // Generate condition
        generateExpression(ifStmt.cond);

        // Jump to else if condition is false
        emit(RPNInstruction.jumpIfFalse(elseLabel, 0, 0));

        // Generate then branch
        generateStatement(ifStmt.thenBranch);

        // Jump to end (skip else branch)
        emit(RPNInstruction.jump(endLabel, 0, 0));

        // Else label
        emit(RPNInstruction.label(elseLabel, 0, 0));

        // Generate else branch (if present)
        if (ifStmt.elseBranch != null) {
            generateStatement(ifStmt.elseBranch);
        }

        // End label
        emit(RPNInstruction.label(endLabel, 0, 0));
    }

    private void generateWhileStatement(Ast.WhileStatement whileStmt) {
        String startLabel = generateLabel("while_start");
        String endLabel = generateLabel("while_end");

        // Start label (condition evaluation point)
        emit(RPNInstruction.label(startLabel, 0, 0));

        // Generate condition
        generateExpression(whileStmt.cond);

        // Jump to end if condition is false
        emit(RPNInstruction.jumpIfFalse(endLabel, 0, 0));

        // Generate body
        generateStatement(whileStmt.body);

        // Jump back to start
        emit(RPNInstruction.jump(startLabel, 0, 0));

        // End label
        emit(RPNInstruction.label(endLabel, 0, 0));
    }

    private void generateReturnStatement(Ast.ReturnStatement returnStmt) {
        if (returnStmt.expr != null) {
            // Generate the return value expression
            generateExpression(returnStmt.expr);
            emit(new RPNInstruction(RPNOpcode.RETURN, null, typeOf(returnStmt.expr),
                returnStmt.line, returnStmt.col));
        } else {
            emit(new RPNInstruction(RPNOpcode.RETURN_VOID, returnStmt.line, returnStmt.col));
        }
    }

    private void generateExpressionStatement(Ast.ExpressionStatement exprStmt) {
        generateExpression(exprStmt.expr);
        // Pop the result since it's not used
        emit(new RPNInstruction(RPNOpcode.POP, exprStmt.line, exprStmt.col));
    }

    private void generateMethodDeclaration(Ast.MethodDeclaration methodDecl) {
        String methodStartLabel = "method_" + methodDecl.name;
        emit(RPNInstruction.label(methodStartLabel, methodDecl.line, methodDecl.col));

        // Create allocator for this method
        // For now, assume all methods are static (no "this" reference)
        // TODO: Detect instance methods and pass true
        LocalVariableAllocator allocator = new LocalVariableAllocator(false);

        // Allocate slots for parameters
        allocator.allocateParameters(methodDecl.parameters);

        // Set as current allocator
        LocalVariableAllocator previousAllocator = currentAllocator;
        currentAllocator = allocator;

        try {
            // Generate method body
            for (Ast.Statement stmt : methodDecl.body.statements) {
                generateStatement(stmt);
            }

            // Add implicit return for void methods
            if (methodDecl.returnType.equals("void")) {
                emit(new RPNInstruction(RPNOpcode.RETURN_VOID, methodDecl.line, methodDecl.col));
            }
        } finally {
            // Restore previous allocator
            currentAllocator = previousAllocator;
        }
    }

    private void generateClassDeclaration(Ast.ClassDeclaration classDecl) {
        // For now, just generate all method declarations
        // Field handling will be added later
        for (Ast.Statement stmt : classDecl.members) {
            if (stmt instanceof Ast.MethodDeclaration) {
                generateMethodDeclaration((Ast.MethodDeclaration) stmt);
            }
        }
    }

    private void generateFieldDeclaration(Ast.FieldDeclaration fieldDecl) {
        // Generate the initializer expression if present
        if (fieldDecl.init != null) {
            generateExpression(fieldDecl.init);

            // Store the value in the field (for now, treat like variable)
            SigmaType fieldType = typeOf(fieldDecl.init);
            emit(RPNInstruction.store(fieldDecl.name, fieldType, fieldDecl.line, fieldDecl.col));
        }
    }

    private void generateBlock(Ast.Block block) {
        for (Ast.Statement stmt : block.statements) {
            generateStatement(stmt);
        }
    }

    // ============ Expression Generation ============

    private void generateExpression(Ast.Expression expr) {
        if (expr instanceof Ast.IntLiteral) {
            generateIntLiteral((Ast.IntLiteral) expr);
        } else if (expr instanceof Ast.DoubleLiteral) {
            generateDoubleLiteral((Ast.DoubleLiteral) expr);
        } else if (expr instanceof Ast.StringLiteral) {
            generateStringLiteral((Ast.StringLiteral) expr);
        } else if (expr instanceof Ast.BooleanLiteral) {
            generateBooleanLiteral((Ast.BooleanLiteral) expr);
        } else if (expr instanceof Ast.NullLiteral) {
            generateNullLiteral((Ast.NullLiteral) expr);
        } else if (expr instanceof Ast.Identifier) {
            generateIdentifier((Ast.Identifier) expr);
        } else if (expr instanceof Ast.Binary) {
            generateBinary((Ast.Binary) expr);
        } else if (expr instanceof Ast.Unary) {
            generateUnary((Ast.Unary) expr);
        } else if (expr instanceof Ast.Call) {
            generateCall((Ast.Call) expr);
        } else if (expr instanceof Ast.MemberAccess) {
            generateMemberAccess((Ast.MemberAccess) expr);
        } else if (expr instanceof Ast.NewInstance) {
            generateNewInstance((Ast.NewInstance) expr);
        } else {
            throw new UnsupportedOperationException(
                "Unsupported expression type: " + expr.getClass().getSimpleName()
            );
        }
    }

    private void generateIntLiteral(Ast.IntLiteral lit) {
        SigmaType type = typeOf(lit);
        emit(RPNInstruction.push(lit.value, type, lit.line, lit.col));
    }

    private void generateDoubleLiteral(Ast.DoubleLiteral lit) {
        SigmaType type = typeOf(lit);
        emit(RPNInstruction.push(lit.value, type, lit.line, lit.col));
    }

    private void generateStringLiteral(Ast.StringLiteral lit) {
        SigmaType type = typeOf(lit);
        emit(RPNInstruction.push(lit.value, type, lit.line, lit.col));
    }

    private void generateBooleanLiteral(Ast.BooleanLiteral lit) {
        SigmaType type = typeOf(lit);
        // BooleanLiteral doesn't have line/col
        emit(RPNInstruction.push(lit.value, type, 0, 0));
    }

    private void generateNullLiteral(Ast.NullLiteral lit) {
        SigmaType type = typeOf(lit);
        // NullLiteral doesn't have line/col
        emit(RPNInstruction.push(null, type, 0, 0));
    }

    private void generateIdentifier(Ast.Identifier id) {
        SigmaType type = typeOf(id);
        RPNInstruction loadInstr = RPNInstruction.load(id.name, type, id.line, id.col);

        // Set slot index if allocator is available
        if (currentAllocator != null && currentAllocator.hasVariable(id.name)) {
            int slot = currentAllocator.getSlot(id.name);
            loadInstr.setSlotIndex(slot);
        }

        emit(loadInstr);
    }

    private void generateBinary(Ast.Binary binary) {
        // Generate left and right operands (in order)
        generateExpression(binary.left);
        generateExpression(binary.right);

        // Emit the corresponding operation
        RPNOpcode opcode = binaryOperatorToOpcode(binary.op);
        SigmaType resultType = typeOf(binary);
        emit(RPNInstruction.simple(opcode, resultType, binary.line, binary.col));
    }

    private void generateUnary(Ast.Unary unary) {
        // Generate the operand
        generateExpression(unary.expr);

        // Emit the corresponding operation
        RPNOpcode opcode = unaryOperatorToOpcode(unary.op);
        SigmaType resultType = typeOf(unary);
        emit(RPNInstruction.simple(opcode, resultType, unary.line, unary.col));
    }

    private void generateCall(Ast.Call call) {
        // Generate all arguments (left to right)
        for (Ast.Expression arg : call.args) {
            generateExpression(arg);
        }

        // Emit CALL instruction
        String methodName;
        if (call.target instanceof Ast.Identifier) {
            methodName = ((Ast.Identifier) call.target).name;
        } else if (call.target instanceof Ast.MemberAccess) {
            // For method calls like obj.method(), use the member name
            Ast.MemberAccess memberAccess = (Ast.MemberAccess) call.target;
            methodName = memberAccess.memberName;
            // Also need to push the object onto the stack
            generateExpression(memberAccess.object);
        } else {
            throw new UnsupportedOperationException(
                "Unsupported call target: " + call.target.getClass().getSimpleName()
            );
        }

        SigmaType returnType = typeOf(call);
        emit(RPNInstruction.call(methodName, call.args.size(), returnType,
            call.line, call.col));
    }

    private void generateMemberAccess(Ast.MemberAccess memberAccess) {
        // Generate the object expression
        generateExpression(memberAccess.object);

        // Emit GET_FIELD instruction
        SigmaType fieldType = typeOf(memberAccess);
        emit(new RPNInstruction(RPNOpcode.GET_FIELD, memberAccess.memberName,
            fieldType, memberAccess.line, memberAccess.col));
    }

    private void generateNewInstance(Ast.NewInstance newInstance) {
        SigmaType objectType = typeOf(newInstance);

        // Step 1: Allocate uninitialized object
        emit(new RPNInstruction(RPNOpcode.NEW, newInstance.className, objectType,
            newInstance.line, newInstance.col));

        // Step 2: Duplicate object reference (one for constructor, one for return value)
        emit(new RPNInstruction(RPNOpcode.DUP, null, objectType,
            newInstance.line, newInstance.col));

        // Step 3: Push all constructor arguments onto stack
        for (Ast.Expression arg : newInstance.args) {
            generateExpression(arg);
        }

        // Step 4: Call constructor (INVOKESPECIAL <init>)
        // This consumes the duplicate reference and all arguments
        emit(RPNInstruction.invokespecial("<init>", newInstance.args.size(),
            TypeRegistry.VOID, newInstance.line, newInstance.col));

        // Stack now contains: [initialized_object]
    }

    // ============ Helper Methods ============

    /**
     * Emits an instruction to the program.
     */
    private void emit(RPNInstruction instruction) {
        programBuilder.add(instruction);
    }

    /**
     * Generates a unique label name.
     */
    private String generateLabel(String prefix) {
        return prefix + "_" + (labelCounter++);
    }

    /**
     * Gets the type of an expression from the semantic analysis results.
     */
    private SigmaType typeOf(Ast.Expression expr) {
        SigmaType type = expressionTypes.get(expr);
        if (type == null) {
            // Fallback to error type
            return SigmaType.ErrorType.getInstance();
        }
        return type;
    }

    /**
     * Maps binary operators to RPN opcodes.
     */
    private RPNOpcode binaryOperatorToOpcode(String operator) {
        switch (operator) {
            case "+": return RPNOpcode.ADD;
            case "-": return RPNOpcode.SUB;
            case "*": return RPNOpcode.MUL;
            case "/": return RPNOpcode.DIV;
            case "%": return RPNOpcode.MOD;
            case "**": return RPNOpcode.POW;
            case "&&": return RPNOpcode.AND;
            case "||": return RPNOpcode.OR;
            case "==": return RPNOpcode.EQ;
            case "!=": return RPNOpcode.NE;
            case "<": return RPNOpcode.LT;
            case "<=": return RPNOpcode.LE;
            case ">": return RPNOpcode.GT;
            case ">=": return RPNOpcode.GE;
            default:
                throw new UnsupportedOperationException("Unsupported binary operator: " + operator);
        }
    }

    /**
     * Maps unary operators to RPN opcodes.
     */
    private RPNOpcode unaryOperatorToOpcode(String operator) {
        switch (operator) {
            case "-":
            case "neg": return RPNOpcode.NEG;
            case "!": return RPNOpcode.NOT;
            default:
                throw new UnsupportedOperationException("Unsupported unary operator: " + operator);
        }
    }
}
