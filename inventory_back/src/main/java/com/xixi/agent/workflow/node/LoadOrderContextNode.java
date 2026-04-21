package com.xixi.agent.workflow.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xixi.agent.mapper.AgentQueryMapper;
import com.xixi.agent.service.AgentSessionService;
import com.xixi.agent.vo.OrderSnapshotVO;
import com.xixi.agent.workflow.state.WorkflowEntity;
import com.xixi.agent.workflow.state.WorkflowStateKeys;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@RequiredArgsConstructor
public class LoadOrderContextNode implements NodeAction{
    private final AgentQueryMapper agentQueryMapper;
    private final AgentSessionService agentSessionService;
    private final ObjectMapper objectMapper;

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        WorkflowEntity entity = (WorkflowEntity) state.value(WorkflowStateKeys.ENTITY).orElse(null);
        String threadId = state.value(WorkflowStateKeys.THREAD_ID, "").toString();
        if(entity == null || entity.getOrderNo()==null){
            Map<String, Object> response = Map.of("success", false, "message", "未识别采购订单号");
            agentSessionService.saveToolMessage(threadId, "loadOrderContext", toJson(entity), toJson(response));
            return Map.of(WorkflowStateKeys.ERROR_MESSAGE,"未识别采购订单号");
        }
        OrderSnapshotVO snapshot = agentQueryMapper.getOrderSnapshotByOrderNo(entity.getOrderNo());
        if(snapshot == null){
            Map<String, Object> response = Map.of("success", false, "message", "采购订单号不存在", "orderNo", entity.getOrderNo());
            agentSessionService.saveToolMessage(threadId, "loadOrderContext", toJson(entity), toJson(response));
            return Map.of(WorkflowStateKeys.ERROR_MESSAGE,"采购订单号不存在");
        }
        agentSessionService.saveToolMessage(threadId, "loadOrderContext", toJson(entity),
                toJson(Map.of("success", true, "data", snapshot)));
        return Map.of(WorkflowStateKeys.ORDER_SNAPSHOT,snapshot);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
