package com.xixi.agent.workflow.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xixi.agent.mapper.SupplierPerformanceMapper;
import com.xixi.agent.service.AgentSessionService;
import com.xixi.agent.vo.SupplierPerformanceMetricsVO;
import com.xixi.agent.workflow.state.WorkflowEntity;
import com.xixi.agent.workflow.state.WorkflowStateKeys;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@RequiredArgsConstructor
public class LoadSupplierContextNode implements NodeAction {
    private final SupplierPerformanceMapper supplierPerformanceMapper;
    private final AgentSessionService agentSessionService;
    private final ObjectMapper objectMapper;

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        WorkflowEntity entity = (WorkflowEntity) state.value(WorkflowStateKeys.ENTITY).orElse(null);
        String threadId = state.value(WorkflowStateKeys.THREAD_ID, "").toString();
        if (entity == null || entity.getSupplierId() == null) {
            Map<String, Object> response = Map.of("success", false, "message", "未识别到供应商ID");
            agentSessionService.saveToolMessage(threadId, "loadSupplierContext", toJson(entity), toJson(response));
            return Map.of(WorkflowStateKeys.ERROR_MESSAGE, "未识别到供应商ID");
        }

        Integer days = entity.getDays() == null ? 30 : entity.getDays();
        SupplierPerformanceMetricsVO metrics =
                supplierPerformanceMapper.getSupplierPerformanceMetrics(entity.getSupplierId(), days);

        if (metrics == null) {
            Map<String, Object> response = Map.of("success", false, "message", "供应商不存在", "supplierId", entity.getSupplierId());
            agentSessionService.saveToolMessage(threadId, "loadSupplierContext", toJson(entity), toJson(response));
            return Map.of(WorkflowStateKeys.ERROR_MESSAGE, "供应商不存在");
        }

        agentSessionService.saveToolMessage(threadId, "loadSupplierContext", toJson(entity),
                toJson(Map.of("success", true, "data", metrics)));
        return Map.of(WorkflowStateKeys.SUPPLIER_METRICS, metrics);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
