package org.uiuc.ise.yicheng.purity.analysis.agent.visitors;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.LocalVariablesSorter;
import org.uiuc.ise.yicheng.purity.analysis.agent.Config;
import org.uiuc.ise.yicheng.purity.analysis.agent.PurityRecorder;

import static org.objectweb.asm.Opcodes.*;

/**
 * Created by Yicheng Ouyang on 2021/9/19
 */
// Todo: add big try catch for the method.
public class PurityAnalysisMethodVisitor extends LocalVariablesSorter {

//    public boolean isTestMethod;
//    public boolean expectedException;
    /**
     * There is no need to do purity analysis for constructor or static initializer,
     * purity analysis for constructor will cause problem: VerifyError: (method: <init>)
     * Unable to pop operand off an empty stack
     */
//    private boolean needAnalysePurity;
//    private boolean needSkipMethod;
    private String methodName = null;
    private String className = null;
    private String selfDesc = null;
    private String selfMethodId = null;
    private Type selfReturnType = null;
    private boolean isStatic;
    private boolean isPublic = false;

    private int threadIdLocalIdx;

    private final static Label tryStart = new Label();
    private final static Label tryEnd = new Label();
    private final static Label catchStart = new Label();
    private final static Label catchEnd = new Label();

    public PurityAnalysisMethodVisitor(MethodVisitor mv, String methodName, int access, String desc, String className, boolean isStatic, boolean isPublic) {
        super(Config.ASM_VERSION, access, desc, mv);
        this.methodName = methodName;
        this.className = className;
        this.selfDesc = desc;
//        isTestMethod = isJUnit3TestClass && methodName.startsWith("test");
        selfReturnType = Type.getReturnType(desc);
        selfMethodId = String.format("%s%c%s%c%s", className, Config.MID_SEPARATOR, methodName, Config.MID_SEPARATOR, desc);
        this.isStatic = isStatic;
//        expectedException = false;
//        needAnalysePurity = shouldAnalysePurity();
//        needSkipMethod = !selfReturnType.getDescriptor().equals("V")
//                && !(Type.getArgumentTypes(this.selfDesc).length == 0 && isStatic)
//                && !methodName.equals("<init>");
//        MyEkstaziAgent.log("PurityRecordMVAdapter is initialized!!!");
        this.isPublic = isPublic;
//        if (!isTestMethod)
//            isTestMethod = isTestngClass && isPublic;
    }

//    public boolean shouldAnalysePurity() {
//        return !methodName.equals("<clinit>") && !isEnum;
//    }


    // start of the method
    public void visitCode() {
        mv.visitCode();
        mv.visitTryCatchBlock(tryStart, tryEnd, catchStart, "java/lang/Throwable");
        mv.visitLabel(tryStart);
        threadIdLocalIdx = this.newLocal(Type.getType("J"));
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Thread", "currentThread",
                "()Ljava/lang/Thread;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Thread", "getId", "()J", false);
        mv.visitVarInsn(LSTORE, threadIdLocalIdx);
        mv.visitVarInsn(LLOAD, threadIdLocalIdx);
        mv.visitLdcInsn(this.selfMethodId);
        mv.visitMethodInsn(INVOKESTATIC, PurityRecorder.SLASH_CLASS_NAME, PurityRecorder.METHOD_START,
                "(JLjava/lang/String;)V", false);
    }

    public void visitInsn(int opcode) {
        if (opcode >= IRETURN && opcode <= RETURN) {
            // when a <init> method ends, trigger obj_new event
            if (methodName.equals("<init>")) {
                mv.visitVarInsn(LLOAD, threadIdLocalIdx);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "identityHashCode",
                        "(Ljava/lang/Object;)I", false);
                mv.visitMethodInsn(INVOKESTATIC, PurityRecorder.SLASH_CLASS_NAME, PurityRecorder.OBJ_NEW,
                        "(JI)V", false);
            }
            // trigger method_end event
            mv.visitVarInsn(LLOAD, threadIdLocalIdx);
            mv.visitLdcInsn(this.selfMethodId);
            mv.visitMethodInsn(INVOKESTATIC, PurityRecorder.SLASH_CLASS_NAME, PurityRecorder.METHOD_END,
                    "(JLjava/lang/String;)V", false);
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
            mv.visitVarInsn(LLOAD, threadIdLocalIdx);
            mv.visitInsn(DUP2_X1);
            mv.visitInsn(POP2);
            // ..., ref, idx, value, threadId, ref
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "identityHashCode",
                    "(Ljava/lang/Object;)I", false);
            mv.visitMethodInsn(INVOKESTATIC, PurityRecorder.SLASH_CLASS_NAME,
                    PurityRecorder.OBJ_MODIFY, "(JI)V", false);
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
            mv.visitVarInsn(LLOAD, threadIdLocalIdx);
            mv.visitInsn(DUP2_X1);
            mv.visitInsn(POP2);
            // ..., ref, idx, value, threadId, ref
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "identityHashCode",
                    "(Ljava/lang/Object;)I", false);
            mv.visitMethodInsn(INVOKESTATIC, PurityRecorder.SLASH_CLASS_NAME,
                    PurityRecorder.OBJ_MODIFY, "(JI)V", false);
        }
        mv.visitInsn(opcode);
    }

    public void visitIntInsn(int opcode, int operand) {
        mv.visitIntInsn(opcode, operand);
        // primitive array creation
        if (opcode == NEWARRAY) {
            mv.visitInsn(DUP);
            mv.visitVarInsn(LLOAD, threadIdLocalIdx);
            mv.visitInsn(DUP2_X1);
            mv.visitInsn(POP2);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "identityHashCode",
                    "(Ljava/lang/Object;)I", false);
            mv.visitMethodInsn(INVOKESTATIC, PurityRecorder.SLASH_CLASS_NAME, PurityRecorder.OBJ_NEW,
                    "(JI)V", false);
        }
    }

    public void visitTypeInsn(int opcode, String desc) {
        mv.visitTypeInsn(opcode, desc);
        // reference array creation
        if (opcode == ANEWARRAY) {
            mv.visitInsn(DUP);
            mv.visitVarInsn(LLOAD, threadIdLocalIdx);
            mv.visitInsn(DUP2_X1);
            mv.visitInsn(POP2);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "identityHashCode",
                    "(Ljava/lang/Object;)I", false);
            mv.visitMethodInsn(INVOKESTATIC, PurityRecorder.SLASH_CLASS_NAME, PurityRecorder.OBJ_NEW,
                    "(JI)V", false);
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
                mv.visitVarInsn(LLOAD, threadIdLocalIdx);
                mv.visitInsn(DUP2_X1);
                mv.visitInsn(POP2);
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "identityHashCode",
                        "(Ljava/lang/Object;)I", false);
                mv.visitMethodInsn(INVOKESTATIC, PurityRecorder.SLASH_CLASS_NAME,
                        PurityRecorder.OBJ_MODIFY, "(JI)V", false);
                if (t.getSize() == 2) {
                    mv.visitInsn(DUP_X2);
                    mv.visitInsn(POP);
                } else {
                    mv.visitInsn(SWAP);
                }
            } else if (opc == PUTSTATIC) {
                mv.visitVarInsn(LLOAD, threadIdLocalIdx);
                mv.visitLdcInsn(this.selfMethodId);
                mv.visitMethodInsn(INVOKESTATIC, PurityRecorder.SLASH_CLASS_NAME,
                        PurityRecorder.STATIC_FIELD_MODIFY, "(JLjava/lang/String;)V", false);
            }
        mv.visitFieldInsn(opc, owner, name, desc);
    }

//    public void dynamicallyLog(String msg) {
//        mv.visitLdcInsn(msg);
//        mv.visitMethodInsn(INVOKESTATIC, "org/myekstazi/agent/MyEkstaziAgent", "log",
//                "(Ljava/lang/Object;)V", false);
//    }

    // Todo: track the new event for all created array
    public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
        mv.visitMultiANewArrayInsn(descriptor, numDimensions);
        mv.visitInsn(DUP);
        mv.visitVarInsn(LLOAD, threadIdLocalIdx);
        mv.visitInsn(DUP2_X1);
        mv.visitInsn(POP2);
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "identityHashCode",
                "(Ljava/lang/Object;)I", false);
        mv.visitMethodInsn(INVOKESTATIC, PurityRecorder.SLASH_CLASS_NAME, PurityRecorder.OBJ_NEW,
                "(JI)V", false);
    }

    @Override
    public void visitEnd() {
        mv.visitLabel(tryEnd);
        mv.visitJumpInsn(GOTO, catchEnd);
        mv.visitLabel(catchStart);
        // exception caught
        mv.visitVarInsn(LLOAD, threadIdLocalIdx);
        mv.visitLdcInsn(this.selfMethodId);
        mv.visitMethodInsn(INVOKESTATIC, PurityRecorder.SLASH_CLASS_NAME, PurityRecorder.METHOD_END,
                "(JLjava/lang/String;)V", false);
        mv.visitInsn(ATHROW);
        mv.visitLabel(catchEnd);

        super.visitEnd();
    }
}
