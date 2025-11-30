package org.sigma.jvm;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.sigma.parser.Ast;

import java.util.List;

/**
 * Generates a minimal JVM class that prints a fixed message to stdout.
 * Placeholder for future full AST bytecode generation.
 */
public class JvmClassGenerator implements Opcodes {

    public byte[] generate(Ast.CompilationUnit compilationUnit) {
        String message = resolveMessage(compilationUnit);

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cw.visit(V17, ACC_PUBLIC | ACC_SUPER, "Script", null, "java/lang/Object", null);

        MethodVisitor init = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        init.visitCode();
        init.visitVarInsn(ALOAD, 0);
        init.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        init.visitInsn(RETURN);
        init.visitMaxs(0, 0);
        init.visitEnd();

        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
        mv.visitCode();
        mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        mv.visitLdcInsn(message);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }

    private String resolveMessage(Ast.CompilationUnit unit) {
        if (unit != null && unit.statements != null) {
            for (Ast.Statement stmt : unit.statements) {
                if (stmt instanceof Ast.ExpressionStatement) {
                    Ast.Expression expr = ((Ast.ExpressionStatement) stmt).expr;
                    if (expr instanceof Ast.StringLiteral) {
                        return ((Ast.StringLiteral) expr).value;
                    }
                }
            }
        }
        return "Sigma JVM backend placeholder";
    }
}
