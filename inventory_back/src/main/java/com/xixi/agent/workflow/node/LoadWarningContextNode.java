package com.xixi.agent.workflow.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xixi.agent.mapper.AgentWarningMapper;
import com.xixi.agent.service.AgentSessionService;
import com.xixi.agent.vo.WarningSnapshotVO;
import com.xixi.agent.workflow.state.WorkflowEntity;
import com.xixi.agent.workflow.state.WorkflowStateKeys;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class LoadWarningContextNode implements NodeAction {
    private final AgentWarningMapper agentWarningMapper;
    private final AgentSessionService agentSessionService;
    private final ObjectMapper objectMapper;

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        WorkflowEntity entity = (WorkflowEntity) state.value(WorkflowStateKeys.ENTITY).orElse(null);
        String threadId = state.value(WorkflowStateKeys.THREAD_ID, "").toString();
        Integer days = entity == null || entity.getDays() == null ? 7 : entity.getDays();

        Map<String, List<WarningSnapshotVO>> context = new HashMap<>();
        context.put("waitConfirmOverdue", agentWarningMapper.getWaitConfirmOverdueOrders(days));
        context.put("inProgressWithoutArrival", agentWarningMapper.getInProgressWithoutArrivalOrders(days));
        context.put("partialArrivalStuck", agentWarningMapper.getPartialArrivalStuckOrders(days));
        context.put("arrivedWithoutInbound", agentWarningMapper.getArrivedWithoutInboundRecords(days));
        context.put("pendingInboundOverdue", agentWarningMapper.getPendingInboundOverdueRecords(days));

        agentSessionService.saveToolMessage(threadId, "loadWarningContext",
                toJson(Map.of("days", days)),
                toJson(Map.of("success", true, "data", context)));
        return Map.of(WorkflowStateKeys.WARNING_CONTEXT, context);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
