package org.uiuc.ise.yicheng.purity.analysis.agent.visitors;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.uiuc.ise.yicheng.purity.analysis.agent.Config;
import org.uiuc.ise.yicheng.purity.analysis.agent.PurityRecorder;

import static org.objectweb.asm.Opcodes.*;

/**
 * Created by Yicheng Ouyang on 2021/9/19
 */
public class PurityAnalysisMethodVisitor extends MethodVisitor {

    /**
     * There is no need to do purity analysis for constructor or static initializer,
     * purity analysis for constructor will cause problem: VerifyError: (method: <init>)
     * Unable to pop operand off an empty stack
     */
    private String methodName = null;
    private String className = null;
    private String selfDesc = null;
    private String selfMethodId = null;
    private Type selfReturnType = null;
    private boolean isStatic;
    private boolean isPublic = false;
    private boolean isConstructor = false;

    private Label tryStart = new Label();
    private Label tryEndCatchStart = new Label();

    public PurityAnalysisMethodVisitor(MethodVisitor mv, String methodName, int access, String desc, String className, boolean isStatic, boolean isPublic) {
        super(Config.ASM_VERSION, mv);
        this.methodName = methodName;
        this.className = className;
        this.selfDesc = desc;
        selfReturnType = Type.getReturnType(desc);
        selfMethodId = String.join(Config.MID_SEPARATOR, className, methodName, desc);
        this.isStatic = isStatic;
        this.isPublic = isPublic;
        this.isConstructor = "<init>".equals(methodName);
    }

    // start of the method
    @Override
    public void visitCode() {
        mv.visitCode();
        mv.visitLabel(tryStart);
        mv.visitLdcInsn(this.selfMethodId);
        mv.visitMethodInsn(INVOKESTATIC, PurityRecorder.SLASH_CLASS_NAME, PurityRecorder.METHOD_START,
                "(Ljava/lang/String;)V", false);
    }

    public void visitInsn(int opcode) {
        if (opcode >= IRETURN && opcode <= RETURN) {
            // when a <init> method ends, trigger obj_new event
            if (this.isConstructor) {
                mv.visitVarInsn(ALOAD, 0);
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "identityHashCode",
                        "(Ljava/lang/Object;)I", false);
                mv.visitMethodInsn(INVOKESTATIC, PurityRecorder.SLASH_CLASS_NAME, PurityRecorder.OBJ_NEW,
                        "(I)V", false);
            }
            // trigger method_end event
            mv.visitLdcInsn(this.selfMethodId);
            mv.visitMethodInsn(INVOKESTATIC, PurityRecorder.SLASH_CLASS_NAME, PurityRecorder.METHOD_END,
                    "(Ljava/lang/String;)V", false);
        }

        if (opcode == ATHROW){
            mv.visitLdcInsn(this.selfMethodId);
            mv.visitMethodInsn(INVOKESTATIC, PurityRecorder.SLASH_CLASS_NAME, PurityRecorder.METHOD_END,
                    "(Ljava/lang/String;)V", false);
        }

        // IASTORE, FASTORE, DASTORE, AASTORE, CASTORE, SASTORE: value of category 1 computational type
        if (opcode == IASTORE || opcode == FASTORE || opcode == AASTORE ||
                opcode == BASTORE || opcode == CASTORE || opcode == SASTORE) {
            // ..., ref, idx, value
            mv.visitInsn(DUP2_X1);
            // ..., idx, value, ref, idx, value
            mv.visitInsn(POP2);
            // ..., idx, value, ref
            mv.visitInsn(DUP_X2);
            // ..., ref, idx, value, ref
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "identityHashCode",
                    "(Ljava/lang/Object;)I", false);
            mv.visitMethodInsn(INVOKESTATIC, PurityRecorder.SLASH_CLASS_NAME,
                    PurityRecorder.OBJ_MODIFY, "(I)V", false);
        }
        // LASTORE, BASTORE: value of category 2 computational type
        else if (opcode == LASTORE || opcode == DASTORE) {
            // ..., ref, idx, value
            mv.visitInsn(DUP2_X2);
            // ..., value, ref, idx, value
            mv.visitInsn(POP2);
            // ..., value, ref, idx
            mv.visitInsn(DUP2_X2);
            // ..., ref, idx, value, ref, idx
            mv.visitInsn(POP);
            // ..., ref, idx, value, ref
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "identityHashCode",
                    "(Ljava/lang/Object;)I", false);
            mv.visitMethodInsn(INVOKESTATIC, PurityRecorder.SLASH_CLASS_NAME,
                    PurityRecorder.OBJ_MODIFY, "(I)V", false);
        }
        mv.visitInsn(opcode);
    }

    public void visitIntInsn(int opcode, int operand) {
        mv.visitIntInsn(opcode, operand);
        // primitive array creation
        if (opcode == NEWARRAY) {
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "identityHashCode",
                    "(Ljava/lang/Object;)I", false);
            mv.visitMethodInsn(INVOKESTATIC, PurityRecorder.SLASH_CLASS_NAME, PurityRecorder.OBJ_NEW,
                    "(I)V", false);
        }
    }

    public void visitTypeInsn(int opcode, String desc) {
        mv.visitTypeInsn(opcode, desc);
        // reference array creation
        if (opcode == ANEWARRAY) {
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "identityHashCode",
                    "(Ljava/lang/Object;)I", false);
            mv.visitMethodInsn(INVOKESTATIC, PurityRecorder.SLASH_CLASS_NAME, PurityRecorder.OBJ_NEW,
                    "(I)V", false);
        }
    }

    public void visitFieldInsn(int opc, String owner, String name, String desc) {
            /* The initialization of inner class will result in recording uninitializedThis, see
             * https://stackoverflow.com/questions/63216849/in-the-byte-code-level-what-is-the-process-to-initialize-an-java-inner-class */
            if (opc == PUTFIELD && !className.contains("$")) {
                Type t = Type.getType(desc);
                if (t.getSize() == 2) {
                    // ..., obj, value
                    mv.visitInsn(DUP2_X1);
                    // ..., value, obj, value
                    mv.visitInsn(POP2);
                    // ..., value, obj
                } else {
                    mv.visitInsn(SWAP);
                    // ..., value, obj
                }
                mv.visitInsn(DUP);
                // ..., value, obj, obj
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "identityHashCode",
                        "(Ljava/lang/Object;)I", false);
                mv.visitMethodInsn(INVOKESTATIC, PurityRecorder.SLASH_CLASS_NAME,
                        PurityRecorder.OBJ_MODIFY, "(I)V", false);
                if (t.getSize() == 2) {
                    mv.visitInsn(DUP_X2);
                    mv.visitInsn(POP);
                } else {
                    mv.visitInsn(SWAP);
                }
            } else if (opc == PUTSTATIC) {
                mv.visitLdcInsn(this.selfMethodId);
                mv.visitMethodInsn(INVOKESTATIC, PurityRecorder.SLASH_CLASS_NAME,
                        PurityRecorder.STATIC_FIELD_MODIFY, "(Ljava/lang/String;)V", false);
            }
        mv.visitFieldInsn(opc, owner, name, desc);
    }

    // Todo: track the new event for all created array
    public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
        mv.visitMultiANewArrayInsn(descriptor, numDimensions);
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "identityHashCode",
                "(Ljava/lang/Object;)I", false);
        mv.visitMethodInsn(INVOKESTATIC, PurityRecorder.SLASH_CLASS_NAME, PurityRecorder.OBJ_NEW,
                "(I)V", false);
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        /* It is impossible to create an exception handler covering the entire constructor.
         * Refer to https://stackoverflow.com/a/69554673/11495796. */
        if (!isConstructor){
            mv.visitTryCatchBlock(tryStart, tryEndCatchStart, tryEndCatchStart, "java/lang/Throwable");
            mv.visitLabel(tryEndCatchStart);
            // exception caught
            mv.visitFrame(F_FULL, 0, null, 1, new Object[] {"java/lang/Throwable"});
            mv.visitLdcInsn(this.selfMethodId);
            mv.visitMethodInsn(INVOKESTATIC, PurityRecorder.SLASH_CLASS_NAME, PurityRecorder.METHOD_END,
                    "(Ljava/lang/String;)V", false);
            mv.visitInsn(ATHROW);
        }
        super.visitMaxs(maxStack + 5, maxLocals);
    }
}
