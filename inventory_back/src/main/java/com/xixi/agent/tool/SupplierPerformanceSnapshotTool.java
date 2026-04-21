package com.xixi.agent.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xixi.agent.dto.SupplierScoreRequest;
import com.xixi.agent.mapper.SupplierPerformanceMapper;
import com.xixi.agent.service.AgentSessionService;
import com.xixi.agent.vo.SupplierPerformanceMetricsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.BiFunction;

@Component
@RequiredArgsConstructor
public class SupplierPerformanceSnapshotTool implements BiFunction<SupplierScoreRequest, ToolContext, String> {
    private final SupplierPerformanceMapper supplierPerformanceMapper;
    private final AgentSessionService agentSessionService;
    private final ObjectMapper objectMapper;

    @Override
    public String apply(SupplierScoreRequest request, ToolContext toolContext) {
        try {
            String threadId = extractThreadId(toolContext);
            String requestJson = toJson(request);
            if (request == null || request.getSupplierId() == null) {
                String responseJson = objectMapper.writeValueAsString(Map.of(
                        "success", false,
                        "message", "supplierId不能为空"
                ));
                agentSessionService.saveToolMessage(threadId, "supplierPerformanceSnapshotTool", requestJson, responseJson);
                return responseJson;
            }

            Integer days = request.getDays();
            if (days == null) {
                days = 30;
            }
            SupplierPerformanceMetricsVO metrics =
                    supplierPerformanceMapper.getSupplierPerformanceMetrics(request.getSupplierId(), days);

            if (metrics == null) {
                String responseJson = objectMapper.writeValueAsString(Map.of(
                        "success", false,
                        "message", "供应商不存在"
                ));
                agentSessionService.saveToolMessage(threadId, "supplierPerformanceSnapshotTool", requestJson, responseJson);
                return responseJson;
            }

            String responseJson = objectMapper.writeValueAsString(Map.of(
                    "success", true,
                    "data", metrics
            ));
            agentSessionService.saveToolMessage(threadId, "supplierPerformanceSnapshotTool", requestJson, responseJson);
            return responseJson;
        } catch (Exception e) {
            return "{\"success\":false,\"message\":\"供应商履约工具执行失败\"}";
        }
    }

    private String extractThreadId(ToolContext toolContext) {
        if (toolContext == null || toolContext.getContext() == null) {
            return null;
        }
        Object threadId = toolContext.getContext().get("threadId");
        return threadId == null ? null : threadId.toString();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "{}";
        }
    }
}
