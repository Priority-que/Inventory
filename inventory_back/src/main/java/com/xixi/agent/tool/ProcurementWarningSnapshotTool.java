package com.xixi.agent.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xixi.agent.dto.WarningScanRequest;
import com.xixi.agent.mapper.AgentWarningMapper;
import com.xixi.agent.service.AgentSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

@Component
@RequiredArgsConstructor
public class ProcurementWarningSnapshotTool implements BiFunction<WarningScanRequest, ToolContext,String> {
    private final AgentWarningMapper agentWarningMapper;
    private final AgentSessionService agentSessionService;
    private final ObjectMapper objectMapper;
    @Override
    public String apply(WarningScanRequest warningScanRequest, ToolContext toolContext) {
        try{
            String threadId = extractThreadId(toolContext);
            String requestJson = toJson(warningScanRequest);
            Integer days = warningScanRequest.getDays();
            if(days==null){
                days = 7;
            }
            Map<String,Object> result = new HashMap<>();
            result.put("waitConfirmOverdueOrders",agentWarningMapper.getWaitConfirmOverdueOrders(days));
            result.put("inProgressWithoutArrival",agentWarningMapper.getInProgressWithoutArrivalOrders(days));
            result.put("partialArrivalStuck",agentWarningMapper.getPartialArrivalStuckOrders(days));
            result.put("arrivedWithoutInbound",agentWarningMapper.getArrivedWithoutInboundRecords(days));
            result.put("pendingInboundOverdue",agentWarningMapper.getPendingInboundOverdueRecords(days));
            String responseJson = objectMapper.writeValueAsString(Map.of("success",true,"data",result));
            agentSessionService.saveToolMessage(threadId, "procurementWarningSnapshotTool", requestJson, responseJson);
            return responseJson;
        }catch(Exception e){
            return "{\"success\":false,\"message\":\"采购预警工具执行失败\"}";
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
