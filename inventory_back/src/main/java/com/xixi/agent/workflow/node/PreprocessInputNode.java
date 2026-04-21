package com.xixi.agent.workflow.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.xixi.agent.workflow.state.WorkflowStateKeys;

import java.util.Map;

public class PreprocessInputNode implements NodeAction {
    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String message = state.value(WorkflowStateKeys.MESSAGE,"").toString().trim();
        if(message == null || message.isEmpty()){
            return Map.of(
                    WorkflowStateKeys.ERROR_MESSAGE,"请输入要分析的问题",
                    WorkflowStateKeys.NORMALIZED_MESSAGE,""
            );
        }
        return Map.of(WorkflowStateKeys.NORMALIZED_MESSAGE,message);
    }
}
