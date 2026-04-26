package com.xixi.agent.workflow.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.xixi.agent.workflow.state.WorkflowStateKeys;

import java.util.Map;

public class RouteDecisionNode implements NodeAction {
    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String intent = state.value(WorkflowStateKeys.INTENT, "UNKNOWN").toString();
        return Map.of("_route",intent);
    }
}
