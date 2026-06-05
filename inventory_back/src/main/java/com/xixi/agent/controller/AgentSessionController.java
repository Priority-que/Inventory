package com.xixi.agent.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.xixi.agent.service.AgentSessionService;
import com.xixi.pojo.vo.Result;
import com.xixi.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/agent/session")
@RequiredArgsConstructor
@Tag(name = "AI会话", description = "AI会话接口")
public class AgentSessionController {
    private final AgentSessionService agentSessionService;

    @Operation(summary = "查询AI会话列表", operationId = "list")
    @GetMapping("/list")
    public Result list() {
        return Result.success(agentSessionService.getSessionList(SecurityUtils.getCurrentUserId()));
    }

    @Operation(summary = "查询AI会话消息", operationId = "messages")
    @GetMapping("/messages/{threadId}")
    public Result messages(@PathVariable String threadId) {
        return Result.success(agentSessionService.getMessagesByThreadId(threadId, SecurityUtils.getCurrentUserId()));
    }
}

