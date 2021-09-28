package org.uiuc.ise.yicheng.purity.analysis.agent.utils;

import org.uiuc.ise.yicheng.purity.analysis.agent.Config;

import java.io.PrintWriter;

/**
 * Created by Yicheng Ouyang on 2021/9/19
 */
public class LogUtil {

    static {
        try {
            String agentInfoPath = Config.workingDirectory() + "/agent-info.log";
            FileUtil.prepare(agentInfoPath);
            agentInfo = new PrintWriter(agentInfoPath);

            String agentErrorPath = Config.workingDirectory() + "/agent-error.log";
            FileUtil.prepare(agentErrorPath);
            agentError = new PrintWriter(agentErrorPath);

            String classCheckerPath = Config.workingDirectory() + "/class-checker.log";
            FileUtil.prepare(classCheckerPath);
            classChecker = new PrintWriter(classCheckerPath);
        } catch (Throwable t){
            t.printStackTrace();
        }
    }

    public static PrintWriter agentInfo;

    public static PrintWriter agentError;

    public static PrintWriter classChecker;

    public static void agentInfo(String message){
        agentInfo.println(message);
        agentInfo.flush();
    }

    public static synchronized void agentInfo(Throwable t) {
        t.printStackTrace(agentInfo);
        agentInfo.flush();
    }

    public static synchronized void agentError(Throwable t) {
//        if (t instanceof MethodTooLargeException){
//            t.printStackTrace(LogUtils.agentError);
//        } else {
            agentInfo(t);
            t.printStackTrace();
            t.printStackTrace(agentError);
            agentError.flush();
//        }
    }
}
