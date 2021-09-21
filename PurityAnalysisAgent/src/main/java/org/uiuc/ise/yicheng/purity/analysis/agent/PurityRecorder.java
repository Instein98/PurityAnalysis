package org.uiuc.ise.yicheng.purity.analysis.agent;

import org.uiuc.ise.yicheng.purity.analysis.agent.utils.LogUtil;

import java.util.HashMap;
import java.util.Stack;

/**
 * Created by Yicheng Ouyang on 2021/9/19
 */
// Todo: 1. skip analysis for all confirmed impure method.
public class PurityRecorder {

    public static String SLASH_CLASS_NAME = "org/uiuc/ise/yicheng/purity/analysis/agent/PurityRecorder";
    public static String METHOD_START = "method_start";
    public static String METHOD_END = "method_end";
    public static String OBJ_NEW = "obj_new";
    public static String OBJ_MODIFY = "obj_modify";
    public static String STATIC_FIELD_MODIFY = "static_field_modify";

    public static HashMap<String, Boolean> methodPurityMap = new HashMap();
    public static HashMap<Long, Stack<PurityStackFrame>> purityStack = new HashMap<>();

    public static void method_start(long threadId, String methodId){
        LogUtil.log("method_start: " + methodId);
        Stack<PurityStackFrame> stack = getPurityStack(threadId);
        if (stack.size() == 0){
            stack.push(new PurityStackFrame(methodId, null));
        }else{
            stack.push(new PurityStackFrame(methodId, stack.peek()));
        }
    }

    public static void method_end(long threadId, String methodId){
        LogUtil.log("method_end: " + methodId);
        Stack<PurityStackFrame> stack = getPurityStack(threadId);
        PurityStackFrame purityStackFrame = stack.peek();
        // assert
        if (!purityStackFrame.getMethodId().equals(methodId)){
            throw new RuntimeException(String.format("Method %s not found in purity stack frame!", methodId));
        }

        purityStackFrame.getModifiedObj().removeAll(purityStackFrame.getCreatedObj());

        // pure
        if (purityStackFrame.getModifiedObj().size() == 0){
            setPurity(methodId, true);
        }
        // impure
        else {
            setPurity(methodId, false);
            PurityStackFrame parentFrame = purityStackFrame.getParanetFrame();
            if (parentFrame != null)
                parentFrame.getModifiedObj().addAll(purityStackFrame.getModifiedObj());
        }
    }

    /**
     *  When recording traceObjNew at the end of <init>, traceObjNew can be called
     *  when recorderStack is empty. It is because <init> is excluded for purity
     *  analysis
     */
    public static void obj_new(long threadId, int objId){
        LogUtil.log("obj_new!");
        Stack<PurityStackFrame> stack = getPurityStack(threadId);
        if (stack.size() != 0)
            stack.peek().getCreatedObj().add(objId);
    }

    public static void obj_modify(long threadId, int objId){
        LogUtil.log("obj_modify!");
        Stack<PurityStackFrame> stack = getPurityStack(threadId);
        if (stack.size() != 0)
            stack.peek().getModifiedObj().add(objId);
    }

    // a method is impure if it modifies the static field
    public static void static_field_modify(long threadId, String methodId){
        LogUtil.log("static_field_modify!");
        Stack<PurityStackFrame> stack = getPurityStack(threadId);
        // check if the stack status is expected.
        if (stack.size() == 0 || !stack.peek().getMethodId().equals(methodId)){
            throw new RuntimeException(String.format("Purity stack messed!\nStack size: %d;\nTop element: %s;\nExpected element: %s",
                    stack.size(), stack.size() == 0 ? "null" : stack.peek().getMethodId(), methodId));
        }
        setPurity(methodId, false);
    }

    private static Stack<PurityStackFrame> getPurityStack(long threadId){
        if (!purityStack.containsKey(threadId)){
            Stack<PurityStackFrame> newStack = new Stack<>();
            purityStack.put(threadId, newStack);
            return newStack;
        } else {
            return purityStack.get(threadId);
        }
    }

    private static void setPurity(String methodId, boolean status){
        if (status == true){
            // if already in methodPurityMap, keep the original value
            if (!methodPurityMap.containsKey(methodId)){
                methodPurityMap.put(methodId, true);
            }
        } else {
            methodPurityMap.put(methodId, false);
        }
    }
}
