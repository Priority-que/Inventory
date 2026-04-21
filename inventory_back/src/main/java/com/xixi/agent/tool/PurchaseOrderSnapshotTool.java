package com.xixi.agent.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xixi.agent.dto.PurchaseOrderSnapshotRequest;
import com.xixi.agent.mapper.AgentQueryMapper;
import com.xixi.agent.service.AgentSessionService;
import com.xixi.agent.vo.OrderSnapshotVO;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.BiFunction;

@Component
@RequiredArgsConstructor
public class PurchaseOrderSnapshotTool implements BiFunction<PurchaseOrderSnapshotRequest, ToolContext,String> {
    private final AgentQueryMapper agentQueryMapper;
    private final AgentSessionService agentSessionService;
    private final ObjectMapper objectMapper;

    @Override
    public String apply(PurchaseOrderSnapshotRequest request, ToolContext toolContext) {
        try {
            String threadId = extractThreadId(toolContext);
            String requestJson = toJson(request);
            if(request == null || request.getOrderNo() == null || request.getOrderNo().equals("")) {
                String responseJson = objectMapper.writeValueAsString(Map.of("success",false,
                        "message","orderNo不能为空"));
                agentSessionService.saveToolMessage(threadId, "getPurchaseOrderSnapshot", requestJson, responseJson);
                return responseJson;
            }
            OrderSnapshotVO snapshot = agentQueryMapper.getOrderSnapshotByOrderNo((request.getOrderNo().trim()));
            if(snapshot == null) {
                String responseJson = objectMapper.writeValueAsString(Map.of("success",false,
                        "message","采购订单不存在",
                        "orderNo",request.getOrderNo()));
                agentSessionService.saveToolMessage(threadId, "getPurchaseOrderSnapshot", requestJson, responseJson);
                return responseJson;
            }
            String responseJson = objectMapper.writeValueAsString(Map.of(
                    "success",true,
                    "data",snapshot));
            agentSessionService.saveToolMessage(threadId, "getPurchaseOrderSnapshot", requestJson, responseJson);
            return responseJson;
        } catch (Exception e) {
            return "{\"success\":false,\"message\":\"工具执行失败\"}";
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
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
