package org.uiuc.ise.yicheng.purity.analysis.agent.visitors;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.uiuc.ise.yicheng.purity.analysis.agent.Config;

/**
 * Created by Yicheng Ouyang on 2021/9/19
 */
public class PurityAnalysisClassVistor extends ClassVisitor {

    protected String slashClassName;
    protected ClassLoader loader;

    PurityAnalysisClassVistor(ClassVisitor classVisitor, String className, ClassLoader loader){
        super(Config.ASM_VERSION, classVisitor);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
//        if (slashClassName.contains("$")){
//            return mv;
//        }
        boolean isStatic = (access & Opcodes.ACC_STATIC) != 0;
        boolean isEnum = (access & Opcodes.ACC_ENUM) != 0;
        boolean isPublic = (access & Opcodes.ACC_PUBLIC) != 0;
        boolean isNative = (access & Opcodes.ACC_NATIVE) != 0;
        if (!isNative && !isEnum && !"<clinit>".equals(name)){
            mv = new PurityAnalysisMethodVisitor(mv, name, access, desc, slashClassName, isStatic, isPublic);
        }
        return mv;
    }
}

