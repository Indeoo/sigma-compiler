package org.sigma.codegen;

import org.sigma.ast.Ast;
import org.sigma.syntax.semantic.SymbolTable;
import org.sigma.syntax.semantic.SigmaType;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import java.util.*;

import static org.objectweb.asm.Opcodes.*;

public class SigmaCodeGeneratorRD {

    private final SymbolTable symbolTable;
    private final String className;
    private final List<String> errors = new ArrayList<>();

    private ClassWriter cw;
    private GeneratorAdapter mv;

    private final Map<String,Integer> slots = new HashMap<>();
    private final Map<String, SigmaType> slotTypes = new HashMap<>();

    public SigmaCodeGeneratorRD(SymbolTable st, String className) {
        this.symbolTable = st;
        this.className = className;
    }

    public byte[] generateBytecode(Ast.CompilationUnit cu) {
        try {
            cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            cw.visit(V11, ACC_PUBLIC, className, null, "java/lang/Object", null);
            // default ctor
            var ctor = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            ctor.visitCode(); ctor.visitVarInsn(ALOAD,0); ctor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false); ctor.visitInsn(RETURN); ctor.visitMaxs(1,1); ctor.visitEnd();

            mv = new GeneratorAdapter(ACC_PUBLIC | ACC_STATIC, Method.getMethod("void main (String[])"), null, null, cw);
            mv.visitCode();

            if (cu != null) {
                for (Ast.Statement s : cu.statements) emitStatement(s);
            }

            mv.returnValue(); mv.endMethod(); cw.visitEnd();
            return cw.toByteArray();
        } catch (Exception ex) {
            errors.add(ex.toString()); return null;
        }
    }

    private void emitStatement(Ast.Statement s) {
        if (s instanceof Ast.VariableDeclaration) {
            Ast.VariableDeclaration vd = (Ast.VariableDeclaration)s;
            // choose local type based on declared type name
            SigmaType declared = SigmaType.INT;
            Type localType = Type.INT_TYPE;
            if (vd.typeName != null) {
                String tn = vd.typeName;
                if (tn.equals("double") || tn.equals("float")) { declared = SigmaType.DOUBLE; localType = Type.DOUBLE_TYPE; }
                else if (tn.equals("String")) { declared = SigmaType.STRING; localType = Type.getType(String.class); }
                else if (tn.equals("boolean")) { declared = SigmaType.BOOLEAN; localType = Type.BOOLEAN_TYPE; }
            }
            int slot = mv.newLocal(localType);
            slots.put(vd.name, slot);
            slotTypes.put(vd.name, declared);
            if (vd.init != null) {
                SigmaType t = emitExpression(vd.init);
                // store value into local, performing simple conversions if needed
                if (declared == SigmaType.DOUBLE && t == SigmaType.INT) {
                    mv.visitInsn(I2D); mv.storeLocal(slot, Type.DOUBLE_TYPE);
                } else if (declared == SigmaType.INT && t == SigmaType.DOUBLE) {
                    mv.visitInsn(D2I); mv.storeLocal(slot, Type.INT_TYPE);
                } else if (declared == SigmaType.INT) {
                    mv.storeLocal(slot, Type.INT_TYPE);
                } else if (declared == SigmaType.DOUBLE) {
                    mv.storeLocal(slot, Type.DOUBLE_TYPE);
                } else if (declared == SigmaType.STRING) {
                    mv.storeLocal(slot, Type.getType(String.class));
                } else if (declared == SigmaType.BOOLEAN) {
                    mv.storeLocal(slot, Type.BOOLEAN_TYPE);
                } else {
                    mv.storeLocal(slot, Type.INT_TYPE);
                }
            }
        } else if (s instanceof Ast.ExpressionStatement) {
            Ast.ExpressionStatement es = (Ast.ExpressionStatement)s;
            if (es.expr instanceof Ast.Call) {
                Ast.Call c = (Ast.Call)es.expr;
                if (c.target instanceof Ast.Identifier) {
                    String name = ((Ast.Identifier)c.target).name;
                    if ("println".equals(name)) { handlePrintln(c.args); return; }
                }
            }
            emitExpression(es.expr);
        } else if (s instanceof Ast.Block) {
            for (Ast.Statement st : ((Ast.Block)s).statements) emitStatement(st);
        } else if (s instanceof Ast.IfStatement) {
            Ast.IfStatement is = (Ast.IfStatement)s;
            // evaluate condition -> pushes boolean (int 0/1)
            emitExpression(is.cond);
            org.objectweb.asm.Label elseL = mv.newLabel();
            org.objectweb.asm.Label endL = mv.newLabel();
            // if false jump to else
            mv.ifZCmp(GeneratorAdapter.EQ, elseL);
            // then branch
            if (is.thenBranch != null) emitStatement(is.thenBranch);
            mv.goTo(endL);
            // else branch
            mv.mark(elseL);
            if (is.elseBranch != null) emitStatement(is.elseBranch);
            mv.mark(endL);
        } else if (s instanceof Ast.WhileStatement) {
            Ast.WhileStatement ws = (Ast.WhileStatement)s;
            org.objectweb.asm.Label startL = mv.newLabel();
            org.objectweb.asm.Label endL = mv.newLabel();
            mv.mark(startL);
            emitExpression(ws.cond);
            // if false jump to end
            mv.ifZCmp(GeneratorAdapter.EQ, endL);
            if (ws.body != null) emitStatement(ws.body);
            mv.goTo(startL);
            mv.mark(endL);
        } else if (s instanceof Ast.Assignment) {
            Ast.Assignment a = (Ast.Assignment)s;
            Integer slot = slots.get(a.name);
            SigmaType declared = slotTypes.getOrDefault(a.name, SigmaType.INT);
            if (slot == null) {
                // implicit local - create one as int
                int newSlot = mv.newLocal(Type.INT_TYPE);
                slots.put(a.name, newSlot);
                slotTypes.put(a.name, SigmaType.INT);
                slot = newSlot;
            }
            SigmaType t = emitExpression(a.value);
            if (declared == SigmaType.DOUBLE && t == SigmaType.INT) { mv.visitInsn(I2D); mv.storeLocal(slot, Type.DOUBLE_TYPE); }
            else if (declared == SigmaType.INT && t == SigmaType.DOUBLE) { mv.visitInsn(D2I); mv.storeLocal(slot, Type.INT_TYPE); }
            else if (declared == SigmaType.INT) { mv.storeLocal(slot, Type.INT_TYPE); }
            else if (declared == SigmaType.DOUBLE) { mv.storeLocal(slot, Type.DOUBLE_TYPE); }
            else if (declared == SigmaType.STRING) { mv.storeLocal(slot, Type.getType(String.class)); }
            else if (declared == SigmaType.BOOLEAN) { mv.storeLocal(slot, Type.BOOLEAN_TYPE); }
        }
    }

    private void handlePrintln(List<Ast.Expression> args) {
        if (args == null || args.isEmpty()) {
            mv.getStatic(Type.getType(System.class), "out", Type.getType(java.io.PrintStream.class));
            mv.invokeVirtual(Type.getType(java.io.PrintStream.class), Method.getMethod("void println ()"));
            return;
        }

        // Evaluate the argument first and store to a local so we don't leave other values on the stack
        Ast.Expression arg = args.get(0);
        SigmaType t = emitExpression(arg);
        int valSlot;
        if (t == SigmaType.DOUBLE) {
            valSlot = mv.newLocal(Type.DOUBLE_TYPE);
            mv.storeLocal(valSlot, Type.DOUBLE_TYPE);
        } else if (t == SigmaType.INT) {
            valSlot = mv.newLocal(Type.INT_TYPE);
            mv.storeLocal(valSlot, Type.INT_TYPE);
        } else if (t == SigmaType.BOOLEAN) {
            valSlot = mv.newLocal(Type.BOOLEAN_TYPE);
            mv.storeLocal(valSlot, Type.BOOLEAN_TYPE);
        } else {
            valSlot = mv.newLocal(Type.getType(String.class));
            mv.storeLocal(valSlot, Type.getType(String.class));
        }

        // Now push System.out and then load the value so we have (PrintStream, value)
        mv.getStatic(Type.getType(System.class), "out", Type.getType(java.io.PrintStream.class));
        if (t == SigmaType.DOUBLE) {
            mv.loadLocal(valSlot, Type.DOUBLE_TYPE);
            mv.invokeVirtual(Type.getType(java.io.PrintStream.class), Method.getMethod("void println (double)"));
        } else if (t == SigmaType.INT) {
            mv.loadLocal(valSlot, Type.INT_TYPE);
            mv.invokeVirtual(Type.getType(java.io.PrintStream.class), Method.getMethod("void println (int)"));
        } else if (t == SigmaType.BOOLEAN) {
            mv.loadLocal(valSlot, Type.BOOLEAN_TYPE);
            mv.invokeVirtual(Type.getType(java.io.PrintStream.class), Method.getMethod("void println (boolean)"));
        } else {
            mv.loadLocal(valSlot, Type.getType(String.class));
            mv.invokeVirtual(Type.getType(java.io.PrintStream.class), Method.getMethod("void println (String)"));
        }
    }

    private SigmaType emitExpression(Ast.Expression e) {
        if (e instanceof Ast.IntLiteral) { mv.push(((Ast.IntLiteral)e).value); return SigmaType.INT; }
        if (e instanceof Ast.DoubleLiteral) { mv.push(((Ast.DoubleLiteral)e).value); return SigmaType.DOUBLE; }
        if (e instanceof Ast.StringLiteral) { mv.push(((Ast.StringLiteral)e).value); return SigmaType.STRING; }
        if (e instanceof Ast.Identifier) {
            Integer slot = slots.get(((Ast.Identifier)e).name);
            if (slot != null) { mv.loadLocal(slot, Type.INT_TYPE); return SigmaType.INT; }
            mv.push(((Ast.Identifier)e).name); return SigmaType.STRING;
        }
        if (e instanceof Ast.Binary) {
            Ast.Binary b = (Ast.Binary)e;
            if ("+".equals(b.op)) {
                // simple: if either side is string literal, convert both to strings and concat
                if (b.left instanceof Ast.StringLiteral || b.right instanceof Ast.StringLiteral) {
                    int leftStr = mv.newLocal(Type.getType(String.class));
                    int rightStr = mv.newLocal(Type.getType(String.class));
                    // left -> string
                    SigmaType lt = emitExpression(b.left);
                    if (lt == SigmaType.INT) mv.invokeStatic(Type.getType(String.class), Method.getMethod("String valueOf (int)"));
                    else if (lt == SigmaType.DOUBLE) mv.invokeStatic(Type.getType(String.class), Method.getMethod("String valueOf (double)"));
                    else if (lt == SigmaType.BOOLEAN) mv.invokeStatic(Type.getType(String.class), Method.getMethod("String valueOf (boolean)"));
                    else if (lt == SigmaType.STRING) { /* already string */ }
                    else mv.invokeStatic(Type.getType(String.class), Method.getMethod("String valueOf (Object)"));
                    mv.storeLocal(leftStr);
                    // right -> string
                    SigmaType rt = emitExpression(b.right);
                    if (rt == SigmaType.INT) mv.invokeStatic(Type.getType(String.class), Method.getMethod("String valueOf (int)"));
                    else if (rt == SigmaType.DOUBLE) mv.invokeStatic(Type.getType(String.class), Method.getMethod("String valueOf (double)"));
                    else if (rt == SigmaType.BOOLEAN) mv.invokeStatic(Type.getType(String.class), Method.getMethod("String valueOf (boolean)"));
                    else if (rt == SigmaType.STRING) { /* already string */ }
                    else mv.invokeStatic(Type.getType(String.class), Method.getMethod("String valueOf (Object)"));
                    mv.storeLocal(rightStr);

                    mv.newInstance(Type.getType(StringBuilder.class)); mv.dup(); mv.invokeConstructor(Type.getType(StringBuilder.class), Method.getMethod("void <init> ()"));
                    mv.loadLocal(leftStr); mv.invokeVirtual(Type.getType(StringBuilder.class), Method.getMethod("StringBuilder append (String)"));
                    mv.loadLocal(rightStr); mv.invokeVirtual(Type.getType(StringBuilder.class), Method.getMethod("StringBuilder append (String)"));
                    mv.invokeVirtual(Type.getType(StringBuilder.class), Method.getMethod("String toString ()"));
                    return SigmaType.STRING;
                }
                // evaluate both into locals to handle mixed types safely
                SigmaType lt = emitExpression(b.left);
                int leftSlot = (lt == SigmaType.DOUBLE) ? mv.newLocal(Type.DOUBLE_TYPE) : mv.newLocal(Type.INT_TYPE);
                if (lt == SigmaType.DOUBLE) mv.storeLocal(leftSlot, Type.DOUBLE_TYPE); else mv.storeLocal(leftSlot, Type.INT_TYPE);

                SigmaType rt = emitExpression(b.right);
                int rightSlot = (rt == SigmaType.DOUBLE) ? mv.newLocal(Type.DOUBLE_TYPE) : mv.newLocal(Type.INT_TYPE);
                if (rt == SigmaType.DOUBLE) mv.storeLocal(rightSlot, Type.DOUBLE_TYPE); else mv.storeLocal(rightSlot, Type.INT_TYPE);

                if (lt == SigmaType.DOUBLE || rt == SigmaType.DOUBLE) {
                    // perform double addition
                    if (lt == SigmaType.DOUBLE) mv.loadLocal(leftSlot, Type.DOUBLE_TYPE); else { mv.loadLocal(leftSlot, Type.INT_TYPE); mv.visitInsn(I2D); }
                    if (rt == SigmaType.DOUBLE) mv.loadLocal(rightSlot, Type.DOUBLE_TYPE); else { mv.loadLocal(rightSlot, Type.INT_TYPE); mv.visitInsn(I2D); }
                    mv.visitInsn(DADD); return SigmaType.DOUBLE;
                } else {
                    mv.loadLocal(leftSlot, Type.INT_TYPE); mv.loadLocal(rightSlot, Type.INT_TYPE); mv.visitInsn(IADD); return SigmaType.INT;
                }
            }
            if ("**".equals(b.op)) {
                // pow: evaluate both into locals then convert to double and call Math.pow
                SigmaType lt = emitExpression(b.left);
                int leftSlot = (lt == SigmaType.DOUBLE) ? mv.newLocal(Type.DOUBLE_TYPE) : mv.newLocal(Type.INT_TYPE);
                if (lt == SigmaType.DOUBLE) mv.storeLocal(leftSlot, Type.DOUBLE_TYPE); else mv.storeLocal(leftSlot, Type.INT_TYPE);

                SigmaType rt = emitExpression(b.right);
                int rightSlot = (rt == SigmaType.DOUBLE) ? mv.newLocal(Type.DOUBLE_TYPE) : mv.newLocal(Type.INT_TYPE);
                if (rt == SigmaType.DOUBLE) mv.storeLocal(rightSlot, Type.DOUBLE_TYPE); else mv.storeLocal(rightSlot, Type.INT_TYPE);

                // load as doubles
                if (lt == SigmaType.DOUBLE) mv.loadLocal(leftSlot, Type.DOUBLE_TYPE); else { mv.loadLocal(leftSlot, Type.INT_TYPE); mv.visitInsn(I2D); }
                if (rt == SigmaType.DOUBLE) mv.loadLocal(rightSlot, Type.DOUBLE_TYPE); else { mv.loadLocal(rightSlot, Type.INT_TYPE); mv.visitInsn(I2D); }
                mv.invokeStatic(Type.getType(Math.class), Method.getMethod("double pow (double,double)"));
                if (lt != SigmaType.DOUBLE && rt != SigmaType.DOUBLE) { mv.visitInsn(D2I); return SigmaType.INT; }
                return SigmaType.DOUBLE;
            }
            // comparison operators (>, <, >=, <=, ==, !=)
            if (b.op.equals(">") || b.op.equals("<") || b.op.equals(">=") || b.op.equals("<=") || b.op.equals("==") || b.op.equals("!=")) {
                // evaluate operands into locals
                SigmaType lt = emitExpression(b.left);
                int lslot = (lt == SigmaType.DOUBLE) ? mv.newLocal(Type.DOUBLE_TYPE) : mv.newLocal(Type.INT_TYPE);
                if (lt == SigmaType.DOUBLE) mv.storeLocal(lslot, Type.DOUBLE_TYPE); else mv.storeLocal(lslot, Type.INT_TYPE);

                SigmaType rt = emitExpression(b.right);
                int rslot = (rt == SigmaType.DOUBLE) ? mv.newLocal(Type.DOUBLE_TYPE) : mv.newLocal(Type.INT_TYPE);
                if (rt == SigmaType.DOUBLE) mv.storeLocal(rslot, Type.DOUBLE_TYPE); else mv.storeLocal(rslot, Type.INT_TYPE);

                org.objectweb.asm.Label trueL = mv.newLabel();
                org.objectweb.asm.Label endL = mv.newLabel();

                if (lt == SigmaType.DOUBLE || rt == SigmaType.DOUBLE) {
                    // compare as doubles
                    if (lt == SigmaType.DOUBLE) mv.loadLocal(lslot, Type.DOUBLE_TYPE); else { mv.loadLocal(lslot, Type.INT_TYPE); mv.visitInsn(I2D); }
                    if (rt == SigmaType.DOUBLE) mv.loadLocal(rslot, Type.DOUBLE_TYPE); else { mv.loadLocal(rslot, Type.INT_TYPE); mv.visitInsn(I2D); }
                    // DCMPL pushes int (-1,0,1)
                    mv.visitInsn(DCMPL);
                    switch (b.op) {
                        case ">": mv.visitJumpInsn(IFGT, trueL); break;
                        case "<": mv.visitJumpInsn(IFLT, trueL); break;
                        case ">=": mv.visitJumpInsn(IFGE, trueL); break;
                        case "<=": mv.visitJumpInsn(IFLE, trueL); break;
                        case "==": mv.visitJumpInsn(IFEQ, trueL); break;
                        case "!=": mv.visitJumpInsn(IFNE, trueL); break;
                    }
                } else {
                    // compare as ints
                    mv.loadLocal(lslot, Type.INT_TYPE);
                    mv.loadLocal(rslot, Type.INT_TYPE);
                    switch (b.op) {
                        case ">": mv.visitJumpInsn(IF_ICMPGT, trueL); break;
                        case "<": mv.visitJumpInsn(IF_ICMPLT, trueL); break;
                        case ">=": mv.visitJumpInsn(IF_ICMPGE, trueL); break;
                        case "<=": mv.visitJumpInsn(IF_ICMPLE, trueL); break;
                        case "==": mv.visitJumpInsn(IF_ICMPEQ, trueL); break;
                        case "!=": mv.visitJumpInsn(IF_ICMPNE, trueL); break;
                    }
                }
                // false
                mv.push(false);
                mv.goTo(endL);
                // true
                mv.mark(trueL);
                mv.push(true);
                mv.mark(endL);
                return SigmaType.BOOLEAN;
            }

            // numeric operators: evaluate into locals and handle int/double mixing
            SigmaType ltype = emitExpression(b.left);
            int lslot = (ltype == SigmaType.DOUBLE) ? mv.newLocal(Type.DOUBLE_TYPE) : mv.newLocal(Type.INT_TYPE);
            if (ltype == SigmaType.DOUBLE) mv.storeLocal(lslot, Type.DOUBLE_TYPE); else mv.storeLocal(lslot, Type.INT_TYPE);

            SigmaType rtype = emitExpression(b.right);
            int rslot = (rtype == SigmaType.DOUBLE) ? mv.newLocal(Type.DOUBLE_TYPE) : mv.newLocal(Type.INT_TYPE);
            if (rtype == SigmaType.DOUBLE) mv.storeLocal(rslot, Type.DOUBLE_TYPE); else mv.storeLocal(rslot, Type.INT_TYPE);

            boolean useDouble = ltype == SigmaType.DOUBLE || rtype == SigmaType.DOUBLE;
            if (useDouble) {
                // load operands as doubles
                if (ltype == SigmaType.DOUBLE) mv.loadLocal(lslot, Type.DOUBLE_TYPE); else { mv.loadLocal(lslot, Type.INT_TYPE); mv.visitInsn(I2D); }
                if (rtype == SigmaType.DOUBLE) mv.loadLocal(rslot, Type.DOUBLE_TYPE); else { mv.loadLocal(rslot, Type.INT_TYPE); mv.visitInsn(I2D); }
                switch (b.op) {
                    case "*": mv.visitInsn(DMUL); return SigmaType.DOUBLE;
                    case "/": mv.visitInsn(DDIV); return SigmaType.DOUBLE;
                    case "-": mv.visitInsn(DSUB); return SigmaType.DOUBLE;
                    default: return SigmaType.UNKNOWN;
                }
            } else {
                mv.loadLocal(lslot, Type.INT_TYPE); mv.loadLocal(rslot, Type.INT_TYPE);
                switch (b.op) {
                    case "*": mv.visitInsn(IMUL); return SigmaType.INT;
                    case "/": mv.visitInsn(IDIV); return SigmaType.INT;
                    case "-": mv.visitInsn(ISUB); return SigmaType.INT;
                    default: return SigmaType.UNKNOWN;
                }
            }
        }
        return SigmaType.UNKNOWN;
    }

    public List<String> getErrors() { return new ArrayList<>(errors); }
    public boolean isSuccessful() { return errors.isEmpty(); }

}
