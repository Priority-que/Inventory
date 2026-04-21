package com.xixi.agent.workflow.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.xixi.agent.workflow.state.WorkflowStateKeys;

import java.util.Map;

public class GuardrailValidateNode implements NodeAction {
    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String answer = state.value(WorkflowStateKeys.LLM_ANSWER,"").toString();
        if(answer.contains("供应商不存在")){
            Object supplierScore = state.value(WorkflowStateKeys.SUPPLIER_SCORE).orElse(null);
            if(supplierScore!=null){
                return Map.of(WorkflowStateKeys.GUARDRAIL_RESULT,"REJECT",
                        WorkflowStateKeys.LLM_ANSWER,"系统检测到 AI 回答可能与结构化数据冲突，请以系统结构化结果为准。"
                        );
            }
        }
        return Map.of(WorkflowStateKeys.GUARDRAIL_RESULT,"PASS");
    }
}
