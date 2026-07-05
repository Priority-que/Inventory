package com.xixi.agent.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.xixi.agent.dto.WorkflowAgentRequest;
import com.xixi.agent.model.AgentWorkflowEvent;
import com.xixi.agent.service.AgentWorkflowService;
import com.xixi.agent.vo.WorkflowAgentResponse;
import com.xixi.pojo.vo.Result;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
@Tag(name = "AI助手", description = "AI助手接口")
public class AgentController {
    private final AgentWorkflowService agentWorkflowService;
    private final ObjectMapper objectMapper;

    @Operation(summary = "执行Java Agent Loop工作流", operationId = "execute")
    @PostMapping("/workflow/execute")
    public Result execute(@RequestBody WorkflowAgentRequest request) {
        return Result.success(agentWorkflowService.execute(request, null));
    }

    @Operation(summary = "流式执行Java Agent Loop工作流", operationId = "stream")
    @PostMapping(value = "/workflow/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public void stream(@RequestBody WorkflowAgentRequest request,
                       HttpServletResponse response) throws IOException {
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);

        PrintWriter writer = response.getWriter();
        WorkflowAgentResponse result = agentWorkflowService.execute(request, event -> writeEventQuietly(writer, event));
        writeEvent(writer, "done", result);
        writer.flush();
    }

    private void writeEvent(PrintWriter writer, String eventName, Object payload) throws IOException {
        writer.write("event: ");
        writer.write(eventName);
        writer.write("\n");
        writer.write("data: ");
        writer.write(objectMapper.writeValueAsString(payload));
        writer.write("\n\n");
    }

    private void writeEventQuietly(PrintWriter writer, AgentWorkflowEvent event) {
        try {
            writeEvent(writer, event.getEventName(), event.getPayload());
            writer.flush();
        } catch (IOException ex) {
            throw new IllegalStateException("写入SSE事件失败", ex);
        }
    }
}

