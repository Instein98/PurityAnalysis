package org.uiuc.ise.yicheng.purity.analysis.agent;

import org.uiuc.ise.yicheng.purity.analysis.agent.utils.FileUtil;
import org.uiuc.ise.yicheng.purity.analysis.agent.utils.LogUtil;

import java.lang.instrument.Instrumentation;

/**
 * Created by Yicheng Ouyang on 2021/9/19
 */
public class PreMain {
    public static void premain(String args, Instrumentation inst){
        System.out.println("********************************* PreMain *********************************");
        LogUtil.agentInfo("********************************* PreMain *********************************");
        inst.addTransformer(new PurityAnalysisTransformer());

        Runtime.getRuntime().addShutdownHook(new Thread(){
            public void run() {
                System.out.println("********************************* Shutdown Hook *********************************");
                LogUtil.agentInfo("********************************* Shutdown Hook *********************************");
                LogUtil.agentInfo.close();
                LogUtil.agentError.close();
                FileUtil.dumpMap(PurityRecorder.methodPurityMap, "PurityResult.log");
            }
        });
    }
}
