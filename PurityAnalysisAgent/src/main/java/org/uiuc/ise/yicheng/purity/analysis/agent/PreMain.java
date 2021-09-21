package org.uiuc.ise.yicheng.purity.analysis.agent;

import org.uiuc.ise.yicheng.purity.analysis.agent.utils.LogUtil;

import java.lang.instrument.Instrumentation;

/**
 * Created by Yicheng Ouyang on 2021/9/19
 */
public class PreMain {
    public static void premain(String args, Instrumentation inst){
        System.out.println("********************************* PreMain *********************************");
        LogUtil.log("********************************* PreMain *********************************");
        inst.addTransformer(new PurityAnalysisTransformer());
    }
}
