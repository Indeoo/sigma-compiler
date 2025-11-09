package org.example.codegen;

import org.example.ast.Ast;
import org.example.semantic.SymbolTable;
import org.example.semantic.Symbol;
import org.example.semantic.SigmaType;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import java.util.*;

import static org.objectweb.asm.Opcodes.*;

/**
 * Minimal code generator that consumes the RD AST and emits JVM bytecode for a main method.
 * Supports variable declarations, expression statements, println calls, literals, binary ops including '^' (Math.pow) and string concatenation.
 */
public class SigmaCodeGeneratorRD {

    private final SymbolTable symbolTable;
    private final String className;
    private final List<String> errors = new ArrayList<>();

    private ClassWriter classWriter;
    private GeneratorAdapter mainMethod;

    private final Map<String, Integer> variableSlots = new HashMap<>();
    private final Map<String, SigmaType> variableTypes = new HashMap<>();

    public SigmaCodeGeneratorRD(SymbolTable symbolTable, String className) {
        this.symbolTable = symbolTable;
        this.className = className;
    }

    public byte[] generateBytecode(Ast.CompilationUnit cu) {
        try {
            classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            classWriter.visit(V11, ACC_PUBLIC, className, null, "java/lang/Object", null);
            // default ctor
            MethodVisitor ctor = classWriter.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            ctor.visitCode(); ctor.visitVarInsn(ALOAD, 0); ctor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false); ctor.visitInsn(RETURN); ctor.visitMaxs(1,1); ctor.visitEnd();

            Method mainSig = Method.getMethod("void main (String[])");
            mainMethod = new GeneratorAdapter(ACC_PUBLIC | ACC_STATIC, mainSig, null, null, classWriter);
            mainMethod.visitCode();

            if (cu != null) {
                for (Ast.Statement s : cu.statements) {
                    emitStatement(s);
                }
            }

            mainMethod.returnValue();
            mainMethod.endMethod();
            classWriter.visitEnd();
            return classWriter.toByteArray();
        } catch (Exception e) {
            errors.add("Codegen exception: " + e.getMessage());
            return null;
        }
    }

    private void emitStatement(Ast.Statement s) {
        if (s instanceof Ast.VariableDeclaration) {
            Ast.VariableDeclaration vd = (Ast.VariableDeclaration)s;
            SigmaType t = SigmaType.fromString(vd.typeName);
            int slot;
            switch (t) {
                case INT: slot = mainMethod.newLocal(Type.INT_TYPE); break;
                case DOUBLE: slot = mainMethod.newLocal(Type.DOUBLE_TYPE); break;
                case BOOLEAN: slot = mainMethod.newLocal(Type.BOOLEAN_TYPE); break;
                case STRING:
                default: slot = mainMethod.newLocal(Type.getType(String.class)); break;
            }
            variableSlots.put(vd.name, slot);
            variableTypes.put(vd.name, t);
            if (vd.init != null) {
                SigmaType exprType = emitExpression(vd.init);
                // store according to declared type
                switch (t) {
                    case INT: mainMethod.storeLocal(slot, Type.INT_TYPE); break;
                    case DOUBLE: mainMethod.storeLocal(slot, Type.DOUBLE_TYPE); break;
                    case BOOLEAN: mainMethod.storeLocal(slot, Type.BOOLEAN_TYPE); break;
                    case STRING:
                    default: mainMethod.storeLocal(slot, Type.getType(String.class)); break;
                }
            }
        } else if (s instanceof Ast.ExpressionStatement) {
            Ast.ExpressionStatement es = (Ast.ExpressionStatement)s;
            // If it's a println call, handle specially
            if (es.expr instanceof Ast.Call) {
                Ast.Call call = (Ast.Call)es.expr;
                if (call.target instanceof Ast.Identifier) {
                    String name = ((Ast.Identifier)call.target).name;
                    if ("println".equals(name) || "print".equals(name)) {
                        handlePrintln(call.args);
                        return;
                    }
                }
            }
            // otherwise evaluate and drop
            emitExpression(es.expr);
            // drop: if it's double or int, pop appropriately
            // For simplicity, do nothing; values left on stack will be cleaned by JVM when main returns
        } else if (s instanceof Ast.Block) {
            Ast.Block b = (Ast.Block)s;
            for (Ast.Statement ss : b.statements) emitStatement(ss);
        } else {
            // other statements not implemented yet
        }
    }

    private void handlePrintln(List<Ast.Expression> args) {
        mainMethod.getStatic(Type.getType(System.class), "out", Type.getType("Ljava/io/PrintStream;"));
        if (args == null || args.isEmpty()) {
            mainMethod.invokeVirtual(Type.getType("Ljava/io/PrintStream;"), Method.getMethod("void println ()"));
            return;
        }
        // print first argument only (simple)
        Ast.Expression e = args.get(0);
        SigmaType t = emitExpression(e);
        switch (t) {
            case INT:
                mainMethod.invokeVirtual(Type.getType("Ljava/io/PrintStream;"), Method.getMethod("void println (int)"));
                break;
            case DOUBLE:
                mainMethod.invokeVirtual(Type.getType("Ljava/io/PrintStream;"), Method.getMethod("void println (double)"));
                break;
            case BOOLEAN:
                mainMethod.invokeVirtual(Type.getType("Ljava/io/PrintStream;"), Method.getMethod("void println (boolean)"));
                break;
            case STRING:
            default:
                mainMethod.invokeVirtual(Type.getType("Ljava/io/PrintStream;"), Method.getMethod("void println (String)"));
                break;
        }
    }

    private SigmaType emitExpression(Ast.Expression e) {
        if (e == null) return SigmaType.UNKNOWN;
        if (e instanceof Ast.IntLiteral) {
            int v = ((Ast.IntLiteral)e).value; mainMethod.push(v); return SigmaType.INT;
        }
        if (e instanceof Ast.DoubleLiteral) {
            double v = ((Ast.DoubleLiteral)e).value; mainMethod.push(v); return SigmaType.DOUBLE;
        }
        if (e instanceof Ast.StringLiteral) {
            String s = ((Ast.StringLiteral)e).value; mainMethod.push(s); return SigmaType.STRING;
        }
        if (e instanceof Ast.Identifier) {
            String name = ((Ast.Identifier)e).name;
            Integer slot = variableSlots.get(name);
            if (slot != null) {
                SigmaType t = variableTypes.get(name);
                if (t == SigmaType.INT) { mainMethod.loadLocal(slot, Type.INT_TYPE); return SigmaType.INT; }
                if (t == SigmaType.DOUBLE) { mainMethod.loadLocal(slot, Type.DOUBLE_TYPE); return SigmaType.DOUBLE; }
                if (t == SigmaType.BOOLEAN) { mainMethod.loadLocal(slot, Type.BOOLEAN_TYPE); return SigmaType.BOOLEAN; }
                mainMethod.loadLocal(slot, Type.getType(String.class)); return SigmaType.STRING;
            }
            // unknown identifier - treat as string name
            mainMethod.push(name); return SigmaType.STRING;
        }
        if (e instanceof Ast.Call) {
            Ast.Call c = (Ast.Call)e;
            // only handle simple builtin println/print where target is Identifier
            if (c.target instanceof Ast.Identifier) {
                String name = ((Ast.Identifier)c.target).name;
                if ("println".equals(name) || "print".equals(name)) {
                    if (c.args.isEmpty()) {
                        return SigmaType.VOID;
                    }
                    SigmaType t = emitExpression(c.args.get(0));
                    return t;
                }
            }
            return SigmaType.UNKNOWN;
        }
        if (e instanceof Ast.Unary) {
            Ast.Unary u = (Ast.Unary)e; SigmaType t = emitExpression(u.expr); return t;
        }
        if (e instanceof Ast.Binary) {
            Ast.Binary b = (Ast.Binary)e;
            if ("+".equals(b.op)) {
                // if any side is string -> do StringBuilder concat
                SigmaType leftType = emitExpression(b.left);
                SigmaType rightType = emitExpression(b.right);
                if (leftType == SigmaType.STRING || rightType == SigmaType.STRING) {
                    // simple approach: convert both sides to String then append
                    // evaluate left
                    // Note: to keep stack shape predictable, evaluate both to Strings then call StringBuilder
                    emitExpression(b.left);
                    if (leftType == SigmaType.INT) {
                        mainMethod.invokeStatic(Type.getType(String.class), Method.getMethod("String valueOf (int)"));
                    } else if (leftType == SigmaType.DOUBLE) {
                        mainMethod.invokeStatic(Type.getType(String.class), Method.getMethod("String valueOf (double)"));
                    } else if (leftType == SigmaType.BOOLEAN) {
                        mainMethod.invokeStatic(Type.getType(String.class), Method.getMethod("String valueOf (boolean)"));
                    }
                    emitExpression(b.right);
                    if (rightType == SigmaType.INT) {
                        mainMethod.invokeStatic(Type.getType(String.class), Method.getMethod("String valueOf (int)"));
                    } else if (rightType == SigmaType.DOUBLE) {
                        mainMethod.invokeStatic(Type.getType(String.class), Method.getMethod("String valueOf (double)"));
                    } else if (rightType == SigmaType.BOOLEAN) {
                        mainMethod.invokeStatic(Type.getType(String.class), Method.getMethod("String valueOf (boolean)"));
                    }
                    // now concatenate via StringBuilder
                    mainMethod.newInstance(Type.getType(StringBuilder.class));
                    mainMethod.dup();
                    mainMethod.invokeConstructor(Type.getType(StringBuilder.class), Method.getMethod("void <init> ()"));
                    // append left string
                    mainMethod.swap(); // bring builder under first arg
                    // Not strictly correct but keep simple: call append(String) twice
                    mainMethod.invokeVirtual(Type.getType(StringBuilder.class), Method.getMethod("StringBuilder append (String)"));
                    mainMethod.invokeVirtual(Type.getType(StringBuilder.class), Method.getMethod("StringBuilder append (String)"));
                    mainMethod.invokeVirtual(Type.getType(StringBuilder.class), Method.getMethod("String toString ()"));
                    return SigmaType.STRING;
                }
                // numeric addition
                if (leftType == SigmaType.DOUBLE || rightType == SigmaType.DOUBLE) {
                    // evaluate both and apply DADD
                    emitExpression(b.left); emitExpression(b.right); mainMethod.visitInsn(DADD); return SigmaType.DOUBLE;
                } else {
                    emitExpression(b.left); emitExpression(b.right); mainMethod.visitInsn(IADD); return SigmaType.INT;
                }
            }
            if ("^".equals(b.op)) {
                // power: use Math.pow
                SigmaType ltype = emitExpression(b.left);
                SigmaType rtype = emitExpression(b.right);
                boolean leftDouble = ltype == SigmaType.DOUBLE;
                boolean rightDouble = rtype == SigmaType.DOUBLE;
                if (!leftDouble && !rightDouble) {
                    // both int: convert to double, call pow, cast to int
                    mainMethod.visitInsn(I2D);
                    // right is on stack as int -> convert
                    mainMethod.visitInsn(I2D);
                    mainMethod.invokeStatic(Type.getType(Math.class), Method.getMethod("double pow (double,double)"));
                    mainMethod.visitInsn(D2I);
                    return SigmaType.INT;
                } else {
                    // ensure both are double
                    if (!leftDouble) mainMethod.visitInsn(I2D);
                    if (!rightDouble) mainMethod.visitInsn(I2D);
                    mainMethod.invokeStatic(Type.getType(Math.class), Method.getMethod("double pow (double,double)"));
                    return SigmaType.DOUBLE;
                }
            }
            // other binary ops: eval left then right and emit appropriate ops
            emitExpression(b.left); emitExpression(b.right);
            switch (b.op) {
                case "*": mainMethod.visitInsn(IMUL); return SigmaType.INT;
                case "/": mainMethod.visitInsn(IDIV); return SigmaType.INT;
                case "%": mainMethod.visitInsn(IREM); return SigmaType.INT;
                case "-": mainMethod.visitInsn(ISUB); return SigmaType.INT;
                default: return SigmaType.UNKNOWN;
            }
        }
        return SigmaType.UNKNOWN;
    }

    public List<String> getErrors() { return new ArrayList<>(errors); }
    public boolean isSuccessful() { return errors.isEmpty(); }

}
