package org.uiuc.ise.yicheng.purity.analysis.agent;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Yicheng Ouyang on 2021/9/19
 */
public class PurityStackFrame {

    private List<Integer> createdObj = new ArrayList<>();;
    private List<Integer> modifiedObj = new ArrayList<>();;
//    private List<Integer> calleeImpureObj = new ArrayList<>();

    private PurityStackFrame paranetFrame;
    private String methodId;

    public PurityStackFrame(String methodId, PurityStackFrame parent){
        this.methodId = methodId;
        this.paranetFrame = parent;
    }

    public List<Integer> getCreatedObj() {
        return createdObj;
    }

    public void setCreatedObj(List<Integer> createdObj) {
        this.createdObj = createdObj;
    }

    public List<Integer> getModifiedObj() {
        return modifiedObj;
    }

    public void setModifiedObj(List<Integer> modifiedObj) {
        this.modifiedObj = modifiedObj;
    }

    public PurityStackFrame getParanetFrame() {
        return paranetFrame;
    }

    public void setParanetFrame(PurityStackFrame paranetFrame) {
        this.paranetFrame = paranetFrame;
    }

    public String getMethodId() {
        return methodId;
    }

    public void setMethodId(String methodId) {
        this.methodId = methodId;
    }
}
