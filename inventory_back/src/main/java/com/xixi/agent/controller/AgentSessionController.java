package com.xixi.agent.controller;

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
public class AgentSessionController {
    private final AgentSessionService agentSessionService;

    @GetMapping("/list")
    public Result list() {
        return Result.success(agentSessionService.getSessionList(SecurityUtils.getCurrentUserId()));
    }

    @GetMapping("/messages/{threadId}")
    public Result messages(@PathVariable String threadId) {
        return Result.success(agentSessionService.getMessagesByThreadId(threadId, SecurityUtils.getCurrentUserId()));
    }
}
