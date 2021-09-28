package org.uiuc.ise.yicheng.purity.analysis.agent;


import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.CheckClassAdapter;
import org.uiuc.ise.yicheng.purity.analysis.agent.utils.FileUtil;
import org.uiuc.ise.yicheng.purity.analysis.agent.utils.LogUtil;
import org.uiuc.ise.yicheng.purity.analysis.agent.visitors.PurityAnalysisClassVistor;

import java.io.PrintWriter;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.HashSet;
import java.util.Set;

import static org.uiuc.ise.yicheng.purity.analysis.agent.utils.LogUtil.*;

/**
 * Created by Yicheng Ouyang on 2021/9/19
 */
public class PurityAnalysisTransformer implements ClassFileTransformer {

    static Set<String> excludeClasses = new HashSet<>();

    static {
        excludeClasses.add("java");
        excludeClasses.add("jdk");
        excludeClasses.add("sun");
        excludeClasses.add("com/sun");
        excludeClasses.add("org/apache/maven");
        excludeClasses.add("junit");
        excludeClasses.add("org/uiuc/ise");
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        byte[] result = classfileBuffer;
        try{
            if (className == null || shouldExcludeClass(className)){
                return result;
            }

            agentInfo("Instrumenting " + className);

            ClassReader cr = new ClassReader(classfileBuffer);
            ClassWriter cw = new ClassWriter(cr, 0);
            ClassVisitor cv = new PurityAnalysisClassVistor(cw, className, loader);

            cr.accept(cv, 0);
            result = cw.toByteArray();

            FileUtil.write(Config.workingDirectory() + "/PurityAnalysisTransformer/"
                    + className.replace('/', '.') + ".class", result);

            CheckClassAdapter.verify(
                    new ClassReader(result),
                    loader,
                    true,
                    new PrintWriter(classChecker)
//                    new PrintWriter(LogUtil.agentError)
            );
        } catch (Throwable t){
            t.printStackTrace();
            LogUtil.agentError(t);
        }
        return result;
    }

    private static boolean shouldExcludeClass(String className){
        for (String prefix: excludeClasses){
            if (className.startsWith(prefix)){
                return true;
            }
        }
        return false;
    }
}
